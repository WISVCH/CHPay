package ch.wisv.chpay.api.client.service;

public class InvalidApiClientTokenException extends RuntimeException {
  public InvalidApiClientTokenException(String message) {
    super(message);
  }
}
