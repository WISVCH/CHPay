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
      BigDecimal amount,
      String description,
      String redirectUrl,
      String webhookUrl,
      String fallbackUrl) {
    super(amount, description, TransactionStatus.PENDING, TransactionType.EXTERNAL_PAYMENT);
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
      BigDecimal amount,
      String description,
      String redirectUrl,
      String webhookUrl,
      String fallbackUrl) {
    if (amount.compareTo(BigDecimal.ZERO) >= 0) {
      throw new IllegalArgumentException("The amount of a payment must be negative");
    }
    if (description == null || description.trim().isEmpty()) {
      throw new IllegalArgumentException("Description cannot be null or empty");
    }
    if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("Redirect URL cannot be null or empty");
    }
    if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("Webhook URL cannot be null or empty");
    }
    if (fallbackUrl == null || fallbackUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("Fallback URL cannot be null or empty");
    }
    return new ExternalTransaction(amount, description, redirectUrl, webhookUrl, fallbackUrl);
  }

  /**
   * Links a user to this external transaction. This should only be called when the transaction
   * is being paid by a user who didn't exist when the transaction was created.
   *
   * @param user the user to link to this transaction
   * @throws IllegalStateException if the transaction already has a user or is not in PENDING status
   */
  public void linkUser(User user) {
    if (this.getUser() != null) {
      throw new IllegalStateException("Transaction already has a linked user");
    }
    if (this.getStatus() != TransactionStatus.PENDING) {
      throw new IllegalStateException("Can only link user to pending transactions");
    }
    this.setUser(user);
  }
}
