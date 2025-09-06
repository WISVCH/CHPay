package chpay.frontend.events.controller;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.frontend.events.CHPaymentRequest;
import chpay.frontend.events.CHPaymentResponse;
import chpay.frontend.events.service.ExternalPaymentServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class ExternalPaymentController {

  private final ExternalPaymentServiceImpl externalPaymentService;
  private final UserRepository userRepository;
  private final TransactionRepository transactionRepository;

  @Autowired
  public ExternalPaymentController(
      ExternalPaymentServiceImpl externalPaymentService,
      UserRepository userRepository,
      TransactionRepository transactionRepository) {
    this.externalPaymentService = externalPaymentService;
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
  }

  /**
   * Creates an External Transaction from a CHPaymentRequest dto, saves it to the repository as
   * pending and returns a CHPaymentResponse wrapped in ResponseEntity.
   *
   * @param request the request dto
   * @return the created response entity
   */
  @PreAuthorize("hasRole('API_USER')")
  @PostMapping
  public ResponseEntity<CHPaymentResponse> createExternalPayment(
      @RequestBody CHPaymentRequest request) {
    // For API users, find or create user based on consumer email
    User user = userRepository.findByEmail(request.getConsumerEmail())
        .orElseThrow(() -> new NoSuchElementException("User not found for email: " + request.getConsumerEmail()));
    
    CHPaymentResponse response = externalPaymentService.createTransaction(request, user);
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
      @RequestParam UUID PaymentId, @NonNull HttpServletResponse response) {
    Optional<Transaction> tx = transactionRepository.findById(PaymentId);
    if (tx.isEmpty()) {
      response.setStatus(HttpStatus.NOT_FOUND.value());
      return null;
    }

    Transaction.TransactionStatus status = tx.get().getStatus();
    return ResponseEntity.ok(status);
  }
}
