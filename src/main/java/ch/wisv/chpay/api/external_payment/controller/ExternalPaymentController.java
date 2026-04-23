package ch.wisv.chpay.api.external_payment.controller;

import ch.wisv.chpay.api.external_payment.model.CHPaymentRequest;
import ch.wisv.chpay.api.external_payment.model.CHPaymentResponse;
import ch.wisv.chpay.api.external_payment.service.ExternalPaymentServiceImpl;
import ch.wisv.chpay.core.model.transaction.ExternalTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/external-payment")
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
   * <p>External transactions are always created anonymously and will be linked to a user when
   * payment is completed.
   *
   * @param request the request dto
   * @return the created response entity
   */
  @PreAuthorize("hasAuthority('SCOPE_external_payment')")
  @PostMapping
  public ResponseEntity<CHPaymentResponse> createExternalPayment(
      @RequestBody CHPaymentRequest request,
      @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
    UUID apiClientId = extractApiClientId(principal);
    CHPaymentResponse response = externalPaymentService.createTransaction(request, apiClientId);
    return ResponseEntity.ok(response);
  }

  /**
   * Mapping to get the status of a transaction based on the id. Used by events to update the status
   * of the payment.
   *
   * @param PaymentId the id of the transaction
   * @return Response entity containing the status ( one of {@code FAILED}, {@code PENDING} {@code
   *     SUCCESSFUL}
   */
  @PreAuthorize("hasAuthority('SCOPE_external_payment')")
  @GetMapping("/status")
  public ResponseEntity<Transaction.TransactionStatus> getExternalPaymentStatus(
      @RequestParam UUID PaymentId,
      @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
    UUID apiClientId = extractApiClientId(principal);
    Optional<Transaction> tx = transactionRepository.findById(PaymentId);
    if (tx.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Transaction transaction = tx.get();
    if (!(transaction instanceof ExternalTransaction externalTransaction)) {
      return ResponseEntity.notFound().build();
    }
    if (externalTransaction.getApiClient() == null
        || !externalTransaction.getApiClient().getId().equals(apiClientId)) {
      return ResponseEntity.notFound().build();
    }

    Transaction.TransactionStatus status = externalTransaction.getStatus();
    return ResponseEntity.ok(status);
  }

  private UUID extractApiClientId(OAuth2AuthenticatedPrincipal principal) {
    if (principal == null) {
      throw new AccessDeniedException("Missing API client principal");
    }
    Object rawClientId = principal.getAttributes().get("client_id");
    if (!(rawClientId instanceof String clientIdValue)) {
      throw new AccessDeniedException("Missing API client ID");
    }
    try {
      return UUID.fromString(clientIdValue);
    } catch (IllegalArgumentException ex) {
      throw new AccessDeniedException("Invalid API client ID", ex);
    }
  }
}
