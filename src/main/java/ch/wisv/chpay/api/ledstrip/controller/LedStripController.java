package ch.wisv.chpay.api.ledstrip.controller;

import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leds")
public class LedStripController {

  private static final List<Transaction.TransactionType> PAYMENT_TYPES =
      List.of(Transaction.TransactionType.PAYMENT, Transaction.TransactionType.EXTERNAL_PAYMENT);
  private static final List<Transaction.TransactionStatus> FULFILLED_STATUSES =
      List.of(
          Transaction.TransactionStatus.SUCCESSFUL,
          Transaction.TransactionStatus.REFUNDED,
          Transaction.TransactionStatus.PARTIALLY_REFUNDED);

  private final TransactionRepository transactionRepository;

  public LedStripController(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  public record LatestTransactionResponse(
      String description, BigDecimal amount, LocalDateTime timestamp) {}

  @GetMapping("/ping")
  public ResponseEntity<Void> ping() {
    return ResponseEntity.ok().build();
  }

  @GetMapping("/latest")
  public ResponseEntity<LatestTransactionResponse> getLatestTransaction() {
    return transactionRepository
        .findFirstByTypeInAndStatusInOrderByTimestampDesc(PAYMENT_TYPES, FULFILLED_STATUSES)
        .map(
            t ->
                ResponseEntity.ok(
                    new LatestTransactionResponse(
                        t.getDescription(), t.getTimestamp(), t.getType())))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
