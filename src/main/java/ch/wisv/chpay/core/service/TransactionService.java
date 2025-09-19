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
   * Gets a list of the transactions for a given user for a certain page.
   *
   * @param user The user to get the transactions for.
   * @param page The page number to get
   * @param size how many transactions by page
   * @return A list of transactions for the given user.
   */
  @Transactional(readOnly = true)
  public List<Transaction> getTransactionsForUserPageable(User user, long page, long size) {
    Pageable pageable = PageRequest.of(((int) page), ((int) size));
    return transactionRepository.findByUser(user, pageable);
  }

  /**
   * Gets a list of the transactions for a given user for a certain page sorted by some column.
   *
   * @param user The user to get the transactions for.
   * @param page The page number to get
   * @param size how many transactions by page
   * @param sort object by which to sort
   * @return A list of transactions for the given user.
   */
  @Transactional(readOnly = true)
  public List<Transaction> getTransactionsForUserPageable(
      User user, long page, long size, Sort sort) {
    Pageable pageable = PageRequest.of(((int) page), ((int) size), sort);
    return transactionRepository.findByUser(user, pageable);
  }

  /**
   * Gets a list of the transactions for a given user and given types for a certain page sorted by
   * some column.
   *
   * @param user The user to get the transactions for.
   * @param page The page number to get
   * @param size how many transactions by page
   * @param sort object by which to sort
   * @param types the types by which to filter transactions
   * @return A list of transactions for the given user and type.
   */
  @Transactional(readOnly = true)
  public List<Transaction> getTransactionsForUserPageable(
      User user, long page, long size, Sort sort, Collection<Transaction.TransactionType> types) {
    Pageable pageable = PageRequest.of(((int) page), ((int) size), sort);
    return transactionRepository.findByUserAndTypeIn(user, types, pageable);
  }

  /**
   * Gets a list of the transactions for a given user id for a certain page.
   *
   * @param userId The user to get the transactions for.
   * @param page The page number to get
   * @param size how many transactions by page
   * @return A list of transactions for the given user.
   */
  @Transactional(readOnly = true)
  public List<Transaction> getTransactionsForUserIdPageable(UUID userId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return transactionRepository.findByUserId(userId, pageable);
  }

  /***
   * get the count of all transactions associated with a user
   * @param user the user by whom is searched
   * @return count of transactions
   */
  public long countTransactionsByUser(User user) {
    return transactionRepository.countByUser(user);
  }

  /***
   * get the count of all transactions associated with a user based on type
   * @param user the user by whom is searched
   * @param types of transaction by which to filter
   * @return count of transactions
   */
  public long countTransactionsByUser(User user, List<Transaction.TransactionType> types) {
    return transactionRepository.countByUserAndTypeIn(user, types);
  }

  /***
   * get the count of all transactions associated with a userId
   * @param userId the of id of the user by whom is searched
   * @return count of transactions
   */
  public long countTransactionsByUserId(UUID userId) {
    return transactionRepository.countByUser_Id(userId);
  }

  /***
   * get the count of all transactions
   * @return count of transactions
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countTransactionsAll() {
    return transactionRepository.count();
  }

  /**
   * Get the count of all transactions between two dates
   *
   * @param dateStart start of range
   * @param dateEnd end of range
   * @return count of transactions
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countTransactionsByDate(LocalDateTime dateStart, LocalDateTime dateEnd) {
    return transactionRepository.countTransactionsByTimestampBetween(dateStart, dateEnd);
  }

  /**
   * Get the count of all transactions by amount
   *
   * @param amountUnder upper bound of amount
   * @param amountOver lower bound of amount
   * @return count of transactions
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countTransactionsByAmount(BigDecimal amountUnder, BigDecimal amountOver) {
    return transactionRepository.countTransactionsByAmountBetween(amountUnder, amountOver);
  }

  /**
   * Get the count of all transactions by all the given users
   *
   * @param users list of users
   * @return count of transactions
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countTransactionsByUsersIn(List<User> users) {
    return transactionRepository.countTransactionsByUserIn(users);
  }

  /**
   * Get the count of all transactions found for a description query
   *
   * @param desc description query
   * @return count of transactions
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countTransactionsByDescription(String desc) {
    return transactionRepository.countTransactionsByDescriptionContainsIgnoreCase(desc);
  }

  /**
   * Counts the number of transactions that match the specified types and statuses.
   *
   * @param types a list of transaction types to filter by
   * @param statuses a list of transaction statuses to filter by
   * @return the total number of transactions that match the given types and statuses
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countTransactionsByTypeAndStatus(
      List<Transaction.TransactionType> types, List<Transaction.TransactionStatus> statuses) {
    return transactionRepository.countTransactionByTypeInAndStatusIn(types, statuses);
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

  /**
   * Gets a list of the transactions for all users.
   *
   * @param page The page number to get
   * @param size how many transactions by page
   * @return A list of transactions.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getAllTransactionsPageable(int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return transactionRepository.findBy(pageable);
  }

  /**
   * Gets a list of the transactions for all users filtered by amount of price.
   *
   * @param page The page number to get
   * @param size how many transactions by page
   * @param amountUnder upper bound of amount
   * @param amountOver lower bound of amount
   * @return A list of transactions.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getAllTransactionsByAmountPageable(
      BigDecimal amountUnder, BigDecimal amountOver, int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return transactionRepository.findTransactionByAmountBetween(amountUnder, amountOver, pageable);
  }

  /**
   * Gets a list of the transactions for all users filtered by date.
   *
   * @param page The page number to get
   * @param size how many transactions by page
   * @param start the first date in range
   * @param end last date in range
   * @return A list of transactions.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getAllTransactionsByDatePageable(
      LocalDateTime start, LocalDateTime end, int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, sort);
    return transactionRepository.findTransactionsByTimestampBetween(start, end, pageable);
  }

  /**
   * Gets a list of the transactions for a specific list of users.
   *
   * @param page The page number to get
   * @param size how many transactions by page
   * @param users a list of users
   * @return A list of transactions.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getAllTransactionsByUserListPageable(
      List<User> users, int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return transactionRepository.findTransactionByUserIn(users, pageable);
  }

  /**
   * Gets a list of the transactions by description query.
   *
   * @param page The page number to get
   * @param size how many transactions by page
   * @param desc description query
   * @return A list of transactions.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getAllTransactionsByDescriptionPageable(
      String desc, int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return transactionRepository.findTransactionsByDescriptionContainingIgnoreCase(desc, pageable);
  }

  /**
   * Retrieves a pageable list of transactions filtered by specified types and statuses.
   *
   * @param types the list of transaction types to filter by
   * @param statuses the list of transaction statuses to filter by
   * @param page the page number to retrieve (zero-based index)
   * @param size the size of the page to retrieve
   * @return a list of transactions that match the specified types and statuses within the given
   *     page parameters
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getTransactionsForTypeAndStatusPageable(
      List<Transaction.TransactionType> types,
      List<Transaction.TransactionStatus> statuses,
      int page,
      int size,
      Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return transactionRepository.findTransactionByTypeInAndStatusIn(types, statuses, pageable);
  }
}
