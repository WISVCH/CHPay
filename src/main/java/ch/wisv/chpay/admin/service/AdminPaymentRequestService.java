package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.admin.model.PaymentRequestMonthlyStats;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.repository.RequestRepository;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPaymentRequestService {

  private final RequestRepository requestRepository;
  private final TransactionRepository transactionRepository;

  @Autowired
  public AdminPaymentRequestService(
      RequestRepository requestRepository, TransactionRepository transactionRepository) {
    this.requestRepository = requestRepository;
    this.transactionRepository = transactionRepository;
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

  public PaymentRequest expireNow(PaymentRequest paymentRequest) {
    paymentRequest.setExpireAt(LocalDate.now());
    return requestRepository.save(paymentRequest);
  }

  public PaymentRequest updateExpireDate(PaymentRequest paymentRequest, LocalDate expireAt) {
    paymentRequest.setExpireAt(expireAt);
    return requestRepository.save(paymentRequest);
  }

  /**
   * Gets fulfilment counts per month for a specific payment request.
   *
   * @param requestId the UUID of the payment request
   * @return a list of monthly stats entries ordered by month descending
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<PaymentRequestMonthlyStats> getFulfilmentsByMonth(UUID requestId) {
    List<Object[]> results = transactionRepository.countFulfilmentsByRequestIdPerMonth(requestId);
    return results.stream()
        .map(
            row ->
                new PaymentRequestMonthlyStats(
                    YearMonth.of((Integer) row[0], (Integer) row[1]),
                    ((Number) row[2]).longValue()))
        .collect(Collectors.toList());
  }
}
