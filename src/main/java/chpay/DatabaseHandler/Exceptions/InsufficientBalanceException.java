package chpay.DatabaseHandler.Exceptions;

public class InsufficientBalanceException extends RuntimeException {

  public InsufficientBalanceException(String message) {
    super(message);
  }

  public InsufficientBalanceException(String message, Throwable cause) {
    super(message, cause);
  }
}
