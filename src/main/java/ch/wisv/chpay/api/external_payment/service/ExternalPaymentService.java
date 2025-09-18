package ch.wisv.chpay.api.external_payment.service;

import ch.wisv.chpay.api.external_payment.model.CHPaymentRequest;
import ch.wisv.chpay.api.external_payment.model.CHPaymentResponse;

public interface ExternalPaymentService {
  CHPaymentResponse createTransaction(CHPaymentRequest request);
}
