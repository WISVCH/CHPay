package chpay.frontend.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.frontend.events.CHPaymentRequest;
import chpay.frontend.events.CHPaymentResponse;
import chpay.frontend.events.controller.ExternalPaymentController;
import chpay.frontend.events.service.ExternalPaymentServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class ExternalPaymentControllerTest {

  @Mock private ExternalPaymentServiceImpl externalPaymentService;
  @Mock private TransactionRepository transactionRepository;

  @InjectMocks private ExternalPaymentController externalPaymentController;

  private CHPaymentRequest testRequest;
  private CHPaymentResponse testResponse;

  @BeforeEach
  void setUp() {
    testRequest =
        new CHPaymentRequest(
            new BigDecimal("10.00"),
            "Test Description",
            "Consumer Name",
            "consumer@example.com",
            "http://redirect.url",
            "http://webhook.url",
            "http://fallback.url",
            null);

    testResponse = new CHPaymentResponse("test_tx_id", "http://checkout.url");
  }

  @Test
  void createExternalPaymentTest() {
    when(externalPaymentService.createTransaction(testRequest)).thenReturn(testResponse);

    ResponseEntity<CHPaymentResponse> responseEntity =
        externalPaymentController.createExternalPayment(testRequest);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(testResponse, responseEntity.getBody());

    verify(externalPaymentService, times(1)).createTransaction(testRequest);
  }


  @Test
  void getExternalPaymentStatusSuccessTest() {
    UUID paymentId = UUID.randomUUID();
    Transaction transaction = mock(Transaction.class);
    Mockito.lenient().when(transaction.getId()).thenReturn(paymentId);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.SUCCESSFUL);
    when(transactionRepository.findById(paymentId)).thenReturn(Optional.of(transaction));

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(Transaction.TransactionStatus.SUCCESSFUL, responseEntity.getBody());
    verify(transactionRepository, times(1)).findById(paymentId);
  }

  @Test
  void getExternalPaymentStatusPendingTest() {
    UUID paymentId = UUID.randomUUID();
    Transaction transaction = mock(Transaction.class);
    Mockito.lenient().when(transaction.getId()).thenReturn(paymentId);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.PENDING);

    when(transactionRepository.findById(paymentId)).thenReturn(Optional.of(transaction));

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(Transaction.TransactionStatus.PENDING, responseEntity.getBody());
    verify(transactionRepository, times(1)).findById(paymentId);
  }

  @Test
  void getExternalPaymentStatusFailedTest() {
    UUID paymentId = UUID.randomUUID();
    Transaction transaction = mock(Transaction.class);
    Mockito.lenient().when(transaction.getId()).thenReturn(paymentId);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.FAILED);

    when(transactionRepository.findById(paymentId)).thenReturn(Optional.of(transaction));

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(Transaction.TransactionStatus.FAILED, responseEntity.getBody());
    verify(transactionRepository, times(1)).findById(paymentId);
  }

  @Test
  void getExternalPaymentStatusNotFoundTest() {
    UUID paymentId = UUID.randomUUID();
    when(transactionRepository.findById(paymentId)).thenReturn(Optional.empty());

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    verify(transactionRepository, times(1)).findById(paymentId);
  }
}
