package ch.wisv.chpay.admin.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MonthlyBalanceBreakdown(
    BigDecimal topUpsAmount,
    long topUpsTransactionCount,
    BigDecimal refundsAmount,
    long refundsTransactionCount,
    List<PaymentRequestLine> paymentsByRequest,
    List<ExternalClientLine> externalPaymentsByClient) {

  public static MonthlyBalanceBreakdown empty() {
    return new MonthlyBalanceBreakdown(
        BigDecimal.ZERO,
        0L,
        BigDecimal.ZERO,
        0L,
        List.of(),
        List.of());
  }

  public BigDecimal totalIncoming() {
    return topUpsAmount.add(refundsAmount);
  }

  public BigDecimal totalOutgoingPayments() {
    return paymentsByRequest.stream()
        .map(PaymentRequestLine::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public long totalOutgoingPaymentsTransactionCount() {
    return paymentsByRequest.stream().mapToLong(PaymentRequestLine::transactionCount).sum();
  }

  public BigDecimal totalOutgoingExternalPayments() {
    return externalPaymentsByClient.stream()
        .map(ExternalClientLine::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public long totalOutgoingExternalPaymentsTransactionCount() {
    return externalPaymentsByClient.stream().mapToLong(ExternalClientLine::transactionCount).sum();
  }

  public BigDecimal totalOutgoing() {
    return totalOutgoingPayments().add(totalOutgoingExternalPayments());
  }

  public BigDecimal difference() {
    return totalIncoming().subtract(totalOutgoing());
  }

  public record PaymentRequestLine(
      UUID requestId, String requestDescription, BigDecimal amount, long transactionCount) {}

  public record ExternalClientLine(String clientName, BigDecimal amount, long transactionCount) {}
}
