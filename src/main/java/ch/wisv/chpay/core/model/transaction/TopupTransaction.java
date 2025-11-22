package ch.wisv.chpay.core.model.transaction;

import ch.wisv.chpay.core.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.Setter;

@Entity
@DiscriminatorValue("TOPUP")
public class TopupTransaction extends Transaction {

  @Column @Setter private String mollieId;

  @ManyToOne
  @JoinColumn(name = "redirect_payment_id", referencedColumnName = "id")
  @Setter
  private Transaction redirectPayment;

  private TopupTransaction(User user, BigDecimal amount, String description) {
    super(user, amount, description, TransactionStatus.PENDING, TransactionType.TOP_UP);
  }

  @Override
  public boolean hasMollieId() {
    return mollieId != null && !mollieId.isEmpty();
  }

  @Override
  public String getMollieId() {
    return this.mollieId;
  }

  public Transaction getRedirectPayment() {
    return redirectPayment;
  }

  /**
   * Creates a new top-up transaction.
   *
   * @param user the user associated with the transaction
   * @param amount the amount to be topped up; must be greater than zero
   * @param description a description detailing the transaction
   * @return a new instance of a Transaction with a status of Pending and a type of TOP_UP
   * @throws IllegalArgumentException if the amount is less than or equal to zero
   */
  public static TopupTransaction createTopUpTransaction(
      User user, BigDecimal amount, String description) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount for a top-up cannot be negative or equal to zero");
    }
    return new TopupTransaction(user, amount, description);
  }

  protected TopupTransaction() {
    super();
    setType(TransactionType.TOP_UP);
  }
}
