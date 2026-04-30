package ch.wisv.chpay.customer.service;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.TopupTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.service.BalanceService;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopUpLifecycleService {
  private static final Logger logger = LoggerFactory.getLogger(TopUpLifecycleService.class);

  private final TransactionRepository transactionRepository;
  private final BalanceService balanceService;
  private final MailService mailService;

  public TopUpLifecycleService(
      TransactionRepository transactionRepository,
      BalanceService balanceService,
      MailService mailService) {
    this.transactionRepository = transactionRepository;
    this.balanceService = balanceService;
    this.mailService = mailService;
  }

  @Transactional
  public TopupTransaction markPending(UUID transactionId) {
    TopupTransaction tx = transactionRepository.findByIdForUpdateTopup(transactionId);
    if (tx == null) {
      throw new NoSuchElementException("Transaction not found: " + transactionId);
    }

    if (tx.getStatus() == Transaction.TransactionStatus.PENDING) {
      tx.setStatus(Transaction.TransactionStatus.PENDING);
      return transactionRepository.saveAndFlush(tx);
    }
    return tx;
  }

  @Transactional
  public TopupTransaction markPaid(UUID transactionId) {
    TopupTransaction tx = transactionRepository.findByIdForUpdateTopup(transactionId);
    if (tx == null) {
      throw new NoSuchElementException("Transaction not found: " + transactionId);
    }

    if (tx.getStatus() != Transaction.TransactionStatus.PENDING) {
      return tx;
    }

    User lockedUser = balanceService.topup(tx.getUser(), tx.getAmount());
    tx.setUser(lockedUser);
    tx.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
    try {
      mailService.sendDepositSuccessEmail(tx, tx.getAmount());
    } catch (Exception e) {
      logger.error("Failed to send deposit success email for transaction {}", tx.getId(), e);
    }
    return transactionRepository.saveAndFlush(tx);
  }

  @Transactional
  public TopupTransaction markCancelled(UUID transactionId) {
    TopupTransaction tx = transactionRepository.findByIdForUpdateTopup(transactionId);
    if (tx == null) {
      throw new NoSuchElementException("Transaction not found: " + transactionId);
    }
    if (tx.getStatus() != Transaction.TransactionStatus.PENDING) {
      return tx;
    }
    tx.setStatus(Transaction.TransactionStatus.CANCELLED);
    try {
      mailService.sendDepositFailEmail(tx, tx.getAmount());
    } catch (Exception e) {
      logger.error("Failed to send deposit fail email for transaction {}", tx.getId(), e);
    }
    return transactionRepository.saveAndFlush(tx);
  }

  @Transactional
  public TopupTransaction markFailed(UUID transactionId) {
    TopupTransaction tx = transactionRepository.findByIdForUpdateTopup(transactionId);
    if (tx == null) {
      throw new NoSuchElementException("Transaction not found: " + transactionId);
    }
    if (tx.getStatus() != Transaction.TransactionStatus.PENDING) {
      return tx;
    }
    tx.setStatus(Transaction.TransactionStatus.FAILED);
    try {
      mailService.sendDepositFailEmail(tx, tx.getAmount());
    } catch (Exception e) {
      logger.error("Failed to send deposit fail email for transaction {}", tx.getId(), e);
    }
    return transactionRepository.saveAndFlush(tx);
  }
}
