package ch.wisv.chpay.core.model.transaction;

import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("PAYMENT")
public class PaymentTransaction extends Transaction {

  @ManyToOne
  @JoinColumn(name = "request_id", nullable = false)
  private PaymentRequest request;

  private PaymentTransaction(
      User user, BigDecimal amount, String description, PaymentRequest request) {
    super(user, amount, description, TransactionStatus.PENDING, TransactionType.PAYMENT);
    this.request = request;
  }

  public PaymentTransaction() {
    super();
    setType(TransactionType.PAYMENT);
  }

  @Override
  public boolean isRefundable() {
    return true;
  }

  @Override
  public boolean isFulfillable() {
    return true;
  }

  @Override
  public PaymentRequest getRequest() {
    return request;
  }

  @Override
  public boolean supportsRequest() {
    return true;
  }

  /**
   * Creates a new payment transaction.
   *
   * @param user the user associated with the transaction
   * @param amount the negative amount representing the payment; must be less than zero
   * @param description a description detailing the purpose of the transaction
   * @return a new instance of a Transaction with a status of SUCCESSFUL and a type of PAYMENT
   * @throws IllegalArgumentException if the amount is zero or positive
   */
  public static PaymentTransaction createPaymentTransaction(
      User user, BigDecimal amount, String description, PaymentRequest request) {
    if (amount.compareTo(BigDecimal.ZERO) >= 0) {
      throw new IllegalArgumentException("The amount of a payment must be negative");
    }
    return new PaymentTransaction(user, amount, description, request);
  }
}
