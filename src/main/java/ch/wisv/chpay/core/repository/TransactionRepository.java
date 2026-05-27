package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.*;
import ch.wisv.chpay.core.model.transaction.Transaction.TransactionStatus;
import ch.wisv.chpay.core.model.transaction.Transaction.TransactionType;
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
  interface AmountCountProjection {
    BigDecimal getTotalAmount();

    Long getTransactionCount();
  }

  interface PaymentRequestAggregateProjection {
    UUID getRequestId();

    String getRequestDescription();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
  }

  interface ExternalApiAggregateProjection {
    String getApiClientName();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
  }

  List<Transaction> findByUser(User user);

  long countByUserAndStatusInAndTypeIn(
      User user, List<TransactionStatus> statuses, List<TransactionType> types);

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

  /** Find all transactions for a given user ID. */
  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
        ORDER BY t.timestamp DESC
      """)
  List<Transaction> findAllByUserId(@Param("userId") UUID userId);

  /** Find all transactions for a given user ID and year-month. */
  @Query(
      """
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
        AND YEAR(t.timestamp) = :year
        AND MONTH(t.timestamp) = :month
        ORDER BY t.timestamp DESC
      """)
  List<Transaction> findTransactionsByUserIdAndYearMonth(
      @Param("userId") UUID userId, @Param("year") int year, @Param("month") int month);

  /** Find all transactions for a given payment request ID and year-month. */
  @Query(
      """
        SELECT t
        FROM Transaction t
        JOIN t.request r
        WHERE r.request_id = :requestId
        AND YEAR(t.timestamp) = :year
        AND MONTH(t.timestamp) = :month
        ORDER BY t.timestamp DESC
      """)
  List<Transaction> findTransactionsByRequestIdAndYearMonth(
      @Param("requestId") UUID requestId, @Param("year") int year, @Param("month") int month);

  /** Get distinct year-month combinations for a specific user. */
  @Query(
      """
        SELECT DISTINCT YEAR(t.timestamp), MONTH(t.timestamp)
        FROM Transaction t
        WHERE t.user.id = :userId
        ORDER BY YEAR(t.timestamp) DESC, MONTH(t.timestamp) DESC
      """)
  List<Object[]> findDistinctYearMonthCombinationsByUserId(@Param("userId") UUID userId);

  /** Get distinct year-month combinations for a specific payment request. */
  @Query(
      """
        SELECT DISTINCT YEAR(t.timestamp), MONTH(t.timestamp)
        FROM Transaction t
        JOIN t.request r
        WHERE r.request_id = :requestId
        ORDER BY YEAR(t.timestamp) DESC, MONTH(t.timestamp) DESC
      """)
  List<Object[]> findDistinctYearMonthCombinationsByRequestId(@Param("requestId") UUID requestId);

  Optional<Transaction> findFirstByTypeInAndStatusInOrderByTimestampDesc(
      List<TransactionType> types, List<TransactionStatus> statuses);

  /** Count fulfilments per month for a specific payment request. */
  @Query(
      """
                      SELECT YEAR(t.timestamp), MONTH(t.timestamp), COUNT(t)
                      FROM Transaction t
                      JOIN t.request r
                      WHERE r.request_id = :requestId
                        AND t.type = 'PAYMENT'
                        AND (t.status = 'SUCCESSFUL'
                             OR t.status = 'REFUNDED'
                             OR t.status = 'PARTIALLY_REFUNDED')
                      GROUP BY YEAR(t.timestamp), MONTH(t.timestamp)
                      ORDER BY YEAR(t.timestamp) DESC, MONTH(t.timestamp) DESC
                    """)
  List<Object[]> countFulfilmentsByRequestIdPerMonth(@Param("requestId") UUID requestId);

  /** Aggregate successful top-ups for a month. */
  @Query(
      """
        SELECT COALESCE(SUM(t.amount), 0) AS totalAmount,
               COUNT(t) AS transactionCount
        FROM Transaction t
        WHERE t.timestamp >= :start
          AND t.timestamp < :end
          AND t.type = 'TOP_UP'
          AND t.status = 'SUCCESSFUL'
      """)
  AmountCountProjection findTopUpAggregateForPeriod(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Aggregate successful refunds for a month. */
  @Query(
      """
        SELECT COALESCE(SUM(t.amount), 0) AS totalAmount,
               COUNT(t) AS transactionCount
        FROM Transaction t
        WHERE t.timestamp >= :start
          AND t.timestamp < :end
          AND t.type = 'REFUND'
          AND t.status = 'SUCCESSFUL'
      """)
  AmountCountProjection findRefundAggregateForPeriod(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Aggregate outgoing internal payments grouped by request for a month. */
  @Query(
      """
        SELECT pt.request.request_id AS requestId,
               pt.request.description AS requestDescription,
               COALESCE(SUM(pt.amount), 0) AS totalAmount,
               COUNT(pt) AS transactionCount
        FROM PaymentTransaction pt
        WHERE pt.timestamp >= :start
          AND pt.timestamp < :end
          AND pt.status IN ('SUCCESSFUL', 'REFUNDED', 'PARTIALLY_REFUNDED')
        GROUP BY pt.request.request_id, pt.request.description
        ORDER BY COALESCE(SUM(pt.amount), 0) ASC
      """)
  List<PaymentRequestAggregateProjection> findPaymentRequestAggregatesForPeriod(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Aggregate outgoing external payments grouped by API client for a month. */
  @Query(
      """
        SELECT COALESCE(ac.name, 'Unknown') AS apiClientName,
               COALESCE(SUM(et.amount), 0) AS totalAmount,
               COUNT(et) AS transactionCount
        FROM ExternalTransaction et
        LEFT JOIN et.apiClient ac
        WHERE et.timestamp >= :start
          AND et.timestamp < :end
          AND et.status IN ('SUCCESSFUL', 'REFUNDED', 'PARTIALLY_REFUNDED')
        GROUP BY ac.id, ac.name
        ORDER BY COALESCE(SUM(et.amount), 0) ASC
      """)
  List<ExternalApiAggregateProjection> findExternalPaymentAggregatesForPeriod(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
