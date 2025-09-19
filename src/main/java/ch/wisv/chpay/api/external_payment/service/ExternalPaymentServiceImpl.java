package ch.wisv.chpay.api.external_payment.service;

import ch.wisv.chpay.api.external_payment.model.CHPaymentRequest;
import ch.wisv.chpay.api.external_payment.model.CHPaymentResponse;
import ch.wisv.chpay.core.model.PendingWebhook;
import ch.wisv.chpay.core.model.transaction.ExternalTransaction;
import ch.wisv.chpay.core.repository.PendingWebhookRepository;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ExternalPaymentServiceImpl implements ExternalPaymentService {

  private final RestTemplate restTemplate;

  private final PendingWebhookRepository webhookRepo;

  @Value("${spring.application.base-url}")
  private String CHPayUri;

  private static final Logger logger = LoggerFactory.getLogger(ExternalPaymentServiceImpl.class);
  private final TransactionRepository repository;

  public ExternalPaymentServiceImpl(
      TransactionRepository repository,
      PendingWebhookRepository webhookRepo,
      RestTemplate restTemplate) {
    this.repository = repository;
    this.webhookRepo = webhookRepo;
    this.restTemplate = restTemplate;
  }

  /**
   * Creates a new external transaction based on the provided payment request. The transaction is
   * saved in the repository without a linked user, and a response containing the transaction ID and
   * checkout URL is returned. The user will be linked to the transaction when payment is completed.
   *
   * @param request the payment request containing details such as amount, description, redirect
   *     URL, webhook URL, and fallback URL
   * @return a {@code CHPaymentResponse} with the transaction ID and checkout URL
   */
  @Override
  @Transactional
  public CHPaymentResponse createTransaction(CHPaymentRequest request) {
    logger.info(
        "Creating external transaction for amount: {}, consumer: {}",
        request.getAmount(),
        request.getConsumerEmail());

    ExternalTransaction tx =
        ExternalTransaction.createExternalTransaction(
            request.getAmount().negate(),
            request.getDescription(),
            request.getRedirectURL(),
            request.getWebhookURL(),
            request.getFallbackURL());
    repository.save(tx);

    String checkoutUrl = CHPayUri + "/payment/transaction/" + tx.getId();

    logger.info(
        "Created external transaction with ID: {}, checkout URL: {}", tx.getId(), checkoutUrl);

    return new CHPaymentResponse(tx.getId().toString(), checkoutUrl);
  }

  /**
   * Sends an HTTP POST request to the specified webhook URL contained within the given {@code
   * ExternalTransaction} object. If the webhook URL is valid, the method attempts to make the
   * request and redirects to a URL on Events. Retries are applied in case of specific locking or
   * timeout exceptions.
   *
   * @param id the identifier of the transaction being processed
   * @param etx the {@code ExternalTransaction} object containing details such as webhook URL,
   *     redirect URL, and fallback URL
   * @return a string representing the redirection URL
   */
  @Retryable(
      retryFor = {RestClientException.class},
      maxAttempts = 5,
      backoff = @Backoff(delay = 200, multiplier = 2))
  @Transactional
  public String postToWebhook(String id, ExternalTransaction etx) throws RestClientException {
    String webhookUrl = etx.getWebhookUrl();
    if (webhookUrl != null && !webhookUrl.isBlank()) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      HttpEntity<String> formEntity = new HttpEntity<>("id=" + id, headers);

      restTemplate.postForEntity(webhookUrl, formEntity, String.class);

      return "redirect:" + etx.getRedirectUrl();
    }
    return "redirect:" + etx.getFallbackUrl() + "?message=Payment+Failed";
  }

  // Not sure if redirecting the user is ideal as the webhook should only fail if Events is
  // down or similar.
  /**
   * Persists the web request to be sent later by the {@code WebhookRetryWorker} later with the same
   * payload.
   *
   * @param e The exception that was thrown. Will be logged.
   * @param id The id of the transaction that failed. Will be used as payload.
   * @param etx The {@code ExternalTransaction} object containing details such as webhook URL,
   * @return Redirects the user to the fallback URL with a message indicating that the payment
   *     failed and will be retried later seconds.
   */
  @Recover
  public String recover(RestClientException e, String id, ExternalTransaction etx) {
    logger.warn(
        "Posting to CH Events webhook failed, saving for retry. id={}, webhookUrl={}",
        id,
        etx.getWebhookUrl(),
        e);

    PendingWebhook pending = new PendingWebhook();
    pending.setWebhookUrl(etx.getWebhookUrl());
    pending.setPayload("id=" + id);
    pending.setRetryCount(0);
    pending.setStatus(PendingWebhook.Status.PENDING);
    pending.setNextAttempt(Instant.now().plusSeconds(60));

    webhookRepo.save(pending);

    return "redirect:" + etx.getFallbackUrl() + "?message=Payment+Failed+Retrying+in+60+seconds";
  }
}
