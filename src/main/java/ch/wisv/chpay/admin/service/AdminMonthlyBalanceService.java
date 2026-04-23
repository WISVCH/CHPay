package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.admin.model.MonthlyBalanceBreakdown;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMonthlyBalanceService {

  private final TransactionRepository transactionRepository;

  public AdminMonthlyBalanceService(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public MonthlyBalanceBreakdown getBreakdownForMonth(YearMonth yearMonth) {
    LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    TransactionRepository.AmountCountProjection topUpAggregate =
        transactionRepository.findTopUpAggregateForPeriod(start, end);
    TransactionRepository.AmountCountProjection refundAggregate =
        transactionRepository.findRefundAggregateForPeriod(start, end);
    List<TransactionRepository.PaymentRequestAggregateProjection> paymentAggregates =
        transactionRepository.findPaymentRequestAggregatesForPeriod(start, end);
    List<TransactionRepository.ExternalApiAggregateProjection> externalAggregates =
        transactionRepository.findExternalPaymentAggregatesForPeriod(start, end);

    IncomingLine topUps = toIncomingLine(topUpAggregate);
    IncomingLine refunds = toIncomingLine(refundAggregate);

    List<MonthlyBalanceBreakdown.PaymentRequestLine> paymentsByRequest =
        paymentAggregates.stream()
            .map(
                row ->
                    new MonthlyBalanceBreakdown.PaymentRequestLine(
                        row.getRequestId(),
                        row.getRequestDescription(),
                        row.getTotalAmount().abs(),
                        row.getTransactionCount()))
            .toList();

    List<MonthlyBalanceBreakdown.ExternalClientLine> externalByClient =
        externalAggregates.stream()
            .map(
                row ->
                    new MonthlyBalanceBreakdown.ExternalClientLine(
                        row.getApiClientName(),
                        row.getTotalAmount().abs(),
                        row.getTransactionCount()))
            .toList();

    return new MonthlyBalanceBreakdown(
        topUps.amount(),
        topUps.transactionCount(),
        refunds.amount(),
        refunds.transactionCount(),
        paymentsByRequest,
        externalByClient);
  }

  private IncomingLine toIncomingLine(TransactionRepository.AmountCountProjection aggregate) {
    if (aggregate == null) {
      return new IncomingLine(BigDecimal.ZERO, 0L);
    }
    return new IncomingLine(
        aggregate.getTotalAmount() == null ? BigDecimal.ZERO : aggregate.getTotalAmount(),
        aggregate.getTransactionCount() == null ? 0L : aggregate.getTransactionCount());
  }

  private record IncomingLine(BigDecimal amount, long transactionCount) {}
}
