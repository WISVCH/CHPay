package chpay.frontend.customer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.transactions.ExternalTransaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.frontend.events.CHPaymentRequest;
import chpay.frontend.events.CHPaymentResponse;
import chpay.frontend.events.service.ExternalPaymentServiceImpl;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class ExternalPaymentServiceImplTest {

  @Mock private TransactionRepository repository;
  @Mock private RestTemplate restTemplate;

  @InjectMocks private ExternalPaymentServiceImpl service;

  @Captor private ArgumentCaptor<ExternalTransaction> transactionCaptor;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(service, "CHPayUri", "http://localhost:8080");
  }

  @Test
  void createTransaction_shouldReturnCorrectResponse() {
    // Arrange
    CHPaymentRequest request = mock(CHPaymentRequest.class);
    when(request.getAmount()).thenReturn(BigDecimal.TEN);
    when(request.getDescription()).thenReturn("Test Payment");
    when(request.getRedirectURL()).thenReturn("http://redirect.url");
    when(request.getWebhookURL()).thenReturn("http://webhook.url");
    when(request.getFallbackURL()).thenReturn("http://fallback.url");

    UUID rand = UUID.randomUUID();
    when(repository.save(any(ExternalTransaction.class)))
        .thenAnswer(
            invocation -> {
              ExternalTransaction tx = invocation.getArgument(0);
              ReflectionTestUtils.setField(tx, "id", rand);
              return tx;
            });

    CHPaymentResponse response = service.createTransaction(request);

    verify(repository).save(transactionCaptor.capture());
    ExternalTransaction savedTx = transactionCaptor.getValue();
    assertEquals("Test Payment", savedTx.getDescription());
    assertEquals("http://localhost:8080/payment/transaction/" + rand, response.getCheckoutUrl());
    assertEquals(rand.toString(), response.getTransactionId());
  }

  @Test
  void postToWebhook_shouldRedirectToFallbackIfWebhookIsNull() {
    // Arrange
    ExternalTransaction tx = mock(ExternalTransaction.class);
    when(tx.getFallbackUrl()).thenReturn("http://fallback.url");
    when(tx.getWebhookUrl()).thenReturn(null);

    String result = service.postToWebhook("123", tx);

    assertEquals("redirect:http://fallback.url?message=Payment+Failed", result);
  }

  @Test
  void postToWebhook_shouldRedirectToWebhookUrlIfSuccessful() {
    ExternalTransaction tx = mock(ExternalTransaction.class);
    when(tx.getRedirectUrl()).thenReturn("http://redirect.url");
    when(tx.getWebhookUrl()).thenReturn("http://webhook.url");

    when(restTemplate.postForEntity(
            eq("http://webhook.url"), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("OK"));

    String result = service.postToWebhook("123", tx);

    assertEquals("redirect:http://redirect.url", result);
  }
}
