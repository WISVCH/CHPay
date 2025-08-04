package chpay.paymentbackend.service;

import chpay.DatabaseHandler.Exceptions.IllegalRefundException;
import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import chpay.DatabaseHandler.Exceptions.UserNotFoundException;
import chpay.DatabaseHandler.transactiondb.entities.*;
import chpay.DatabaseHandler.transactiondb.entities.transactions.RefundTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.paymentbackend.aop.CheckSystemNotFrozen;
import jakarta.persistence.LockTimeoutException;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService {

  private final UserRepository userRepository;
  private final TransactionRepository transactionRepository;
  private final SettingService settingService;
  private static final Logger logger = LoggerFactory.getLogger(BalanceService.class);

  @Autowired
  public BalanceService(
      UserRepository userRepository,
      TransactionRepository transactionRepository,
      SettingService settingService) {
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
    this.settingService = settingService;
  }

  /**
   * Creates a new transaction tied to the user with the given description and amount, changing the
   * balance accordingly.
   *
   * @param user The user to top up.
   */
  @CheckSystemNotFrozen
  @Transactional
  protected Transaction pay(User user, Transaction pendingTransaction)
      throws InsufficientBalanceException,
          UserNotFoundException,
          IllegalStateException,
          NoSuchElementException {
    User lockedFrom = userRepository.findByIdForUpdate(user.getId());

    if (lockedFrom == null) {
      throw new UserNotFoundException("User not found");
    }

    if (!lockedFrom.equals(pendingTransaction.getUser())) {
      throw new IllegalStateException(
          "User is not the same as the one who created the transaction");
    }

    BigDecimal amount = pendingTransaction.getAmount();

    if (lockedFrom.getBalance().compareTo(amount) < 0) {
      throw new InsufficientBalanceException(
          "Insufficient balance for payment for payment" + pendingTransaction.getId());
    }

    debit(lockedFrom, amount.abs());
    pendingTransaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    return transactionRepository.save(pendingTransaction);
  }

  /**
   * Refunds a transaction. Changes the status of the transaction to REFUNDED and creates a new
   * REFUND transaction for logging purposes. The money is added to the user's balance again.
   *
   * @param user The user to refund the transaction for.
   * @param amount The amount to refund. Should be negative as only payments can be refunded.
   * @param original The original transaction to be refunded. Must be a payment or top-up. If the
   *     transaction is a refund, the original must be a refund of a payment or top-up.
   * @return The refund transaction.
   */
  @CheckSystemNotFrozen
  @Transactional
  @Retryable(
      retryFor = {PessimisticEntityLockException.class, LockTimeoutException.class},
      notRecoverable = {
        IllegalRefundException.class,
        UserNotFoundException.class,
        IllegalStateException.class,
        NoSuchElementException.class
      },
      backoff = @Backoff(delay = 200, multiplier = 2))
  protected RefundTransaction refund(User user, BigDecimal amount, Transaction original)
      throws IllegalStateException,
          IllegalArgumentException,
          IllegalRefundException,
          UserNotFoundException {
    User lockedFrom = userRepository.findByIdForUpdate(user.getId());

    if (lockedFrom == null) {
      throw new UserNotFoundException("User not found");
    }

    if (amount.compareTo(BigDecimal.ZERO) >= 0) {
      throw new IllegalRefundException(
          "Can only refund payments for events/services, not for top-ups");
    }

    if (!lockedFrom.equals(original.getUser())) {
      throw new IllegalStateException("User is not the same as the one who originally paid");
    }

    credit(lockedFrom, amount.abs());

    RefundTransaction refund = RefundTransaction.createRefund(lockedFrom, amount.abs(), original);

    return transactionRepository.save(refund);
  }

  @Recover
  protected RefundTransaction recoverRefundFromLockTimeout(
      LockTimeoutException e, User user, BigDecimal amount, Transaction original) {
    logger.error(
        "Refund failed due to lock timeout. userId={}, txId={}, amount={}. Exception: {}",
        user.getId(),
        original.getId(),
        amount,
        e.getMessage(),
        e);

    throw new IllegalStateException("Lock timeout while trying to refund transaction");
  }

  @Recover
  protected RefundTransaction recoverRefundFromPessimisticLock(
      PessimisticEntityLockException e, User user, BigDecimal amount, Transaction original) {
    logger.error(
        "Refund failed due to lock exception. userId={}, txId={}, amount={}. Exception: {}",
        user.getId(),
        original.getId(),
        amount,
        e.getMessage(),
        e);
    throw new IllegalStateException(
        "Pessimistic lock exception while trying to refund transaction");
  }

  /**
   * Marks a top-up transaction as paid. Changes the status of the transaction to SUCCESSFUL and
   * adds the amount to the user's balance.
   *
   * @param tx The transaction to be marked as paid. Must be a PENDING transaction.
   */
  @CheckSystemNotFrozen
  @Transactional
  public void markTopUpAsPaid(TopupTransaction tx)
      throws IllegalStateException, IllegalArgumentException {
    User lockedFrom = userRepository.findByIdForUpdate(tx.getUser().getId());

    if (lockedFrom == null) {
      throw new UserNotFoundException("User not found");
    }

    credit(lockedFrom, tx.getAmount());

    userRepository.save(lockedFrom);

    tx.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
    transactionRepository.save(tx);
  }

  /**
   * Marks a top-up transaction as failed. Changes the status of the transaction to FAILED.
   *
   * @param tx The transaction to be marked as failed. Must be a PENDING transaction.
   */
  @CheckSystemNotFrozen
  @Transactional
  public void markTopUpAsFailed(TopupTransaction tx) {
    tx.setStatus(Transaction.TransactionStatus.FAILED);
    transactionRepository.save(tx);
  }

  /**
   * Internal method to add money to a user's balance. Checks if the balance is within the limit.
   *
   * @param user The user to add the money to.
   * @param amount The amount to add to the balance. Must be a positive number.
   */
  private void credit(User user, BigDecimal amount)
      throws IllegalStateException, IllegalArgumentException {
    settingService.assertBalanceWithinLimit(user.getBalance(), amount);

    user.addBalance(amount);

    userRepository.saveAndFlush(user);
  }

  /**
   * Internal method to subtract money from a user's balance.
   *
   * @param user The user to subtract the money from.
   * @param amount The amount to subtract from the balance. Must be a positive number.
   */
  private void debit(User user, BigDecimal amount)
      throws IllegalArgumentException, InsufficientBalanceException {
    user.subtractBalance(amount);
    userRepository.saveAndFlush(user);
  }
}
