package ch.wisv.chpay.core.model;

import ch.wisv.chpay.core.model.transaction.PaymentTransaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "requests")
@Getter
@NoArgsConstructor
public class PaymentRequest {

  @Column(name = "request_id", nullable = false, unique = true, updatable = false)
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID request_id;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String description;

  @Setter
  @Column(nullable = false)
  private boolean fulfilled;

  @Column(nullable = false)
  private boolean multiUse;

  @Setter
  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PaymentTransaction> transactions;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  public PaymentRequest(BigDecimal amount, String description, boolean multiUse) {
    this.amount = amount;
    this.description = description;
    this.fulfilled = false;
    this.multiUse = multiUse;
    this.createdAt = LocalDateTime.now();
    this.transactions = new ArrayList<>();
  }
}
