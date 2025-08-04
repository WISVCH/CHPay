package chpay.DatabaseHandler.Exceptions;

public class TransactionAlreadyFulfilled extends RuntimeException {
  public TransactionAlreadyFulfilled(String message) {
    super(message);
  }
}
