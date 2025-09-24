package ch.wisv.chpay.core.model.transaction;

import ch.wisv.chpay.core.model.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import lombok.Setter;

@Entity
@DiscriminatorValue("REFUND")
public class RefundTransaction extends Transaction {

  @ManyToOne
  @Setter
  @JoinColumn(name = "refund_of", nullable = false)
  private Transaction refundOf;

  public RefundTransaction(User user, BigDecimal amount, String description) {
    super(
        user,
        amount,
        description,
        TransactionStatus.SUCCESSFUL,
        Transaction.TransactionType.REFUND);
  }

  public RefundTransaction() {
    super();
    setType(Transaction.TransactionType.REFUND);
  }

  @Override
  public boolean supportsRefunds() {
    return true;
  }

  @Override
  public Transaction getRefundOf() {
    return refundOf;
  }

  /**
   * Creates a new refund transaction associated with an existing transaction.
   *
   * @param user the user initiating the refund
   * @param amount the amount to be refunded; must be greater than zero
   * @param original the original transaction that is being refunded
   * @return a new instance of a Transaction with a status of SUCCESSFUL and a type of REFUND
   * @throws IllegalArgumentException if the refund amount is less than or equal to zero
   */
  public static RefundTransaction createRefund(User user, BigDecimal amount, Transaction original) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount for a refund cannot be negative or equal to zero");
    }
    String desc = "Refund of transaction with ID: " + original.getId();
    RefundTransaction tx = new RefundTransaction(user, amount, desc);
    tx.refundOf = original;
    return tx;
  }
}
