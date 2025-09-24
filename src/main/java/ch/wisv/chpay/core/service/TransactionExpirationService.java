package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionExpirationService {

  @PersistenceContext private EntityManager entityManager;

  private final TransactionRepository transactionRepository;

  public TransactionExpirationService(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  @Value("${chpay.transactions.expire_every_minutes}")
  private long expireEveryMinutes;

  /**
   * Scheduled task set to at a fixed rate, making any pending transaction older than the specified
   * expiration time fail, changing its status. chpay.transactions.expiration-fixed-rate is the rate
   * to run the task at chpay.transactions.expire-every-minutes is the age at which a transaction is
   * considered old and should be failed. Both of these can be set in the application.yml file
   */
  @Scheduled(fixedRateString = "#{${chpay.transactions.expiration_fixed_rate} * 60 * 1000}")
  @Transactional
  public void updateTransactionStatuses() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireEveryMinutes);

    List<UUID> expiredIds =
        transactionRepository.findExpiredTransactionIds(
            Transaction.TransactionStatus.PENDING, cutoff);

    List<Transaction> expired = new ArrayList<>();
    for (UUID id : expiredIds) {
      Transaction tx = entityManager.find(Transaction.class, id, LockModeType.PESSIMISTIC_WRITE);
      tx.setStatus(Transaction.TransactionStatus.FAILED);
      expired.add(tx);
    }

    if (!expired.isEmpty()) {
      transactionRepository.saveAll(expired);
    }
  }
}
