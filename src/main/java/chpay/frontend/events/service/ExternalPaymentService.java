package chpay.frontend.events.service;

import chpay.frontend.events.CHPaymentRequest;
import chpay.frontend.events.CHPaymentResponse;

public interface ExternalPaymentService {
  CHPaymentResponse createTransaction(CHPaymentRequest request);
}
