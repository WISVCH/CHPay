package chpay.DatabaseHandler.transactiondb.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PendingWebhook {

  @Id @GeneratedValue private UUID id;

  private String webhookUrl;

  private String payload;

  private int retryCount;

  private Instant nextAttempt;

  @Enumerated(EnumType.STRING)
  private Status status;

  public enum Status {
    PENDING,
    FAILED,
    SENT
  }
}
