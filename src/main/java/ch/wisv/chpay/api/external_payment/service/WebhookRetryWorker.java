package ch.wisv.chpay.api.external_payment.service;

import ch.wisv.chpay.core.model.PendingWebhook;
import ch.wisv.chpay.core.repository.PendingWebhookRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WebhookRetryWorker {

  private static final int MAX_RETRIES = 5;

  private final PendingWebhookRepository webhookRepo;

  private static final Logger logger = LoggerFactory.getLogger(WebhookRetryWorker.class);

  public WebhookRetryWorker(PendingWebhookRepository webhookRepo) {
    this.webhookRepo = webhookRepo;
  }

  /**
   * Schedules a task to retry pending webhooks to CH Events. Runs every minute, but requests are
   * tried with an exponential backoff up to MAX_RETRIES, after which the webhook is marked as
   * failed. If that fails, the webhook is marked as failed
   */
  @Scheduled(fixedDelay = 60000) // every 60 seconds
  public void retryPendingWebhooks() {
    List<PendingWebhook> pendingWebhooks =
        webhookRepo.findByStatusAndNextAttemptBefore(PendingWebhook.Status.PENDING, Instant.now());

    for (PendingWebhook wh : pendingWebhooks) {
      try {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> form = new HttpEntity<>(wh.getPayload(), headers);

        new RestTemplate().postForEntity(wh.getWebhookUrl(), form, String.class);

        wh.setStatus(PendingWebhook.Status.SENT);
        webhookRepo.save(wh);
      } catch (Exception e) {
        int retries = wh.getRetryCount() + 1;
        wh.setRetryCount(retries);

        if (retries >= MAX_RETRIES) {
          wh.setStatus(PendingWebhook.Status.FAILED);
          logger.error(
              "Failed to retry webhook with transaction {} after {} attempts",
              wh.getPayload(),
              retries,
              e);
        } else {
          wh.setNextAttempt(Instant.now().plusSeconds((long) Math.pow(2, retries) * 60));
        }

        webhookRepo.save(wh);
      }
    }
  }
}
