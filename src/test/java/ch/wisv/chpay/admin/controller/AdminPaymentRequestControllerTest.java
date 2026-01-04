package ch.wisv.chpay.admin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.wisv.chpay.admin.service.AdminPaymentRequestService;
import ch.wisv.chpay.core.model.PaymentRequest;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

class AdminPaymentRequestControllerTest {

  @Mock private AdminPaymentRequestService adminPaymentRequestService;

  private AdminPaymentRequestController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new AdminPaymentRequestController(adminPaymentRequestService);
  }

  @Test
  void updateExpireDateUpdatesWhenNotExpired() {
    UUID requestId = UUID.randomUUID();
    PaymentRequest paymentRequest = mock(PaymentRequest.class);
    LocalDate newExpireDate = LocalDate.now().plusDays(2);

    when(adminPaymentRequestService.getById(requestId)).thenReturn(Optional.of(paymentRequest));
    when(paymentRequest.isExpired()).thenReturn(false);
    when(paymentRequest.getRequest_id()).thenReturn(requestId);

    String result =
        controller.updateExpireDate(
            mock(Model.class), requestId.toString(), newExpireDate, mock(RedirectAttributes.class));

    verify(adminPaymentRequestService).updateExpireDate(paymentRequest, newExpireDate);
    assertEquals("redirect:/admin/payment-request/" + requestId, result);
  }

  @Test
  void updateExpireDateRejectsExpiredRequest() {
    UUID requestId = UUID.randomUUID();
    PaymentRequest paymentRequest = mock(PaymentRequest.class);
    LocalDate newExpireDate = LocalDate.now().plusDays(3);

    when(adminPaymentRequestService.getById(requestId)).thenReturn(Optional.of(paymentRequest));
    when(paymentRequest.isExpired()).thenReturn(true);

    assertThrows(
        IllegalStateException.class,
        () ->
            controller.updateExpireDate(
                mock(Model.class),
                requestId.toString(),
                newExpireDate,
                mock(RedirectAttributes.class)));

    verify(adminPaymentRequestService, never()).updateExpireDate(paymentRequest, newExpireDate);
  }

  @Test
  void updateExpireDateRejectsNonFutureDate() {
    UUID requestId = UUID.randomUUID();
    PaymentRequest paymentRequest = mock(PaymentRequest.class);
    LocalDate invalidDate = LocalDate.now();

    when(adminPaymentRequestService.getById(requestId)).thenReturn(Optional.of(paymentRequest));
    when(paymentRequest.isExpired()).thenReturn(false);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            controller.updateExpireDate(
                mock(Model.class),
                requestId.toString(),
                invalidDate,
                mock(RedirectAttributes.class)));

    verify(adminPaymentRequestService, never()).updateExpireDate(paymentRequest, invalidDate);
  }
}
