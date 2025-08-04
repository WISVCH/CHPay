package chpay.frontend.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.ExternalTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.frontend.events.service.ExternalPaymentServiceImpl;
import chpay.paymentbackend.service.TransactionService;
import chpay.shared.service.NotificationService;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class PayRequestPageControllerTest {

  @Mock private TransactionService transactionService;
  @Mock private TransactionRepository transactionRepository;
  @Mock private NotificationService notificationService;
  @Mock private ExternalPaymentServiceImpl externalPaymentServiceImpl;
  @Mock private Model model;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private PayRequestPageController payRequestPageController;

  private UUID transactionId;
  private User currentUser;

  @BeforeEach
  void setUp() {
    transactionId = UUID.randomUUID();
    currentUser = mock(User.class);
    Mockito.lenient().when(model.getAttribute("currentUser")).thenReturn(currentUser);
  }

  @Test
  void nonExternalTest() {
    PaymentTransaction transaction = mock(PaymentTransaction.class);
    when(transaction.getId()).thenReturn(transactionId);
    when(transaction.getType()).thenReturn(Transaction.TransactionType.PAYMENT);
    when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.of(transaction));

    String result =
        payRequestPageController.getPage(model, transactionId.toString(), redirectAttributes);

    assertEquals("redirect:/payment/complete/" + transactionId.toString(), result);
    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService).fullfillTransaction(transactionId, currentUser);
    verify(notificationService)
        .addSuccessMessage(eq(redirectAttributes), eq("Authorized Transaction"));
    verify(externalPaymentServiceImpl, never()).postToWebhook(any(), any());
  }

  @Test
  void externalTest() {
    ExternalTransaction externalTransaction = mock(ExternalTransaction.class);
    when(externalTransaction.getId()).thenReturn(transactionId);
    when(externalTransaction.getType()).thenReturn(Transaction.TransactionType.EXTERNAL_PAYMENT);
    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(externalTransaction));

    String result =
        payRequestPageController.getPage(model, transactionId.toString(), redirectAttributes);

    assertEquals("redirect:/external/" + transactionId, result);
    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService, never()).fullfillTransaction(any(), any());
    verify(notificationService, never()).addSuccessMessage(any(), any());
    verify(externalPaymentServiceImpl, never()).postToWebhook(any(), any());
  }

  @Test
  void notFoundTest() {
    when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> {
          payRequestPageController.getPage(model, transactionId.toString(), redirectAttributes);
        },
        "Transaction not found");

    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService, never()).fullfillTransaction(any(), any());
    verify(notificationService, never()).addSuccessMessage(any(), any());
    verify(externalPaymentServiceImpl, never()).postToWebhook(any(), any());
  }

  @Test
  void externalWebhookTest() {
    ExternalTransaction externalTransaction = mock(ExternalTransaction.class);
    when(externalTransaction.getId()).thenReturn(transactionId);
    externalTransaction.setType(Transaction.TransactionType.EXTERNAL_PAYMENT);
    when(externalTransaction.getStatus()).thenReturn(Transaction.TransactionStatus.PENDING);
    when(externalTransaction.getUser()).thenReturn(currentUser);
    String expectedWebhookReturn = "redirect:http://events.com/payment-complete";

    when(transactionRepository.findById(transactionId))
        .thenReturn(Optional.of(externalTransaction));
    when(externalPaymentServiceImpl.postToWebhook(transactionId.toString(), externalTransaction))
        .thenReturn(expectedWebhookReturn);

    String result = payRequestPageController.completeExternalTransaction(transactionId.toString());

    assertEquals(expectedWebhookReturn, result);
    verify(transactionRepository).findById(transactionId);
    verify(transactionService).fullfillExternalTransaction(transactionId, currentUser);
    verify(externalPaymentServiceImpl).postToWebhook(transactionId.toString(), externalTransaction);
  }

  @Test
  void externalFallbackTest() {
    ExternalTransaction externalTransaction = mock(ExternalTransaction.class);
    externalTransaction.setType(Transaction.TransactionType.EXTERNAL_PAYMENT);
    when(externalTransaction.getStatus()).thenReturn(Transaction.TransactionStatus.SUCCESSFUL);
    when(externalTransaction.getFallbackUrl()).thenReturn("http://events.com/fallback");

    when(transactionRepository.findById(transactionId))
        .thenReturn(Optional.of(externalTransaction));

    String result = payRequestPageController.completeExternalTransaction(transactionId.toString());

    assertEquals("redirect:http://events.com/fallback", result);
    verify(transactionRepository).findById(transactionId);
    verify(transactionService, never()).fullfillExternalTransaction(any(), any());
    verify(externalPaymentServiceImpl, never()).postToWebhook(any(), any());
  }

  @Test
  void externalNotFoundTest() {
    when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> {
          payRequestPageController.completeExternalTransaction(transactionId.toString());
        },
        "Transaction not found");

    verify(transactionRepository).findById(transactionId);
    verify(transactionService, never()).fullfillExternalTransaction(any(), any());
    verify(externalPaymentServiceImpl, never()).postToWebhook(any(), any());
  }

  @Test
  void externalExceptionTest() {
    ExternalTransaction externalTransaction = mock(ExternalTransaction.class);
    when(externalTransaction.getId()).thenReturn(transactionId);
    externalTransaction.setType(Transaction.TransactionType.EXTERNAL_PAYMENT);
    when(externalTransaction.getStatus()).thenReturn(Transaction.TransactionStatus.PENDING);
    when(externalTransaction.getUser()).thenReturn(currentUser);

    when(transactionRepository.findById(transactionId))
        .thenReturn(Optional.of(externalTransaction));
    when(externalPaymentServiceImpl.postToWebhook(transactionId.toString(), externalTransaction))
        .thenThrow(new RestClientException("Simulated RestClientException"));

    assertThrows(
        RestClientException.class,
        () -> {
          payRequestPageController.completeExternalTransaction(transactionId.toString());
        });

    verify(transactionRepository).findById(transactionId);
    verify(transactionService).fullfillExternalTransaction(transactionId, currentUser);
    verify(externalPaymentServiceImpl).postToWebhook(transactionId.toString(), externalTransaction);
  }
}
