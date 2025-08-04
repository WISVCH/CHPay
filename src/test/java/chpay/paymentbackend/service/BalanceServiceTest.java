package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.Exceptions.IllegalRefundException;
import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import chpay.DatabaseHandler.transactiondb.entities.*;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.RefundTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import jakarta.persistence.LockTimeoutException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private TransactionRepository transactionRepository;

  @Mock private SettingService settingService;

  @InjectMocks private BalanceService balanceService;

  private User testUser;
  private PaymentTransaction testTransaction;

  @BeforeEach
  void setUp() {
    testUser = new User("Test User", "test@example.com", "test123", new BigDecimal("100.00"));
    testTransaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-10.00"), "Test payment", null);
  }

  @Test
  void pay_sufficientBalance_multiUseRequest_marksSuccessful() {
    BigDecimal amount = new BigDecimal("-50.00");
    PaymentRequest request = mock(PaymentRequest.class);

    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(testUser, amount, "Test Payment", request);

    when(userRepository.findByIdForUpdate(any())).thenReturn(testUser);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Transaction result = balanceService.pay(testUser, transaction);

    assertAll(
        () -> assertEquals(Transaction.TransactionStatus.SUCCESSFUL, result.getStatus()),
        () -> assertEquals(testUser, result.getUser()),
        () -> assertEquals(amount, result.getAmount()));

    verify(userRepository).findByIdForUpdate(any());
    verify(userRepository).saveAndFlush(testUser);
    verify(transactionRepository).save(transaction);
  }

  @Test
  void pay_sufficientBalance_singleUseRequest_marksSuccessfulAndFulfilled() {
    BigDecimal amount = new BigDecimal("-50.00");
    PaymentRequest request = mock(PaymentRequest.class);

    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(testUser, amount, "Test Payment", request);

    when(userRepository.findByIdForUpdate(any())).thenReturn(testUser);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Transaction result = balanceService.pay(testUser, transaction);

    assertEquals(Transaction.TransactionStatus.SUCCESSFUL, result.getStatus());
    assertEquals(testUser, result.getUser());
    assertEquals(amount, result.getAmount());

    verify(userRepository).findByIdForUpdate(any());
    verify(userRepository).saveAndFlush(testUser);
    verify(transactionRepository).save(transaction);
  }

  @Test
  void pay_insufficientBalance_throwsException() {
    BigDecimal amount = new BigDecimal("-150.00");
    PaymentRequest request = mock(PaymentRequest.class);

    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(testUser, amount, "Test Payment", request);

    when(userRepository.findByIdForUpdate(any())).thenReturn(testUser);

    assertThrows(
        InsufficientBalanceException.class, () -> balanceService.pay(testUser, transaction));

    verify(userRepository).findByIdForUpdate(any());
    verify(userRepository, never()).saveAndFlush(any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void refund_validTransaction_createsRefundTransaction() {
    BigDecimal originalPaymentAmount = new BigDecimal("-50.00");
    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(
            testUser, originalPaymentAmount, "Original payment", null);

    UUID originalId = UUID.randomUUID();
    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(original, originalId);
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }

    when(userRepository.findByIdForUpdate(any())).thenReturn(testUser);
    lenient()
        .when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    try (MockedStatic<RefundTransaction> mockedStatic =
        Mockito.mockStatic(RefundTransaction.class)) {

      String refundDescription = "Refund of transaction with ID: " + originalId;

      RefundTransaction manuallyCreatedRefundInstance =
          new RefundTransaction(testUser, originalPaymentAmount.abs(), refundDescription);

      mockedStatic
          .when(
              () ->
                  RefundTransaction.createRefund(
                      any(User.class), any(BigDecimal.class), any(Transaction.class)))
          .thenReturn(manuallyCreatedRefundInstance);

      RefundTransaction mockRefund =
          RefundTransaction.createRefund(testUser, originalPaymentAmount.abs(), original);
      try {
        Field refundOfField = RefundTransaction.class.getDeclaredField("refundOf");
        refundOfField.setAccessible(true);
        refundOfField.set(mockRefund, original);
      } catch (Exception e) {
        fail("Failed to set refundOf field via reflection: " + e.getMessage());
      }
      RefundTransaction result = balanceService.refund(testUser, originalPaymentAmount, original);

      assertEquals(Transaction.TransactionType.REFUND, result.getType());
      assertEquals(Transaction.TransactionStatus.SUCCESSFUL, result.getStatus());
      assertEquals(originalPaymentAmount.abs(), result.getAmount());
      assertEquals(refundDescription, result.getDescription());
      assertEquals(testUser, result.getUser());
      assertEquals(original, result.getRefundOf());

      verify(userRepository).findByIdForUpdate(any());
      verify(settingService).assertBalanceWithinLimit(any(), any());
      verify(userRepository).saveAndFlush(testUser);
      verify(transactionRepository).save(any(Transaction.class));
    }
  }

  @Test
  void refund_positiveAmount_throwsException() {
    BigDecimal amount = new BigDecimal("50.00");
    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Original payment", null);

    when(userRepository.findByIdForUpdate(any())).thenReturn(testUser);

    assertThrows(
        IllegalRefundException.class, () -> balanceService.refund(testUser, amount, original));

    verify(userRepository).findByIdForUpdate(any());
    verify(userRepository, never()).saveAndFlush(any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void markTopUpAsPaid_pendingTransaction_marksSuccessfulAndUpdatesBalance() {
    BigDecimal amount = new BigDecimal("50.00");
    TopupTransaction pendingTopUp =
        TopupTransaction.createTopUpTransaction(testUser, amount, "mollie-123");
    BigDecimal initialBalance = testUser.getBalance();

    when(userRepository.findByIdForUpdate(any())).thenReturn(testUser);

    balanceService.markTopUpAsPaid(pendingTopUp);

    assertEquals(Transaction.TransactionStatus.SUCCESSFUL, pendingTopUp.getStatus());
    assertEquals(initialBalance.add(amount), testUser.getBalance());

    verify(userRepository).findByIdForUpdate(any());
    verify(settingService).assertBalanceWithinLimit(any(), any());
    verify(userRepository).save(testUser);
    verify(transactionRepository).save(pendingTopUp);
  }

  @Test
  void markTopUpAsFailed_pendingTransaction_marksFailedWithoutUpdatingBalance() {
    BigDecimal amount = new BigDecimal("50.00");
    TopupTransaction pendingTopUp =
        TopupTransaction.createTopUpTransaction(testUser, amount, "mollie-123");
    BigDecimal initialBalance = testUser.getBalance();

    balanceService.markTopUpAsFailed(pendingTopUp);

    assertEquals(Transaction.TransactionStatus.FAILED, pendingTopUp.getStatus());
    assertEquals(initialBalance, testUser.getBalance());

    verify(userRepository, never()).findByIdForUpdate(any());
    verify(settingService, never()).assertBalanceWithinLimit(any(), any());
    verify(userRepository, never()).save(any());
    verify(transactionRepository).save(pendingTopUp);
  }

  @Test
  void recoverRefundFromLockTimeout_shouldLogAndThrowIllegalStateException() {
    LockTimeoutException exception = new LockTimeoutException("Lock timeout");
    BigDecimal amount = new BigDecimal("-25.00");

    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(testUser, amount, "Test", null);
    UUID txId = UUID.randomUUID();
    setField(original, "id", txId);

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                balanceService.recoverRefundFromLockTimeout(exception, testUser, amount, original));

    assertEquals("Lock timeout while trying to refund transaction", thrown.getMessage());
  }

  @Test
  void recoverRefundFromPessimisticLock_shouldThrowIllegalStateException() {
    User user = new User("Test User", "test@example.com", "test123", new BigDecimal("50.00"));
    BigDecimal amount = new BigDecimal("-20.00");
    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(user, amount, "desc", null);

    PessimisticEntityLockException ex = mock(PessimisticEntityLockException.class);
    when(ex.getMessage()).thenReturn("Simulated lock failure");

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () -> balanceService.recoverRefundFromPessimisticLock(ex, user, amount, original));

    assertEquals(
        "Pessimistic lock exception while trying to refund transaction", thrown.getMessage());
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field: " + fieldName, e);
    }
  }
}
