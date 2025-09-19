package ch.wisv.chpay.api.payment_request.controller;

import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class RequestApiController {

  private final TransactionRepository transactionRepository;

  public RequestApiController(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  /**
   * GET /api/requests/{requestId}/successful-payments
   *
   * <p>Returns JSON array of all successful payments for that request: [ { "transactionId": "...",
   * "payerName": "Alice Doe", "amount": "17.50", "timestamp": "2025-06-04T12:07:15" }, ... ]
   */
  @PreAuthorize("hasRole('API_USER')")
  @GetMapping("/{requestId}/successful-payments")
  public List<Map<String, String>> getSuccessfulPayments(
      @PathVariable("requestId") UUID requestId) {

    // Fetch all SUCCESSFUL transactions for this request, ordered by timestamp
    List<Transaction> payments =
        transactionRepository.findAllSuccessfulForRequest(
            requestId, Transaction.TransactionStatus.SUCCESSFUL);

    // Map each Transaction → minimal JSON containing only what the client needs
    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    return payments.stream()
        .map(
            tx ->
                Map.<String, String>of(
                    "transactionId", tx.getId().toString(),
                    "payerName", tx.getUser().getName(),
                    "amount", tx.getAmount().abs().toPlainString(),
                    "timestamp", tx.getTimestamp().format(fmt)))
        .collect(Collectors.toList());
  }
}
