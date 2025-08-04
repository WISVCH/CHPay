package chpay.DatabaseHandler.transactiondb.repositories;

import chpay.DatabaseHandler.transactiondb.entities.PendingWebhook;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingWebhookRepository extends JpaRepository<PendingWebhook, UUID> {
  List<PendingWebhook> findByStatusAndNextAttemptBefore(PendingWebhook.Status status, Instant time);
}
