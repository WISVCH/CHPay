package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.PendingWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PendingWebhookRepository extends JpaRepository<PendingWebhook, UUID> {
  List<PendingWebhook> findByStatusAndNextAttemptBefore(PendingWebhook.Status status, Instant time);
}
