package chpay.frontend.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.Exceptions.TransactionAlreadyFulfilled;
import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.paymentbackend.service.RequestService;
import chpay.paymentbackend.service.TransactionService;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class PaymentPageControllerTest {

  private static final String MODEL_ATTR_TX = "transaction";
  private static final String MODEL_ATTR_URL_PAGE = "urlPage";

  @Mock private RequestService requestService;
  @Mock private TransactionService transactionService;
  @Mock private Model model;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private PaymentPageController paymentPageController;

  private UUID requestId;
  private UUID transactionId;
  private User currentUser;
  private PaymentRequest paymentRequest;
  private PaymentTransaction paymentTransaction;

  @BeforeEach
  void setUp() {
    requestId = UUID.randomUUID();
    transactionId = UUID.randomUUID();
    currentUser = mock(User.class);
    Mockito.lenient().when(currentUser.getId()).thenReturn(UUID.randomUUID());
    Mockito.lenient().when(currentUser.getName()).thenReturn("testuser");

    paymentRequest = mock(PaymentRequest.class);
    Mockito.lenient().when(paymentRequest.getRequest_id()).thenReturn(requestId);

    paymentTransaction = mock(PaymentTransaction.class);
    Mockito.lenient().when(paymentTransaction.getId()).thenReturn(transactionId);
    paymentTransaction.setStatus(Transaction.TransactionStatus.PENDING);

    Mockito.lenient().when(model.getAttribute("currentUser")).thenReturn(currentUser);
  }

  @Test
  void validRedirectTest() {
    when(requestService.getRequestById(requestId)).thenReturn(Optional.of(paymentRequest));
    when(requestService.transactionFromRequest(requestId, currentUser))
        .thenReturn(paymentTransaction);

    String result =
        paymentPageController.redirectToPayTransaction(
            model, requestId.toString(), redirectAttributes);

    assertEquals("redirect:/payment/transaction/" + transactionId, result);
    verify(requestService).getRequestById(requestId);
    verify(requestService).transactionFromRequest(requestId, currentUser);
    verify(model).addAttribute(MODEL_ATTR_TX, paymentTransaction);
  }

  @Test
  void notFoundTest() {
    when(requestService.getRequestById(requestId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> {
          paymentPageController.redirectToPayTransaction(
              model, requestId.toString(), redirectAttributes);
        },
        "Request not found");

    verify(requestService).getRequestById(requestId);
    verify(requestService, never()).transactionFromRequest(any(), any());
    verify(model, never()).addAttribute(any(), any());
  }

  @Test
  void alreadyDoneTest() {
    when(requestService.getRequestById(requestId)).thenReturn(Optional.of(paymentRequest));
    when(paymentRequest.isFulfilled()).thenReturn(true);
    assertThrows(
        IllegalStateException.class,
        () -> {
          paymentPageController.redirectToPayTransaction(
              model, requestId.toString(), redirectAttributes);
        },
        "Request is already fulfilled!");

    verify(requestService).getRequestById(requestId);
    verify(requestService, never()).transactionFromRequest(any(), any());
    verify(model, never()).addAttribute(any(), any());
  }

  @Test
  void paymentPageTest() {
    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(paymentTransaction));
    when(paymentTransaction.getStatus()).thenReturn(Transaction.TransactionStatus.PENDING);
    String result =
        paymentPageController.showPaymentPage(model, transactionId.toString(), redirectAttributes);

    assertEquals("payment", result);
    verify(transactionService).getTransactionById(transactionId);
    verify(model).addAttribute(MODEL_ATTR_TX, paymentTransaction);
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "payment");
  }

  @Test
  void noSuchElementTest() {
    when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> {
          paymentPageController.showPaymentPage(
              model, transactionId.toString(), redirectAttributes);
        },
        "Transaction not found");

    verify(transactionService).getTransactionById(transactionId);
    verify(model, never()).addAttribute(any(), any());
  }

  @Test
  void alreadyFulfilledTest() {
    paymentTransaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(paymentTransaction));
    when(paymentTransaction.getStatus()).thenReturn(Transaction.TransactionStatus.SUCCESSFUL);
    assertThrows(
        TransactionAlreadyFulfilled.class,
        () -> {
          paymentPageController.showPaymentPage(
              model, transactionId.toString(), redirectAttributes);
        },
        "This payment has already been fulfilled, or failed.");

    verify(transactionService).getTransactionById(transactionId);
    verify(model, never()).addAttribute(any(), any());
  }

  @Test
  void failedTest() {
    paymentTransaction.setStatus(Transaction.TransactionStatus.FAILED);
    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(paymentTransaction));
    when(paymentTransaction.getStatus()).thenReturn(Transaction.TransactionStatus.FAILED);
    assertThrows(
        TransactionAlreadyFulfilled.class,
        () -> {
          paymentPageController.showPaymentPage(
              model, transactionId.toString(), redirectAttributes);
        },
        "This payment has already been fulfilled, or failed.");

    verify(transactionService).getTransactionById(transactionId);
    verify(model, never()).addAttribute(any(), any());
  }
}
