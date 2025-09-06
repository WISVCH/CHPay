package chpay.frontend.events.controller;

import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.frontend.events.CHPaymentRequest;
import chpay.frontend.events.CHPaymentResponse;
import chpay.frontend.events.service.ExternalPaymentServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class ExternalPaymentController {

  private final ExternalPaymentServiceImpl externalPaymentService;
  private final TransactionRepository transactionRepository;

  @Autowired
  public ExternalPaymentController(
      ExternalPaymentServiceImpl externalPaymentService,
      TransactionRepository transactionRepository) {
    this.externalPaymentService = externalPaymentService;
    this.transactionRepository = transactionRepository;
  }

  /**
   * Creates an External Transaction from a CHPaymentRequest dto, saves it to the repository as
   * pending and returns a CHPaymentResponse wrapped in ResponseEntity.
   * 
   * External transactions are always created anonymously and will be linked to a user
   * when payment is completed.
   *
   * @param request the request dto
   * @return the created response entity
   */
  @PreAuthorize("hasRole('API_USER')")
  @PostMapping
  public ResponseEntity<CHPaymentResponse> createExternalPayment(
    @RequestBody CHPaymentRequest request) {
    CHPaymentResponse response = externalPaymentService.createTransaction(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Mapping to get the status of a transaction based on the id. Used by events to update the status
   * of the payment.
   *
   * @param PaymentId the id of the transaction
   * @param response the HttpServletResponse
   * @return Response entity containing the status ( one of {@code FAILED}, {@code PENDING} {@code
   *     SUCCESSFUL}
   */
  @PreAuthorize("hasRole('API_USER')")
  @GetMapping("/status")
  public ResponseEntity<Transaction.TransactionStatus> getExternalPaymentStatus(
      @RequestParam UUID PaymentId) {
    Optional<Transaction> tx = transactionRepository.findById(PaymentId);
    if (tx.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Transaction.TransactionStatus status = tx.get().getStatus();
    return ResponseEntity.ok(status);
  }
}
