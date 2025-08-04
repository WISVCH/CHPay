package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.RequestRepository;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

  @Mock private BalanceService balanceService;

  @Mock private RequestRepository requestRepository;

  @Mock private TransactionRepository transactionRepository;

  @InjectMocks private RequestService requestService;

  private User testUser;
  private PaymentRequest testRequest;
  private PaymentTransaction testTransaction;
  private UUID requestId;

  @BeforeEach
  void setUp() {
    testUser = new User("Test User", "test@example.com", "test123", new BigDecimal("100.00"));
    requestId = UUID.randomUUID();
    testRequest = new PaymentRequest(new BigDecimal("50.00"), "Test request", false);
    try {
      java.lang.reflect.Field idField = PaymentRequest.class.getDeclaredField("request_id");
      idField.setAccessible(true);
      idField.set(testRequest, requestId);
    } catch (Exception e) {
      fail("Failed to set request ID: " + e.getMessage());
    }

    testTransaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment", null);
  }

  @Test
  void createRequest_validParameters_createsRequest() {
    BigDecimal amount = new BigDecimal("50.00");
    String description = "Test request";
    boolean multiuse = false;

    when(requestRepository.save(any(PaymentRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PaymentRequest result = requestService.createRequest(amount, description, multiuse);

    assertEquals(amount, result.getAmount());
    assertEquals(description, result.getDescription());
    assertEquals(multiuse, result.isMultiUse());
    assertFalse(result.isFulfilled());

    verify(requestRepository).save(any(PaymentRequest.class));
  }

  @Test
  void transactionFromRequest_singleUseRequest_createsTransaction() {
    when(requestRepository.findByIdForUpdate(requestId)).thenReturn(testRequest);
    when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

    PaymentTransaction result = requestService.transactionFromRequest(requestId, testUser);

    assertEquals(testTransaction, result);

    verify(requestRepository).findByIdForUpdate(requestId);
    verify(transactionRepository).save(any(Transaction.class));
    verify(requestRepository).save(testRequest);
  }

  @Test
  void transactionFromRequest_multiUseRequest_createsTransaction() {
    testRequest = new PaymentRequest(new BigDecimal("50.00"), "Test request", true);
    try {
      java.lang.reflect.Field idField = PaymentRequest.class.getDeclaredField("request_id");
      idField.setAccessible(true);
      idField.set(testRequest, requestId);
    } catch (Exception e) {
      fail("Failed to set request ID: " + e.getMessage());
    }

    when(requestRepository.findByIdForUpdate(requestId)).thenReturn(testRequest);
    when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

    PaymentTransaction result = requestService.transactionFromRequest(requestId, testUser);

    assertEquals(testTransaction, result);
    assertFalse(testRequest.isFulfilled());

    verify(requestRepository).findByIdForUpdate(requestId);
    verify(transactionRepository).save(any(Transaction.class));
    verify(requestRepository).save(testRequest);
  }

  @Test
  void transactionFromRequest_alreadyFulfilledSingleUseRequest_throwsException() {
    testRequest.setFulfilled(true);

    when(requestRepository.findByIdForUpdate(requestId)).thenReturn(testRequest);

    assertThrows(
        IllegalStateException.class,
        () -> requestService.transactionFromRequest(requestId, testUser));

    verify(requestRepository).findByIdForUpdate(requestId);
    verify(transactionRepository, never()).save(any());
    verify(requestRepository, never()).save(any());
  }

  @Test
  void transactionFromRequest_alreadyFulfilledMultiUseRequest_allowsTransaction() {
    testRequest = new PaymentRequest(new BigDecimal("50.00"), "Test request", true);
    testRequest.setFulfilled(true); // This should be ignored for multi-use requests

    try {
      java.lang.reflect.Field idField = PaymentRequest.class.getDeclaredField("request_id");
      idField.setAccessible(true);
      idField.set(testRequest, requestId);
    } catch (Exception e) {
      fail("Failed to set request ID: " + e.getMessage());
    }

    when(requestRepository.findByIdForUpdate(requestId)).thenReturn(testRequest);
    when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

    PaymentTransaction result = requestService.transactionFromRequest(requestId, testUser);

    assertEquals(testTransaction, result);

    verify(requestRepository).findByIdForUpdate(requestId);
    verify(transactionRepository).save(any(Transaction.class));
    verify(requestRepository).save(testRequest);
  }
}
