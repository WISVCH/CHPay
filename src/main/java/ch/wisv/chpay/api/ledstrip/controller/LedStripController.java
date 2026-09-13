package ch.wisv.chpay.api.ledstrip.controller;

import ch.wisv.chpay.core.model.Confetti;
import ch.wisv.chpay.core.model.LedPattern;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
      String description,
      LocalDateTime timestamp,
      Transaction.TransactionType type,
      Integer r,
      Integer g,
      Integer b,
      String pattern) {}

  @PreAuthorize("hasAuthority('SCOPE_ledstrip')")
  @GetMapping("/ping")
  public ResponseEntity<Void> ping() {
    return ResponseEntity.ok().build();
  }

  @PreAuthorize("hasAuthority('SCOPE_ledstrip')")
  @GetMapping("/latest")
  public ResponseEntity<LatestTransactionResponse> getLatestTransaction() {
    return transactionRepository
        .findFirstByTypeInAndStatusInOrderByTimestampDesc(PAYMENT_TYPES, FULFILLED_STATUSES)
        .map(
            t -> {
              User user = t.getUser();
              Confetti confetti = user != null ? user.getConfetti() : null;
              String ledColor = confetti != null ? confetti.getLedColor() : null;
              LedPattern ledPattern = confetti != null ? confetti.getLedPattern() : null;
              Integer r = null, g = null, b = null;
              if (ledColor != null && ledColor.length() == 7) {
                r = Integer.parseInt(ledColor.substring(1, 3), 16);
                g = Integer.parseInt(ledColor.substring(3, 5), 16);
                b = Integer.parseInt(ledColor.substring(5, 7), 16);
              }
              String pattern = ledPattern != null ? ledPattern.name() : null;
              return ResponseEntity.ok(
                  new LatestTransactionResponse(
                      t.getDescription(), t.getTimestamp(), t.getType(), r, g, b, pattern));
            })
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
