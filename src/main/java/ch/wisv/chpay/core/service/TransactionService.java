package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.aop.CheckSystemNotFrozen;
import ch.wisv.chpay.core.exception.IllegalRefundException;
import ch.wisv.chpay.core.exception.InsufficientBalanceException;
import ch.wisv.chpay.core.exception.UserNotFoundException;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.*;
import ch.wisv.chpay.core.repository.RequestRepository;
import ch.wisv.chpay.core.repository.TransactionRepository;
import jakarta.persistence.LockTimeoutException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
public class TransactionService {
  private final TransactionRepository transactionRepository;
  private final BalanceService balanceService;
  private final RequestRepository requestRepository;
  private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

  @Autowired
  TransactionService(
      TransactionRepository transactionRepository,
      BalanceService balanceService,
      RequestRepository requestRepository) {
    this.transactionRepository = transactionRepository;
    this.balanceService = balanceService;
    this.requestRepository = requestRepository;
  }

  /**
   * Refund a transaction. Changes the status of the transaction to REFUNDED and creates a new
   * REFUND transaction for logging purposes. The money is added to the user's balance again.
   *
   * @param transactionId The id of the transaction to be refunded.
   * @return The refund transaction.
   */
  @CheckSystemNotFrozen
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public RefundTransaction refundTransaction(UUID transactionId)
      throws NoSuchElementException,
          IllegalRefundException,
          IllegalStateException,
          IllegalArgumentException {
    Transaction original = getTransactionOrThrow(transactionId);

    if (original.getStatus() != Transaction.TransactionStatus.SUCCESSFUL
        || original.getType() == Transaction.TransactionType.REFUND) {
      throw new IllegalRefundException(
          "Transaction cannot be refunded, not successful or is a refund");
    }

    if (!original.isRefundable()) {
      throw new IllegalRefundException("Transaction cannot be refunded");
    }

    boolean alreadyRefunded = transactionRepository.existsByRefundOf(original);
    if (alreadyRefunded) {
      throw new IllegalRefundException("Transaction already refunded.");
    }

    BigDecimal refundAmount = original.getAmount();
    User originalUser = original.getUser();

    RefundTransaction refund = balanceService.refund(originalUser, refundAmount.negate(), original);
    original.setStatus(Transaction.TransactionStatus.REFUNDED);
    return transactionRepository.save(refund);
  }

  /**
   * Processes a partial refund for a given transaction. Ensures the transaction is eligible for
   * refund and verifies that the refund amount does not exceed the refundable balance. Updates the
   * original transaction's status accordingly and creates a new refund transaction.
   *
   * @param transactionId the unique identifier of the transaction to refund
   * @param refundAmount the amount to be refunded, which must be positive and within the refundable
   *     limit
   * @return the newly created refund transaction
   * @throws IllegalStateException if the transaction is not eligible for a refund, such as when it
   *     is not successful, is already a refund, or is a top-up transaction
   * @throws IllegalArgumentException if the refund amount is non-positive or exceeds the remaining
   *     refundable balance
   */
  @CheckSystemNotFrozen
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public RefundTransaction partialRefund(UUID transactionId, BigDecimal refundAmount)
      throws NoSuchElementException,
          IllegalRefundException,
          IllegalStateException,
          IllegalArgumentException {
    Transaction original = getTransactionOrThrow(transactionId);

    if ((original.getStatus() != Transaction.TransactionStatus.SUCCESSFUL
            && original.getStatus() != Transaction.TransactionStatus.PARTIALLY_REFUNDED)
        || original.getType() == Transaction.TransactionType.REFUND) {
      throw new IllegalRefundException(
          "Transaction cannot be refunded, not successful or is a refund");
    }

    if (!original.isRefundable()) {
      throw new IllegalRefundException("Transaction cannot be refunded");
    }

    if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalRefundException("Refund amount must be positive");
    }

    BigDecimal totalRefunded =
        transactionRepository.findByRefundOf(original).stream()
            .map(RefundTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal originalPaid = original.getAmount().abs();
    BigDecimal remainingAmount = originalPaid.subtract(totalRefunded);

    if (refundAmount.compareTo(remainingAmount) > 0) {
      throw new IllegalRefundException("Refund amount exceeds remaining refundable amount");
    }

    RefundTransaction refund =
        balanceService.refund(original.getUser(), refundAmount.negate(), original);

    if ((refundAmount.add(totalRefunded)).compareTo(original.getAmount().abs()) == 0) {
      original.setStatus(Transaction.TransactionStatus.REFUNDED);
    } else {
      original.setStatus(Transaction.TransactionStatus.PARTIALLY_REFUNDED);
    }

    transactionRepository.save(original);
    return transactionRepository.save(refund);
  }

  // If this ever gets deployed, and you see this, I'm sorry

  /**
   * Gets a refundable transaction by it's id, or throws an exception. If new transactions are
   * added, this method should be updated to support them.
   *
   * @param transactionId the id of the transaction to get
   * @return the transaction
   */
  private Transaction getTransactionOrThrow(UUID transactionId) {
    return Stream.<Supplier<Optional<? extends Transaction>>>of(
            () -> transactionRepository.findByIdForUpdateExternal(transactionId),
            () -> transactionRepository.findByIdForUpdatePayment(transactionId))
        .map(Supplier::get)
        .flatMap(Optional::stream)
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
  }

  /**
   * Processes a pending transaction. Locks the transaction for update and call pay from
   * balanceService to update the user's balance based on the amount.
   *
   * @param transactionId the unique identifier of the transaction to be fulfilled
   * @param user the user initiating the transaction fulfillment
   * @return the updated transaction after fulfillment
   * @throws IllegalStateException if the transaction is not in a PENDING state
   */
  @CheckSystemNotFrozen
  @Retryable(
      retryFor = {PessimisticEntityLockException.class, LockTimeoutException.class},
      notRecoverable = {
        InsufficientBalanceException.class,
        UserNotFoundException.class,
        IllegalStateException.class,
        NoSuchElementException.class
      },
      backoff = @Backoff(delay = 200, multiplier = 2))
  @Transactional
  public Transaction fullfillTransaction(UUID transactionId, User user)
      throws IllegalStateException,
          NoSuchElementException,
          InsufficientBalanceException,
          UserNotFoundException {
    PaymentTransaction lockedTransaction =
        transactionRepository
            .findByIdForUpdatePayment(transactionId)
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

    if (!lockedTransaction.isFulfillable()) {
      throw new IllegalStateException("Transaction cannot be fulfilled");
    }

    if (lockedTransaction.getStatus() != Transaction.TransactionStatus.PENDING) {
      throw new IllegalStateException("Transaction is not in pending state");
    }

    PaymentRequest request = lockedTransaction.getRequest();

    if (request != null && request.isFulfilled() && !request.isMultiUse()) {
      throw new IllegalStateException("Request is already fulfilled or has expired");
    }
    Transaction result = balanceService.pay(user, lockedTransaction);

    if (request != null && !request.isMultiUse()) {
      request.setFulfilled(true);
      requestRepository.save(request);
    }

    return result;
  }

  /**
   * Attempts to fulfill an externally initiated transaction. On a lock failure the transaction is
   * set to {@code FAILED}. This is only needed for events, so the user can be redirected back and
   * shown an error page there. Like internal transactions, this method is retryable on locking
   * issues, but not on balance insufficiency.
   *
   * @param transactionId the ID of the transaction to fulfill
   * @param user the user executing the transaction
   * @return the updated {@link PaymentTransaction} after successful fulfillment
   * @throws IllegalStateException if the transaction is not pending or constraints fail
   * @throws InsufficientBalanceException if the user does not have enough balance
   */
  @CheckSystemNotFrozen
  @Retryable(
      retryFor = {PessimisticEntityLockException.class, LockTimeoutException.class},
      notRecoverable = {
        InsufficientBalanceException.class,
        UserNotFoundException.class,
        IllegalStateException.class,
        NoSuchElementException.class
      },
      backoff = @Backoff(delay = 200, multiplier = 2))
  @Transactional
  public Transaction fullfillExternalTransaction(UUID transactionId, User user)
      throws IllegalStateException,
          NoSuchElementException,
          InsufficientBalanceException,
          UserNotFoundException {
    ExternalTransaction lockedTransaction =
        transactionRepository
            .findByIdForUpdateExternal(transactionId)
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

    if (!lockedTransaction.isFulfillable()) {
      throw new IllegalStateException("Transaction cannot be fulfilled");
    }

    if (lockedTransaction.getStatus() != Transaction.TransactionStatus.PENDING) {
      throw new IllegalStateException("Transaction is not in pending state");
    }

    return balanceService.pay(user, lockedTransaction);
  }

  @Recover
  @Transactional
  public Transaction recoverFromPessimisticLockException(
      PessimisticEntityLockException e, UUID transactionId, User user) {
    logger.warn(
        "Recovering from {} for txId={} and userId={}",
        "lock exception",
        transactionId,
        user.getId());
    return markTransactionAsFailed(transactionId, "Lock exception" + e.getMessage());
  }

  @Recover
  @Transactional
  public Transaction recoverFromLockTimeout(LockTimeoutException e, UUID transactionId, User user) {
    logger.warn(
        "Recovering from {} for txId={} and userId={}",
        "lock timeout",
        transactionId,
        user.getId());
    return markTransactionAsFailed(transactionId, "Lock timeout" + e.getMessage());
  }

  /**
   * Marks a transaction as failed and logs failure.
   *
   * @param transactionId the id of the transaction to mark as failed.
   * @param reason the reason for failure.
   * @return the transaction that was marked as failed.
   */
  private Transaction markTransactionAsFailed(UUID transactionId, String reason) {
    Transaction tx =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

    tx.setStatus(Transaction.TransactionStatus.FAILED);
    logger.error("Transaction {} marked as FAILED due to {}", transactionId, reason);
    return tx;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public BigDecimal getNonRefundedAmount(UUID transactionId) {
    Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
    if (transaction.getStatus() == Transaction.TransactionStatus.REFUNDED) {
      return BigDecimal.ZERO;
    }
    BigDecimal totalRefunded =
        transactionRepository.findByRefundOf(transaction).stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal originalPaid = transaction.getAmount().abs();
    return originalPaid.subtract(totalRefunded);
  }

  /**
   * Saves a transaction
   *
   * @param transaction the transaction
   * @return the transaction
   */
  @CheckSystemNotFrozen
  @Retryable(backoff = @Backoff(delay = 200, multiplier = 2))
  @Transactional
  public Transaction save(Transaction transaction) {
    return transactionRepository.save(transaction);
  }

  /**
   * Find a transaction by its id
   *
   * @param mollieId the id
   * @return the transction
   */
  @Transactional(readOnly = true)
  public Optional<TopupTransaction> getTransaction(String mollieId) {
    return transactionRepository.findTransactionByMollieId(mollieId);
  }

  /**
   * Gets a list of the transactions for a given user.
   *
   * @param user The user to get the transactions for.
   * @return A list of transactions for the given user.
   */
  @Transactional(readOnly = true)
  public List<Transaction> getTransactionsForUser(User user) {
    return transactionRepository.findByUser(user);
  }

  /**
   * Gets a transaction by its id.
   *
   * @param id The id of the transaction.
   * @return Optional of the transaction with the given id.
   */
  @Transactional(readOnly = true)
  public Optional<Transaction> getTransactionById(UUID id) {
    return transactionRepository.findById(id);
  }
}
