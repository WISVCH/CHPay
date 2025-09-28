package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.repository.RequestRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPaymentRequestService {

  private final RequestRepository requestRepository;

  @Autowired
  public AdminPaymentRequestService(RequestRepository requestRepository) {
    this.requestRepository = requestRepository;
  }

  /**
   * Gets all PaymentRequest.
   *
   * @return a list of PaymentRequest objects for the specified month
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<PaymentRequest> getAll() {
    return requestRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  public Optional<PaymentRequest> getById(UUID id) {
    return requestRepository.findById(id);
  }

  public PaymentRequest expireAndSave(PaymentRequest paymentRequest) {
    paymentRequest.setExpired(true);
    return requestRepository.save(paymentRequest);
  }
}
