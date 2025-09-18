package ch.wisv.chpay.core.exception;

public class IllegalRefundException extends RuntimeException {
  public IllegalRefundException(String message) {
    super(message);
  }

  public IllegalRefundException(String message, Throwable cause) {
    super(message, cause);
  }
}
