package chpay.frontend.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import chpay.frontend.events.CHPaymentRequest;
import chpay.frontend.events.CHPaymentResponse;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PaymentDataTest {

  @Test
  void chPaymentRequest_constructorAndGetters_workCorrectly() {
    BigDecimal amount = new BigDecimal("123.45");
    String description = "Test Payment";
    String consumerName = "John Doe";
    String consumerEmail = "john.doe@example.com";
    String redirectURL = "http://example.com/redirect";
    String webhookURL = "http://example.com/webhook";
    String fallbackURL = "http://example.com/fallback";
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("orderId", "ORD-001");
    metadata.put("itemCount", 3);

    CHPaymentRequest request =
        new CHPaymentRequest(
            amount,
            description,
            consumerName,
            consumerEmail,
            redirectURL,
            webhookURL,
            fallbackURL,
            metadata);

    assertNotNull(request);
    assertEquals(amount, request.getAmount());
    assertEquals(description, request.getDescription());
    assertEquals(consumerName, request.getConsumerName());
    assertEquals(consumerEmail, request.getConsumerEmail());
    assertEquals(redirectURL, request.getRedirectURL());
    assertEquals(webhookURL, request.getWebhookURL());
    assertEquals(fallbackURL, request.getFallbackURL());
    assertEquals(metadata, request.getMetadata());

    CHPaymentRequest request2 =
        new CHPaymentRequest(
            amount,
            description,
            consumerName,
            consumerEmail,
            redirectURL,
            webhookURL,
            fallbackURL,
            Collections.emptyMap());
    assertEquals(Collections.emptyMap(), request2.getMetadata());
  }

  @Test
  void chPaymentResponse_constructorAndGetters_workCorrectly() {
    String transactionId = "tx_12345abc";
    String checkoutUrl = "https://checkout.chpay.com/12345abc";

    CHPaymentResponse response = new CHPaymentResponse(transactionId, checkoutUrl);

    assertNotNull(response);
    assertEquals(transactionId, response.getTransactionId());
    assertEquals(checkoutUrl, response.getCheckoutUrl());
  }

  @Test
  void chPaymentRequest_dataLombokFeatures_workAsExpected() {
    BigDecimal amount = new BigDecimal("10.00");
    String desc = "Gadget";
    String name = "Jane";
    String email = "jane@example.com";
    String redir = "redir.com";
    String webhook = "webhook.com";
    String fallback = "fallback.com";
    Map<String, Object> meta = Collections.singletonMap("key", "value");

    CHPaymentRequest req1 =
        new CHPaymentRequest(amount, desc, name, email, redir, webhook, fallback, meta);
    CHPaymentRequest req2 =
        new CHPaymentRequest(amount, desc, name, email, redir, webhook, fallback, meta);
    CHPaymentRequest req3 =
        new CHPaymentRequest(
            new BigDecimal("20.00"), desc, name, email, redir, webhook, fallback, meta);

    assertEquals(req1, req2);
    assertEquals(req1.hashCode(), req2.hashCode());
    assertNotNull(req1.toString());
    org.junit.jupiter.api.Assertions.assertNotEquals(req1, req3);
  }

  @Test
  void chPaymentResponse_dataLombokFeatures_workAsExpected() {
    String txId = "tx_xyz";
    String url = "url.com";

    CHPaymentResponse res1 = new CHPaymentResponse(txId, url);
    CHPaymentResponse res2 = new CHPaymentResponse(txId, url);
    CHPaymentResponse res3 = new CHPaymentResponse("tx_abc", url);

    assertEquals(res1, res2);
    assertEquals(res1.hashCode(), res2.hashCode());
    assertNotNull(res1.toString());

    org.junit.jupiter.api.Assertions.assertNotEquals(res1, res3);
  }
}
