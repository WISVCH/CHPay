package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.aop.CheckSystemNotFrozen;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.PaymentTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.RequestRepository;
import ch.wisv.chpay.core.repository.TransactionRepository;
import jakarta.persistence.LockTimeoutException;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestService {

  private final RequestRepository requestRepository;
  private final TransactionRepository transactionRepository;

  @Autowired
  public RequestService(
      RequestRepository requestRepository, TransactionRepository transactionRepository) {
    this.requestRepository = requestRepository;
    this.transactionRepository = transactionRepository;
  }

  /**
   * Creates a new payment request. Multi-use requests have no time-out or maximum number of uses.
   *
   * @param amount The amount to pay.
   * @param description A description of the payment request.
   * @param multiuse Whether the request is multiuse or not.
   * @return The created payment request.
   */
  @CheckSystemNotFrozen
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public PaymentRequest createRequest(BigDecimal amount, String description, boolean multiuse) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount for request must be positive.");
    }
    return requestRepository.save(new PaymentRequest(amount, description, multiuse));
  }

  /**
   * Creates a new PENDING transaction tied to a user based on a payment request.
   *
   * @param requestId ID of the request to be fulfilled.
   * @param payer User who is paying for the request.
   * @return The created transaction.
   */
  @CheckSystemNotFrozen
  @Retryable(
      retryFor = {PessimisticEntityLockException.class, LockTimeoutException.class},
      notRecoverable = {
        IllegalStateException.class,
        NoSuchElementException.class,
        IllegalArgumentException.class
      },
      backoff = @Backoff(delay = 200, multiplier = 2))
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @Transactional
  public PaymentTransaction transactionFromRequest(UUID requestId, User payer) {
    PaymentRequest request = requestRepository.findByIdForUpdate(requestId);

    if (request == null) {
      throw new NoSuchElementException();
    }

    if (request.isExpired()) {
      throw new IllegalStateException("Request has expired");
    }

    if (request.getFulfilments() > 0 && !request.isMultiUse()) {
      throw new IllegalStateException("Request is already fulfilled");
    }

    Optional<PaymentTransaction> existing =
        transactionRepository.findFirstByUserAndRequestAndStatus(
            payer, request, Transaction.TransactionStatus.PENDING);

    if (existing.isPresent()) {
      return existing.get();
    }

    PaymentTransaction tx =
        PaymentTransaction.createPaymentTransaction(
            payer, request.getAmount().negate(), request.getDescription(), request);

    requestRepository.save(request);

    return transactionRepository.save(tx);
  }

  /***
   * Return a request called by its id
   * @param paymentRequestId id of the request
   * @return the object of the request
   */
  @Transactional(readOnly = true)
  public Optional<PaymentRequest> getRequestById(UUID paymentRequestId) {
    return requestRepository.findById(paymentRequestId);
  }
}
