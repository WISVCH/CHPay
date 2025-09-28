package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.*;
import ch.wisv.chpay.core.model.transaction.Transaction.TransactionStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
  List<Transaction> findByUser(User user);

  @Query(
      "SELECT SUM(t.amount) FROM Transaction t WHERE t.type='TOP_UP' AND t.status='SUCCESSFUL' AND t.timestamp BETWEEN :dateStart AND :dateEnd")
  BigDecimal getIncomingSum(LocalDateTime dateStart, LocalDateTime dateEnd);

  @Query(
      "SELECT SUM(t.amount) FROM Transaction t WHERE t.type='TOP_UP' AND t.status='SUCCESSFUL' AND t.user=:user AND t.timestamp BETWEEN :dateStart AND :dateEnd")
  BigDecimal getIncomingSumUser(LocalDateTime dateStart, LocalDateTime dateEnd, User user);

  @Query(
      "SELECT SUM(t.amount) FROM Transaction t WHERE t.type='PAYMENT' AND (t.status='SUCCESSFUL' OR  t.status='REFUNDED' OR t.status='PARTIALLY_REFUNDED') AND t.timestamp BETWEEN :dateStart AND :dateEnd")
  BigDecimal getPaymentSum(LocalDateTime dateStart, LocalDateTime dateEnd);

  @Query(
      "SELECT SUM(t.amount) FROM Transaction t WHERE t.type='PAYMENT' AND (t.status='SUCCESSFUL' OR  t.status='REFUNDED' OR t.status='PARTIALLY_REFUNDED') AND t.user=:user AND t.timestamp BETWEEN :dateStart AND :dateEnd")
  BigDecimal getPaymentSumUser(LocalDateTime dateStart, LocalDateTime dateEnd, User user);

  @Query(
      "SELECT SUM(t.amount) FROM Transaction t WHERE t.type='REFUND' AND t.status='SUCCESSFUL' AND t.timestamp BETWEEN :dateStart AND :dateEnd")
  BigDecimal getRefundSum(LocalDateTime dateStart, LocalDateTime dateEnd);

  @Query(
      "SELECT SUM(t.amount) FROM Transaction t WHERE t.type='REFUND' AND t.status='SUCCESSFUL' AND t.user=:user AND t.timestamp BETWEEN :dateStart AND :dateEnd")
  BigDecimal getRefundSumUser(LocalDateTime dateStart, LocalDateTime dateEnd, User user);

  long countTransactionByUserAndTimestampBetweenAndStatusIs(
      User user,
      LocalDateTime timestampAfter,
      LocalDateTime timestampBefore,
      Transaction.TransactionStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM TopupTransaction t WHERE t.id = :id")
  TopupTransaction findByIdForUpdateTopup(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM PaymentTransaction t WHERE t.id = :id")
  Optional<PaymentTransaction> findByIdForUpdatePayment(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM ExternalTransaction t WHERE t.id = :id")
  Optional<ExternalTransaction> findByIdForUpdateExternal(@Param("id") UUID id);

  Optional<TopupTransaction> findTransactionByMollieId(String mollieId);

  @Query("SELECT COUNT(r) > 0 FROM RefundTransaction r WHERE r.refundOf = :original")
  boolean existsByRefundOf(Transaction refundedTransaction);

  List<RefundTransaction> findByRefundOf(Transaction original);

  @Query(
      "SELECT pt FROM PaymentTransaction pt WHERE pt.user = :user AND pt.request = :request AND pt.status = :status")
  Optional<PaymentTransaction> findFirstByUserAndRequestAndStatus(
      User user, PaymentRequest request, Transaction.TransactionStatus status);

  @Query("SELECT t.id FROM Transaction t WHERE t.status = :status AND t.timestamp < :cutoff")
  List<UUID> findExpiredTransactionIds(Transaction.TransactionStatus status, LocalDateTime cutoff);

  /**
   * Find all transactions for a given PaymentRequest ID whose status is SUCCESSFUL, ordered by
   * timestamp ascendingly.
   */
  @Query(
      """
        SELECT t
          FROM Transaction t
          JOIN t.request r
         WHERE r.request_id = :requestId
           AND t.status      = :status
         ORDER BY t.timestamp ASC
      """)
  List<Transaction> findAllSuccessfulForRequest(
      @Param("requestId") UUID requestId, @Param("status") TransactionStatus status);

  /**
   * Find all transactions for a given PaymentRequest ID whose status is SUCCESSFUL, ordered by
   * timestamp ascendingly.
   */
  @Query(
      """
              SELECT t
                FROM Transaction t
                JOIN t.request r
               WHERE r.request_id = :requestId
               ORDER BY t.timestamp DESC
            """)
  List<Transaction> findAllByRequestId(@Param("requestId") UUID requestId);

  /** Find all transactions for a given year and month. */
  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE YEAR(t.timestamp) = :year
        AND MONTH(t.timestamp) = :month
        ORDER BY t.timestamp DESC
      """)
  List<Transaction> findTransactionsByYearAndMonth(
      @Param("year") int year, @Param("month") int month);

  /** Get distinct year-month combinations from all transactions. */
  @Query(
      """
        SELECT DISTINCT YEAR(t.timestamp), MONTH(t.timestamp)
        FROM Transaction t
        ORDER BY YEAR(t.timestamp) DESC, MONTH(t.timestamp) DESC
      """)
  List<Object[]> findDistinctYearMonthCombinations();
}
