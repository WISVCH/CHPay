package ch.wisv.chpay.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "statistics",
    indexes = {@Index(name = "idx_time", columnList = "date")})
@Getter
@NoArgsConstructor
public class Statistic {
  public enum StatisticType {
    BALANCE,
    INCOMING_FUNDS,
    OUTGOING_FUNDS,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Statistic.StatisticType type;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column(nullable = false)
  private LocalDate date;

  @PrePersist
  protected void onCreate() {
    if (this.timestamp == null) {
      this.timestamp = LocalDateTime.now();
    }
    if (this.date == null) {
      this.date = this.timestamp.toLocalDate();
    }
  }

  public Statistic(StatisticType type, BigDecimal amount) {
    this.type = type;
    this.amount = amount;
  }

  public Statistic(StatisticType type, BigDecimal amount, LocalDateTime timestamp) {
    this.type = type;
    this.amount = amount;
    this.timestamp = timestamp;
    this.date = timestamp.toLocalDate();
  }
}
