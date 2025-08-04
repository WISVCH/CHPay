package chpay.DatabaseHandler.transactiondb.entities.transactions;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.entities.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(
    name = "transactions",
    indexes = {
      @Index(name = "idx_user_id", columnList = "user_id"),
      @Index(name = "idx_transaction_user_time", columnList = "user_id, timestamp")
    })
@Getter
@NoArgsConstructor
public class Transaction {

  public enum TransactionType {
    TOP_UP,
    REFUND,
    PAYMENT,
    EXTERNAL_PAYMENT
  }

  public enum TransactionStatus {
    SUCCESSFUL,
    PENDING,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private String description;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionStatus status;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType type;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @PrePersist
  protected void onCreate() {
    if (this.timestamp == null) {
      this.timestamp = LocalDateTime.now();
    }
  }

  public Transaction(
      User user,
      BigDecimal amount,
      String description,
      TransactionStatus status,
      TransactionType type) {
    this.user = user;
    this.amount = amount;
    this.description = description;
    this.status = status;
    this.type = type;
  }

  public boolean supportsRequest() {
    return false;
  }

  public PaymentRequest getRequest() {
    return null;
  }

  public boolean supportsRefunds() {
    return false;
  }

  public Transaction getRefundOf() {
    return null;
  }

  public boolean hasMollieId() {
    return false;
  }

  public String getMollieId() {
    return null;
  }

  public boolean isFulfillable() {
    return false;
  }

  public boolean isRefundable() {
    return false;
  }
}
