package ch.wisv.chpay.core.exception;

public class TransactionAlreadyFulfilled extends RuntimeException {
  public TransactionAlreadyFulfilled(String message) {
    super(message);
  }
}
