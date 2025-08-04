package chpay.frontend.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.RefundTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.frontend.customer.controller.AdministratorTransactionPageController;
import chpay.paymentbackend.service.TransactionService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
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
public class AdministratorTransactionPageControllerTest {

  private static final String MODEL_ATTR_TX = "transaction";
  private static final String MODEL_ATTR_REFUND_ID = "refundId";
  private static final String MODEL_ATTR_REQUEST_ID = "requestId";
  private static final String MODEL_ATTR_REFUND_POSSIBLE = "refundPossible";
  private static final String MODEL_ATTR_URL_PAGE = "urlPage";

  @Mock private TransactionService transactionService;
  @Mock private NotificationService notificationService;
  @Mock private Model model;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private AdministratorTransactionPageController controller;

  private UUID transactionId;
  private Transaction mockTransaction;

  @BeforeEach
  void setUp() {
    transactionId = UUID.randomUUID();
    mockTransaction = mock(Transaction.class);
    Mockito.lenient().when(mockTransaction.getId()).thenReturn(transactionId);
    Mockito.lenient().when(mockTransaction.getAmount()).thenReturn(new BigDecimal("100.00"));
    Mockito.lenient()
        .when(mockTransaction.getStatus())
        .thenReturn(Transaction.TransactionStatus.SUCCESSFUL);
  }

  @Test
  void fullRefundRedirectTest() {
    UUID refundTxId = UUID.randomUUID();
    RefundTransaction mockRefund = mock(RefundTransaction.class);
    when(mockRefund.getId()).thenReturn(refundTxId);
    Mockito.lenient().when(mockRefund.getAmount()).thenReturn(new BigDecimal("100.00"));

    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(mockTransaction));
    when(transactionService.refundTransaction(transactionId)).thenReturn(mockRefund);

    String result =
        controller.processRefund(model, transactionId.toString(), null, redirectAttributes);

    assertEquals("redirect:/admin/transactions/" + refundTxId.toString(), result);
    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService).refundTransaction(transactionId);
    verify(transactionService, never()).partialRefund(any(), any());
  }

  @Test
  void partialRefundRedirectTest() {
    UUID refundTxId = UUID.randomUUID();
    String partialAmount = "50.00";
    RefundTransaction mockPartialRefund = mock(RefundTransaction.class);
    when(mockPartialRefund.getId()).thenReturn(refundTxId);
    Mockito.lenient().when(mockPartialRefund.getAmount()).thenReturn(new BigDecimal("100.00"));

    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(mockTransaction));
    when(transactionService.partialRefund(transactionId, new BigDecimal(partialAmount)))
        .thenReturn(mockPartialRefund);

    String result =
        controller.processRefund(
            model, transactionId.toString(), partialAmount, redirectAttributes);

    assertEquals("redirect:/admin/transactions/" + refundTxId.toString(), result);
    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService).partialRefund(transactionId, new BigDecimal(partialAmount));
    verify(transactionService, never()).refundTransaction(any());
  }

  @Test
  void transactionNotFoundTest() {
    when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> {
          controller.processRefund(model, transactionId.toString(), null, redirectAttributes);
        },
        "Transaction not found");

    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService, never()).refundTransaction(any());
    verify(transactionService, never()).partialRefund(any(), any());
  }

  @Test
  void showTransactiOPageTest() {
    BigDecimal nonRefundedAmount = new BigDecimal("50.00");
    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(mockTransaction));
    when(transactionService.getNonRefundedAmount(transactionId)).thenReturn(nonRefundedAmount);

    String result =
        controller.showTransactionPage(model, transactionId.toString(), redirectAttributes);

    assertEquals("admin-transaction", result);
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "adminTransactions");
    verify(model).addAttribute(MODEL_ATTR_TX, mockTransaction);
    verify(model).addAttribute(MODEL_ATTR_REFUND_ID, null);
    verify(model).addAttribute(MODEL_ATTR_REQUEST_ID, null);
    verify(model).addAttribute(MODEL_ATTR_REFUND_POSSIBLE, nonRefundedAmount);
    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService).getNonRefundedAmount(transactionId);
  }

  @Test
  void transactionPageRefundIdTest() {
    UUID originalTxId = UUID.randomUUID();
    Transaction originalTransaction = mock(Transaction.class);
    when(originalTransaction.getId()).thenReturn(originalTxId);
    RefundTransaction refundTransaction = mock(RefundTransaction.class);
    when(refundTransaction.getId()).thenReturn(transactionId);
    when(refundTransaction.getRefundOf()).thenReturn(originalTransaction);

    BigDecimal nonRefundedAmount = BigDecimal.ZERO;
    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(refundTransaction));
    when(transactionService.getNonRefundedAmount(transactionId)).thenReturn(nonRefundedAmount);

    String result =
        controller.showTransactionPage(model, transactionId.toString(), redirectAttributes);

    assertEquals("admin-transaction", result);
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "adminTransactions");
    verify(model).addAttribute(MODEL_ATTR_TX, refundTransaction);
    verify(model).addAttribute(MODEL_ATTR_REQUEST_ID, null);
    verify(model).addAttribute(MODEL_ATTR_REFUND_POSSIBLE, nonRefundedAmount);
    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService).getNonRefundedAmount(transactionId);
  }

  @Test
  void transactionPagePaymentTest() {
    UUID requestId = UUID.randomUUID();
    PaymentRequest paymentRequest = mock(PaymentRequest.class);
    when(paymentRequest.getRequest_id()).thenReturn(requestId);

    PaymentTransaction paymentTransaction = mock(PaymentTransaction.class);
    when(paymentTransaction.getId()).thenReturn(transactionId);
    when(paymentTransaction.getRequest()).thenReturn(paymentRequest);

    BigDecimal nonRefundedAmount = new BigDecimal("100.00");
    when(transactionService.getTransactionById(transactionId))
        .thenReturn(Optional.of(paymentTransaction));
    when(transactionService.getNonRefundedAmount(transactionId)).thenReturn(nonRefundedAmount);

    String result =
        controller.showTransactionPage(model, transactionId.toString(), redirectAttributes);

    assertEquals("admin-transaction", result);
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "adminTransactions");
    verify(model).addAttribute(MODEL_ATTR_TX, paymentTransaction);
    verify(model).addAttribute(MODEL_ATTR_REFUND_ID, null);
    verify(model).addAttribute(MODEL_ATTR_REFUND_POSSIBLE, nonRefundedAmount);
    verify(transactionService).getTransactionById(transactionId);
    verify(transactionService).getNonRefundedAmount(transactionId);
  }

  @Test
  void transactionPageNotFoundTest() {
    when(transactionService.getTransactionById(transactionId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> {
          controller.showTransactionPage(model, transactionId.toString(), redirectAttributes);
        },
        "Transaction not found");

    verify(transactionService).getTransactionById(transactionId);
    verify(model, never()).addAttribute(eq(MODEL_ATTR_TX), any());
    verify(model, never()).addAttribute(eq(MODEL_ATTR_REFUND_ID), any());
    verify(model, never()).addAttribute(eq(MODEL_ATTR_REQUEST_ID), any());
    verify(model, never()).addAttribute(eq(MODEL_ATTR_REFUND_POSSIBLE), any());
  }
}
