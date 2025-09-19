package ch.wisv.chpay.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

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
