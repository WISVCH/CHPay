package chpay.paymentbackend.service;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.repositories.RequestRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RequestExpirationService {

  private final RequestRepository requestRepository;

  @Autowired
  public RequestExpirationService(RequestRepository requestRepository) {
    this.requestRepository = requestRepository;
  }

  @Value("${chpay.paymentrequests.expire-every-months}")
  private long expirationTime;

  /**
   * Marks old, unfulfilled payment requests as fulfilled and persists the changes in the database.
   *
   * <p>This method identifies payment requests that are older than a specified expiration period,
   * marks them as fulfilled, and updates these requests in the database. The expiration period is
   * defined through the configuration property `chpay.paymentrequests.expire-every-months`.
   *
   * <p>The process runs daily at 3:00 AM, as scheduled by the cron configuration.
   */
  @Transactional
  @Scheduled(cron = "0 0 3 * * *") // runs every day at 3 am
  public void expireOldRequests() {
    LocalDateTime cutoff = LocalDateTime.now().minusMonths(expirationTime);

    List<PaymentRequest> oldRequests = requestRepository.findOldRequestsForUpdate(cutoff);

    for (PaymentRequest request : oldRequests) {
      request.setFulfilled(true);
    }

    if (!oldRequests.isEmpty()) {
      requestRepository.saveAll(oldRequests);
    }
  }
}
