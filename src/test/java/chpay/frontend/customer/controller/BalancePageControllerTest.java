package chpay.frontend.customer.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.paymentbackend.service.BalanceService;
import chpay.paymentbackend.service.DepositService;
import chpay.paymentbackend.service.SettingService;
import chpay.paymentbackend.service.TransactionService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import javassist.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class BalancePageControllerTest {

  private static final String REDIRECT_URL = "redirect:/balance";
  private static final BigDecimal MAX_BALANCE_VALUE = new BigDecimal("270");

  @InjectMocks private BalancePageController balancePageController;

  @Mock private DepositService depositService;
  @Mock private TransactionService transactionsService;
  @Mock private NotificationService notificationService;
  @Mock private BalanceService balanceService;
  @Mock private TransactionRepository transactionRepository;
  @Mock private SettingService settingService;
  @Mock private RedirectAttributes redirectAttributes;
  @Mock private User mockUser;
  @Mock private Model model;

  @Test
  void testShowBalancePage() {
    String viewName = balancePageController.showBalancePage(model);
    assertEquals("balance", viewName, "The view name should be 'balance'");
  }

  @Test
  void testHandleTopupReturnValue() {
    String topupAmount = "10";
    when(mockUser.getBalance()).thenReturn(new BigDecimal("50"));
    when(settingService.getMaxBalance()).thenReturn(MAX_BALANCE_VALUE);
    String result = balancePageController.handleTopup(topupAmount, mockUser, redirectAttributes);
    assertEquals(REDIRECT_URL, result, "Should redirect to balance after topup");
  }

  @Test
  void testHandleTopupWithValidAmount() {
    User currentUser = new User("Stoyan", "skutsarov@tudelft.nl", "skutsarov");
    currentUser.addBalance(BigDecimal.TEN);
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal(100000));
    BigDecimal topupAmount = new BigDecimal(20);
    TopupTransaction transaction =
        TopupTransaction.createTopUpTransaction(currentUser, topupAmount, "Mollie Deposit");
    String mollieUrl = "https://example.com/mollie-payment";

    when(depositService.getMollieUrl(any(TopupTransaction.class))).thenReturn(mollieUrl);
    when(transactionsService.save(any(Transaction.class))).thenReturn(transaction);

    String result =
        balancePageController.handleTopup(topupAmount.toString(), currentUser, redirectAttributes);

    assertEquals("redirect:" + mollieUrl, result);
  }

  @Test
  void testHandleTopupWithInvalidInput() {
    User currentUser = new User("Stoyan", "skutsarov@tudelft.nl", "skutsarov");
    currentUser.addBalance(BigDecimal.TEN);
    ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);

    String result = balancePageController.handleTopup("invalid", currentUser, redirectAttributes);

    assertEquals("redirect:/balance", result);
    verify(notificationService, times(1))
        .addErrorMessage(eq(redirectAttributes), errorMessageCaptor.capture());
    assertEquals("Invalid top-up amount.", errorMessageCaptor.getValue());
  }

  @Test
  void testHandleTopupWithNegativeAmount() {
    User currentUser = new User("Stoyan", "skutsarov@tudelft.nl", "skutsarov");
    currentUser.addBalance(BigDecimal.TEN);
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal(100));
    BigDecimal negativeAmount = new BigDecimal("-5");
    ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);

    String result =
        balancePageController.handleTopup(
            negativeAmount.toString(), currentUser, redirectAttributes);

    assertEquals("redirect:/balance", result);
    verify(notificationService, times(1))
        .addErrorMessage(eq(redirectAttributes), errorMessageCaptor.capture());
    assertEquals("Top-up amount must be positive.", errorMessageCaptor.getValue());
  }

  @Test
  void testHandleTopupWithExceedingAmount() {
    User currentUser = new User("Stoyan", "skutsarov@tudelft.nl", "skutsarov");
    currentUser.addBalance(new BigDecimal(90));
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal(100));
    BigDecimal exceedingAmount = new BigDecimal(15);
    ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);

    String result =
        balancePageController.handleTopup(
            exceedingAmount.toString(), currentUser, redirectAttributes);

    assertEquals("redirect:/balance", result);
    verify(notificationService, times(1))
        .addErrorMessage(eq(redirectAttributes), errorMessageCaptor.capture());
    assertEquals(
        "Top-up amount must be less than " + settingService.getMaxBalance(),
        errorMessageCaptor.getValue());
  }

  @Test
  void testHandleTopupWithNegativeAmountRedirect() {
    String topupAmount = "-10";

    String result = balancePageController.handleTopup(topupAmount, mockUser, redirectAttributes);
    assertEquals(REDIRECT_URL, result, "Should redirect to balance after negative amount");
  }

  @Test
  void testHandleTopupWithNegativeAmountMessage() {
    String topupAmount = "-10";

    balancePageController.handleTopup(topupAmount, mockUser, redirectAttributes);

    verify(notificationService)
        .addErrorMessage(redirectAttributes, "Top-up amount must be positive.");
  }

  @Test
  void testHandleTopupWithExceedingAmountRedirect() {
    String topupAmount = "250";
    when(mockUser.getBalance()).thenReturn(new BigDecimal("50"));
    when(settingService.getMaxBalance()).thenReturn(MAX_BALANCE_VALUE);

    String result = balancePageController.handleTopup(topupAmount, mockUser, redirectAttributes);
    assertEquals(REDIRECT_URL, result, "Should redirect to balance after exceeding amount");
  }

  @Test
  void testHandleTopupWithExceedingAmountMessage() {
    String topupAmount = "250";
    when(mockUser.getBalance()).thenReturn(new BigDecimal("50"));
    when(settingService.getMaxBalance()).thenReturn(MAX_BALANCE_VALUE);

    balancePageController.handleTopup(topupAmount, mockUser, redirectAttributes);

    // Verify error notification was sent
    verify(notificationService)
        .addErrorMessage(
            redirectAttributes, "Top-up amount must be less than " + MAX_BALANCE_VALUE);
  }

  @Test
  void testHandleTopupWithInvalidInputRedirect() {
    String topupAmount = "abc";
    String result = balancePageController.handleTopup(topupAmount, mockUser, redirectAttributes);
    assertEquals(REDIRECT_URL, result, "Should redirect to balance after invalid input");
  }

  @Test
  void testHandleTopupWithInvalidInputMessage() {
    String topupAmount = "abc";

    // This will throw NumberFormatException which should be caught
    // and an error notification should be sent
    balancePageController.handleTopup(topupAmount, mockUser, redirectAttributes);

    // We can't verify the exact error message as it depends on the exception message
    // But we can verify that an error notification was attempted
    verify(notificationService).addErrorMessage(eq(redirectAttributes), anyString());
  }

  @Test
  void showBalancePage_includesMinTopUpFromSettings() {
    BigDecimal expectedMin = new BigDecimal("7.25");
    when(settingService.getMinTopUp()).thenReturn(expectedMin);

    when(settingService.getMaxBalance()).thenReturn(new BigDecimal("150.00"));

    Model model = new ExtendedModelMap();

    String viewName = balancePageController.showBalancePage(model);

    assertThat(viewName).isEqualTo("balance");
    Object actualMin = model.getAttribute("minTopUp");
    assertThat(actualMin).isInstanceOf(BigDecimal.class);
    assertThat((BigDecimal) actualMin).isEqualByComparingTo(expectedMin);
  }

  @Test
  void testNullUrlError() {
    User currentUser = new User("Stoyan", "skutsarov@tudelft.nl", "skutsarov");
    currentUser.addBalance(BigDecimal.ZERO);
    BigDecimal topupAmount = new BigDecimal(10);
    TopupTransaction transaction =
        TopupTransaction.createTopUpTransaction(currentUser, topupAmount, "Mollie Deposit");

    when(settingService.getMaxBalance()).thenReturn(new BigDecimal(1000));
    when(transactionRepository.save(any(TopupTransaction.class))).thenReturn(transaction);
    when(depositService.getMollieUrl(any(TopupTransaction.class))).thenReturn(null);

    String result =
        balancePageController.handleTopup(topupAmount.toString(), currentUser, redirectAttributes);

    assertEquals(REDIRECT_URL, result);
    verify(notificationService)
        .addErrorMessage(
            eq(redirectAttributes), eq("Could not create the payment link. Please try again."));
    verify(transactionRepository).save(any(TopupTransaction.class));
    verify(depositService).getMollieUrl(any(TopupTransaction.class));
    verify(transactionsService, never()).save(any(Transaction.class));
  }

  @Test
  void testTopUpException() {
    User currentUser = new User("Stoyan", "skutsarov@tudelft.nl", "skutsarov");
    currentUser.addBalance(BigDecimal.ZERO);
    String topupAmount = "10";

    when(settingService.getMaxBalance())
        .thenThrow(new RuntimeException("Simulated unexpected error"));

    String result = balancePageController.handleTopup(topupAmount, currentUser, redirectAttributes);

    assertEquals(REDIRECT_URL, result);
    verify(notificationService)
        .addErrorMessage(
            eq(redirectAttributes),
            contains("An unexpected error occurred: Simulated unexpected error"));
    verify(depositService, never()).getMollieUrl(any(TopupTransaction.class));
    verify(transactionsService, never()).save(any(Transaction.class));
    verify(transactionRepository, never()).save(any(TopupTransaction.class));
  }

  @Test
  void testDepositPending() throws NotFoundException {
    UUID transactionUuid = UUID.randomUUID();
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.PENDING);
    when(transactionRepository.findById(transactionUuid)).thenReturn(Optional.of(transaction));

    String result =
        balancePageController.depositSuccess(transactionUuid.toString(), redirectAttributes, model);

    assertEquals("pending", result);
    verify(transactionRepository).findById(transactionUuid);
  }

  @Test
  void successTest() throws NotFoundException {
    UUID transactionUuid = UUID.randomUUID();
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.SUCCESSFUL);
    when(transactionRepository.findById(transactionUuid)).thenReturn(Optional.of(transaction));

    String result =
        balancePageController.depositSuccess(transactionUuid.toString(), redirectAttributes, model);

    assertEquals("successful", result);
    verify(transactionRepository).findById(transactionUuid);
  }

  @Test
  void failTest() throws NotFoundException {
    UUID transactionUuid = UUID.randomUUID();
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.FAILED);
    when(transactionRepository.findById(transactionUuid)).thenReturn(Optional.of(transaction));

    String result =
        balancePageController.depositSuccess(transactionUuid.toString(), redirectAttributes, model);

    assertEquals("failed", result);
    verify(transactionRepository).findById(transactionUuid);
  }

  @Test
  void errorTest() throws NotFoundException {
    UUID transactionUuid = UUID.randomUUID();
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.REFUNDED);
    when(transactionRepository.findById(transactionUuid)).thenReturn(Optional.of(transaction));

    String result =
        balancePageController.depositSuccess(transactionUuid.toString(), redirectAttributes, model);

    assertEquals("error", result);
    verify(transactionRepository).findById(transactionUuid);
  }

  @Test
  void depositSuccess_transactionNotFound_throwsNotFoundException() {
    UUID transactionUuid = UUID.randomUUID();
    when(transactionRepository.findById(transactionUuid)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> {
          balancePageController.depositSuccess(
              transactionUuid.toString(), redirectAttributes, model);
        },
        transactionUuid.toString());

    verify(transactionRepository).findById(transactionUuid);
  }
}
