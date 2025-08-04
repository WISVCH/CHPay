package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.woutschoovaerts.mollie.Client;
import be.woutschoovaerts.mollie.data.common.Link;
import be.woutschoovaerts.mollie.data.payment.PaymentLinks;
import be.woutschoovaerts.mollie.data.payment.PaymentRequest;
import be.woutschoovaerts.mollie.data.payment.PaymentResponse;
import be.woutschoovaerts.mollie.data.payment.PaymentStatus;
import be.woutschoovaerts.mollie.exception.MollieException;
import be.woutschoovaerts.mollie.handler.payments.PaymentHandler;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

  @Mock private BalanceService balanceService;
  @Mock private UserRepository userRepository;
  @Mock private Client mollieClient;
  @Mock private TransactionRepository transactionRepository;
  @Mock private MailService mailService;
  @InjectMocks private DepositService depositService;

  @Value("${spring.application.base-url}")
  private String baseUrl;

  private final String TEST_REDIRECT_URL = baseUrl + "/payment/complete/";
  private final String TEST_WEBHOOK_URL = baseUrl + "/balance/status";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(depositService, "redirectUrl", TEST_REDIRECT_URL);
    ReflectionTestUtils.setField(depositService, "webhookUrl", TEST_WEBHOOK_URL);
    ReflectionTestUtils.setField(depositService, "fee", "0.32");
  }

  @Test
  public void testGetMollieUrl() throws Exception {
    TopupTransaction t = mock();
    when(t.getId()).thenReturn(UUID.randomUUID());
    User u = createUser();
    when(t.getUser()).thenReturn(u);
    when(t.getAmount()).thenReturn(BigDecimal.ONE);
    when(userRepository.findByOpenID(anyString())).thenReturn(Optional.ofNullable(u));
    PaymentResponse paymentResponse = new PaymentResponse();
    Link link = Link.builder().build();
    paymentResponse.setLinks(PaymentLinks.builder().checkout(link).build());
    paymentResponse.setId("idid");
    PaymentHandler handler = mock(PaymentHandler.class);
    when(mollieClient.payments()).thenReturn(handler);
    when(handler.createPayment(Mockito.any())).thenReturn(paymentResponse);

    assertEquals(link.getHref(), depositService.getMollieUrl(t));
  }

  @Test
  public void testCreateMolliePaymentRequestFromOrder() {
    TopupTransaction t = mock();
    User u = createUser();
    when(t.getUser()).thenReturn(u);
    when(t.getId()).thenReturn(UUID.randomUUID());
    when(userRepository.findByOpenID(anyString())).thenReturn(Optional.ofNullable(u));
    when(t.getAmount()).thenReturn(BigDecimal.ONE);
    PaymentRequest paymentRequest = depositService.createPayment(t);
    assertEquals(paymentRequest.getDescription(), ("W.I.S.V. 'Christiaan Huygens'"));
    assertEquals(paymentRequest.getAmount().getValue().toString(), "1.32");
  }

  @Test
  public void testValidateTransaction_PendingStatus() throws Exception {
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transactionRepository.findByIdForUpdateTopup(Mockito.any())).thenReturn(transaction);
    when(transaction.getMollieId()).thenReturn("mocked-id");

    PaymentResponse paymentResponse = new PaymentResponse();
    paymentResponse.setId("mocked-id");
    PaymentHandler handler = mock(PaymentHandler.class);
    when(mollieClient.payments()).thenReturn(handler);
    paymentResponse.setStatus(PaymentStatus.PENDING);

    when(mollieClient.payments().getPayment("mocked-id")).thenReturn(paymentResponse);
    UUID id = UUID.randomUUID();
    when(transactionRepository.findByIdForUpdateTopup(id)).thenReturn(transaction);
    when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);
    TopupTransaction updatedTransaction = depositService.validateTransaction(id);

    Mockito.verify(transaction).setStatus(Transaction.TransactionStatus.PENDING);
    Mockito.verify(transactionRepository).saveAndFlush(transaction);
    assertEquals(transaction, updatedTransaction);
  }

  @Test
  public void testValidateTransaction_PaidStatus() throws Exception {
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transactionRepository.findByIdForUpdateTopup(Mockito.any())).thenReturn(transaction);
    when(transaction.getMollieId()).thenReturn("mocked-id");

    PaymentResponse paymentResponse = new PaymentResponse();
    paymentResponse.setId("mocked-id");
    PaymentHandler handler = mock(PaymentHandler.class);
    when(mollieClient.payments()).thenReturn(handler);
    paymentResponse.setStatus(PaymentStatus.PAID);

    when(mollieClient.payments().getPayment("mocked-id")).thenReturn(paymentResponse);
    UUID id = UUID.randomUUID();
    when(transactionRepository.findByIdForUpdateTopup(id)).thenReturn(transaction);
    when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);
    TopupTransaction updatedTransaction = depositService.validateTransaction(id);

    Mockito.verify(transaction).setStatus(Transaction.TransactionStatus.SUCCESSFUL);
    Mockito.verify(transactionRepository).saveAndFlush(transaction);
    assertEquals(transaction, updatedTransaction);
  }

  @Test
  public void testValidateTransaction_ExpiredStatus() throws Exception {
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transactionRepository.findByIdForUpdateTopup(Mockito.any())).thenReturn(transaction);
    when(transaction.getMollieId()).thenReturn("mocked-id");

    PaymentResponse paymentResponse = new PaymentResponse();
    paymentResponse.setId("mocked-id");
    PaymentHandler handler = mock(PaymentHandler.class);
    when(mollieClient.payments()).thenReturn(handler);
    paymentResponse.setStatus(PaymentStatus.EXPIRED);

    when(mollieClient.payments().getPayment("mocked-id")).thenReturn(paymentResponse);
    UUID id = UUID.randomUUID();
    when(transactionRepository.findByIdForUpdateTopup(id)).thenReturn(transaction);
    when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);
    TopupTransaction updatedTransaction = depositService.validateTransaction(id);

    Mockito.verify(transactionRepository).saveAndFlush(transaction);
    assertEquals(transaction, updatedTransaction);
  }

  @Test
  public void testValidateTransaction_CanceledStatus() throws Exception {
    TopupTransaction transaction = mock(TopupTransaction.class);
    when(transactionRepository.findByIdForUpdateTopup(Mockito.any())).thenReturn(transaction);
    when(transaction.getMollieId()).thenReturn("mocked-id");

    PaymentResponse paymentResponse = new PaymentResponse();
    paymentResponse.setId("mocked-id");
    PaymentHandler handler = mock(PaymentHandler.class);
    when(mollieClient.payments()).thenReturn(handler);
    paymentResponse.setStatus(PaymentStatus.CANCELED);

    when(mollieClient.payments().getPayment("mocked-id")).thenReturn(paymentResponse);
    UUID id = UUID.randomUUID();
    when(transactionRepository.findByIdForUpdateTopup(id)).thenReturn(transaction);
    when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);
    TopupTransaction updatedTransaction = depositService.validateTransaction(id);

    Mockito.verify(transactionRepository).saveAndFlush(transaction);
    assertEquals(transaction, updatedTransaction);
  }

  @Test
  public void testCreatePayment_UserNotFound_ThrowsException() {
    TopupTransaction transaction = mock();
    when(transaction.getUser()).thenReturn(new User("missing", "missing@email", "missingid"));
    when(transaction.getId()).thenReturn(UUID.randomUUID());
    when(transaction.getAmount()).thenReturn(BigDecimal.ZERO);
    when(userRepository.findByOpenID(anyString())).thenReturn(Optional.empty());

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> depositService.createPayment(transaction));

    assertEquals("User not found", exception.getMessage());
  }

  @Test
  public void testGetMollieUrl_WhenMollieThrows_ShouldReturnRuntimeExceptionWithDetail()
      throws Exception {
    TopupTransaction t = mock();
    User u = createUser();
    when(t.getUser()).thenReturn(u);
    when(t.getId()).thenReturn(UUID.randomUUID());
    when(t.getAmount()).thenReturn(BigDecimal.ONE);
    when(userRepository.findByOpenID(anyString())).thenReturn(Optional.of(u));

    // Simulate a MollieException with top-level "detail"
    MollieException ex = mock(MollieException.class);
    Map<String, Object> details = new HashMap<>();
    details.put("detail", "Simulated Mollie error"); // ✅ must be at top level
    when(ex.getDetails()).thenReturn(details);

    PaymentHandler handler = mock(PaymentHandler.class);
    when(mollieClient.payments()).thenReturn(handler);
    when(handler.createPayment(Mockito.any())).thenThrow(ex);

    // Expect RuntimeException with full "Mollie error: ..." message
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> depositService.getMollieUrl(t));

    assertEquals("Mollie error: Simulated Mollie error", thrown.getMessage());
  }

  protected User createUser() {
    return new User("test", "test@email", "testid");
  }
}
