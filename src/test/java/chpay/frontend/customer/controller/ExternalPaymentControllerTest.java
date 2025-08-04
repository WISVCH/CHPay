package chpay.frontend.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.frontend.events.CHPaymentRequest;
import chpay.frontend.events.CHPaymentResponse;
import chpay.frontend.events.controller.ExternalPaymentController;
import chpay.frontend.events.service.ExternalPaymentServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
public class ExternalPaymentControllerTest {

  @Mock private ExternalPaymentServiceImpl externalPaymentService;
  @Mock private UserRepository userRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private SecurityContext securityContext;
  @Mock private Authentication authentication;
  @Mock private Jwt jwt;
  @Mock private HttpServletResponse httpServletResponse;

  @InjectMocks private ExternalPaymentController externalPaymentController;

  private User testUser;
  private CHPaymentRequest testRequest;
  private CHPaymentResponse testResponse;
  private String testOpenId;

  @BeforeEach
  void setUp() {
    testOpenId = "test-user-sub";
    testUser = mock(User.class);
    Mockito.lenient().when(testUser.getId()).thenReturn(UUID.randomUUID());
    Mockito.lenient().when(testUser.getOpenID()).thenReturn(testOpenId);
    Mockito.lenient().when(testUser.getName()).thenReturn("testuser");
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

    SecurityContextHolder.setContext(securityContext);
    Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    Mockito.lenient().when(authentication.getPrincipal()).thenReturn(jwt);
    Mockito.lenient().when(jwt.getClaimAsString("sub")).thenReturn(testOpenId);
  }

  @Test
  void externalPaymentTest() {
    when(userRepository.findByOpenID(testOpenId)).thenReturn(Optional.of(testUser));
    when(externalPaymentService.createTransaction(testRequest, testUser)).thenReturn(testResponse);

    ResponseEntity<CHPaymentResponse> responseEntity =
        externalPaymentController.createExternalPayment(testRequest);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(testResponse, responseEntity.getBody());

    verify(userRepository, times(1)).findByOpenID(testOpenId);
    verify(externalPaymentService, times(1)).createTransaction(testRequest, testUser);
  }

  @Test
  void externalPaymentExceptionTest() {
    when(userRepository.findByOpenID(testOpenId)).thenReturn(Optional.empty());

    NoSuchElementException thrown =
        assertThrows(
            NoSuchElementException.class,
            () -> {
              externalPaymentController.createExternalPayment(testRequest);
            });

    assertEquals("User not found", thrown.getMessage());
    verify(userRepository, times(1)).findByOpenID(testOpenId);
    verify(externalPaymentService, never()).createTransaction(any(), any());
  }

  @Test
  void externalPaymentSuccessTest() {
    UUID paymentId = UUID.randomUUID();
    Transaction transaction = mock(Transaction.class);
    Mockito.lenient().when(transaction.getId()).thenReturn(paymentId);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.SUCCESSFUL);
    when(transactionRepository.findById(paymentId)).thenReturn(Optional.of(transaction));

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId, httpServletResponse);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(Transaction.TransactionStatus.SUCCESSFUL, responseEntity.getBody());
    verify(transactionRepository, times(1)).findById(paymentId);
    verify(httpServletResponse, never()).setStatus(anyInt());
  }

  @Test
  void externalPaymentPending2() {
    UUID paymentId = UUID.randomUUID();
    Transaction transaction = mock(Transaction.class);
    Mockito.lenient().when(transaction.getId()).thenReturn(paymentId);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.PENDING);

    when(transactionRepository.findById(paymentId)).thenReturn(Optional.of(transaction));

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId, httpServletResponse);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(Transaction.TransactionStatus.PENDING, responseEntity.getBody());
    verify(transactionRepository, times(1)).findById(paymentId);
    verify(httpServletResponse, never()).setStatus(anyInt());
  }

  @Test
  void externalPaymentFailedTest() {
    UUID paymentId = UUID.randomUUID();
    Transaction transaction = mock(Transaction.class);
    Mockito.lenient().when(transaction.getId()).thenReturn(paymentId);
    when(transaction.getStatus()).thenReturn(Transaction.TransactionStatus.FAILED);

    when(transactionRepository.findById(paymentId)).thenReturn(Optional.of(transaction));

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId, httpServletResponse);

    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(Transaction.TransactionStatus.FAILED, responseEntity.getBody());
    verify(transactionRepository, times(1)).findById(paymentId);
    verify(httpServletResponse, never()).setStatus(anyInt());
  }

  @Test
  void externalPaymentNotFoundTest() {
    UUID paymentId = UUID.randomUUID();
    when(transactionRepository.findById(paymentId)).thenReturn(Optional.empty());

    ResponseEntity<Transaction.TransactionStatus> responseEntity =
        externalPaymentController.getExternalPaymentStatus(paymentId, httpServletResponse);

    assertNull(responseEntity); // Controller returns null as per current implementation
    verify(transactionRepository, times(1)).findById(paymentId);
    verify(httpServletResponse, times(1)).setStatus(HttpStatus.NOT_FOUND.value());
  }
}
