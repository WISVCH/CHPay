package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RfidPaymentServiceTest {

  @Mock RequestService requestService;
  @Mock TransactionService txnService;

  @InjectMocks RfidPaymentService paymentService;

  private User user;
  private UUID requestId;
  private PaymentRequest pr;

  @BeforeEach
  void setUp() {
    user = new User("Alice", "alice@example.com", "alice-openid", new BigDecimal("100.00"));
    requestId = UUID.randomUUID();
    pr = new PaymentRequest(new BigDecimal("25.00"), "Coffee", false);
  }

  /** Tests bad weather behavior where there is no such payment request. */
  @Test
  void throwsNoSuchElementException() {
    when(requestService.getRequestById(requestId)).thenReturn(Optional.empty());

    NoSuchElementException ex =
        assertThrows(
            NoSuchElementException.class, () -> paymentService.payFromRequest(user, requestId));
    assertEquals("No payment request " + requestId, ex.getMessage());

    verify(requestService).getRequestById(requestId);
    verifyNoInteractions(txnService);
  }

  /** Throws bad weather scenario where the user has insufficient balance. */
  @Test
  void throwsInsufficientBalanceException() {
    // Make the request bigger than the user's balance
    PaymentRequest big = new PaymentRequest(new BigDecimal("150.00"), "Expensive item", false);

    when(requestService.getRequestById(requestId)).thenReturn(Optional.of(big));

    InsufficientBalanceException ex =
        assertThrows(
            InsufficientBalanceException.class,
            () -> paymentService.payFromRequest(user, requestId));
    // Check the exception message
    String msg = ex.getMessage();
    assertEquals("Insufficient balance: need 150.00 but have 100.00", msg);

    verify(requestService).getRequestById(requestId);
    verifyNoInteractions(txnService);
  }

  /** Tests good weather behavior. */
  @Test
  void goodWeatherScenario() {
    when(requestService.getRequestById(requestId)).thenReturn(Optional.of(pr));

    PaymentTransaction pending = new PaymentTransaction();
    when(requestService.transactionFromRequest(requestId, user)).thenReturn(pending);

    String result = paymentService.payFromRequest(user, requestId);

    // Should return the user’s name
    assertEquals("Alice", result);

    verify(requestService).getRequestById(requestId);
    verify(requestService).transactionFromRequest(requestId, user);
    verify(txnService).fullfillTransaction(eq(pending.getId()), eq(user));
  }

  /** Tests bad weather behavior where an exception is thrown */
  @Test
  void fulfillThrowsRuntimeException() {
    when(requestService.getRequestById(requestId)).thenReturn(Optional.of(pr));

    PaymentTransaction pending = new PaymentTransaction();
    when(requestService.transactionFromRequest(requestId, user)).thenReturn(pending);

    // Simulate a downstream error
    doThrow(new RuntimeException("DB down"))
        .when(txnService)
        .fullfillTransaction(pending.getId(), user);

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> paymentService.payFromRequest(user, requestId));
    assertEquals("DB down", ex.getMessage());

    verify(requestService).getRequestById(requestId);
    verify(requestService).transactionFromRequest(requestId, user);
    verify(txnService).fullfillTransaction(pending.getId(), user);
  }
}
