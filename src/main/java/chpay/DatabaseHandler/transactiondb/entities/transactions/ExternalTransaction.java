package chpay.DatabaseHandler.transactiondb.entities.transactions;

import chpay.DatabaseHandler.transactiondb.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import lombok.Getter;

@Entity
@DiscriminatorValue("EXTERNAL_PAYMENT")
public class ExternalTransaction extends Transaction {

  @Getter
  @Column(nullable = false)
  private String redirectUrl;

  @Getter
  @Column(nullable = false)
  private String webhookUrl;

  @Getter
  @Column(nullable = false)
  private String fallbackUrl;

  private ExternalTransaction(
      User user,
      BigDecimal amount,
      String description,
      String redirectUrl,
      String webhookUrl,
      String fallbackUrl) {
    super(user, amount, description, TransactionStatus.PENDING, TransactionType.EXTERNAL_PAYMENT);
    this.redirectUrl = redirectUrl;
    this.webhookUrl = webhookUrl;
    this.fallbackUrl = fallbackUrl;
  }

  protected ExternalTransaction() {
    super();
    setType(TransactionType.EXTERNAL_PAYMENT);
  }

  @Override
  public boolean isFulfillable() {
    return true;
  }

  @Override
  public boolean isRefundable() {
    return true;
  }

  public static ExternalTransaction createExternalTransaction(
      User user,
      BigDecimal amount,
      String description,
      String redirectUrl,
      String webhookUrl,
      String fallbackUrl) {
    if (amount.compareTo(BigDecimal.ZERO) >= 0) {
      throw new IllegalArgumentException("The amount of a payment must be negative");
    }
    return new ExternalTransaction(user, amount, description, redirectUrl, webhookUrl, fallbackUrl);
  }
}
