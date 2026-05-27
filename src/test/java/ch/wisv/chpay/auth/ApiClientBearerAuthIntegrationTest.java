package ch.wisv.chpay.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.wisv.chpay.api.client.model.ApiClient;
import ch.wisv.chpay.api.client.model.ApiClientRole;
import ch.wisv.chpay.api.client.repository.ApiClientRepository;
import ch.wisv.chpay.core.model.transaction.ExternalTransaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ApiClientBearerAuthIntegrationTest {

  private MockMvc mockMvc;
  @Autowired private ApiClientRepository apiClientRepository;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private WebApplicationContext context;

  private String externalPaymentToken;
  private String secondExternalPaymentToken;
  private String ledstripToken;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    transactionRepository.deleteAll();
    apiClientRepository.deleteAll();
    externalPaymentToken =
        createClientToken(
            "events-client", "token-ext", "secret-ext", Set.of(ApiClientRole.EXTERNAL_PAYMENT));
    secondExternalPaymentToken =
        createClientToken(
            "other-events-client",
            "token-ext-2",
            "secret-ext-2",
            Set.of(ApiClientRole.EXTERNAL_PAYMENT));
    ledstripToken =
        createClientToken("led-client", "token-led", "secret-led", Set.of(ApiClientRole.LEDSTRIP));
  }

  @Test
  void externalPaymentStatusRequiresBearerToken() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/external-payment/status")
                .queryParam("PaymentId", UUID.randomUUID().toString()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void externalPaymentStatusAllowsClientWithExternalPaymentRole() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/external-payment/status")
                .queryParam("PaymentId", UUID.randomUUID().toString())
                .header("Authorization", "Bearer " + externalPaymentToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void externalPaymentStatusRejectsClientWithoutExternalPaymentRole() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/external-payment/status")
                .queryParam("PaymentId", UUID.randomUUID().toString())
                .header("Authorization", "Bearer " + ledstripToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void externalPaymentCreateRequiresBearerToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/external-payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validExternalPaymentRequestJson()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void externalPaymentCreateRejectsClientWithoutExternalPaymentRole() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/external-payment")
                .header("Authorization", "Bearer " + ledstripToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validExternalPaymentRequestJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void externalPaymentCreateRejectsRegularOidcUserSession() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/external-payment")
                .with(oidcLogin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validExternalPaymentRequestJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void externalPaymentCreateStoresApiClientReference() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/external-payment")
                    .header("Authorization", "Bearer " + externalPaymentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validExternalPaymentRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").isNotEmpty())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    String paymentId = extractJsonValue(responseBody, "transactionId");
    ExternalTransaction transaction =
        (ExternalTransaction)
            transactionRepository
                .findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new AssertionError("Created external transaction not found"));

    ApiClient client =
        apiClientRepository
            .findByTokenIdAndEnabledTrue("token-ext")
            .orElseThrow(() -> new AssertionError("Expected API client not found"));
    if (transaction.getApiClient() == null) {
      throw new AssertionError("Expected external transaction to reference creating API client");
    }
    if (!transaction.getApiClient().getId().equals(client.getId())) {
      throw new AssertionError("External transaction API client reference does not match creator");
    }
  }

  @Test
  void externalPaymentStatusDoesNotExposeOtherClientTransactions() throws Exception {
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/external-payment")
                    .header("Authorization", "Bearer " + externalPaymentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validExternalPaymentRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").isNotEmpty())
            .andReturn();

    String createResponseBody = createResult.getResponse().getContentAsString();
    String paymentId = extractJsonValue(createResponseBody, "transactionId");

    mockMvc
        .perform(
            get("/api/v1/external-payment/status")
                .queryParam("PaymentId", paymentId)
                .header("Authorization", "Bearer " + secondExternalPaymentToken))
        .andExpect(status().isNotFound());
  }

  private String createClientToken(
      String name, String tokenId, String secret, Set<ApiClientRole> roles) {
    ApiClient client = new ApiClient(name, tokenId, passwordEncoder.encode(secret), roles);
    apiClientRepository.saveAndFlush(client);
    return tokenId + "." + secret;
  }

  private String validExternalPaymentRequestJson() {
    return """
        {
          "amount": 12.50,
          "description": "Event ticket",
          "consumerName": "Alice",
          "consumerEmail": "alice@example.com",
          "redirectURL": "https://events.example/ok",
          "webhookURL": "https://events.example/hook",
          "fallbackURL": "https://events.example/fail",
          "metadata": {}
        }
        """;
  }

  private String extractJsonValue(String json, String key) {
    String pattern = "\"" + key + "\":\"";
    int start = json.indexOf(pattern);
    if (start < 0) {
      throw new AssertionError("Missing key in JSON: " + key);
    }
    int valueStart = start + pattern.length();
    int valueEnd = json.indexOf('"', valueStart);
    if (valueEnd < 0) {
      throw new AssertionError("Invalid JSON value for key: " + key);
    }
    return json.substring(valueStart, valueEnd);
  }
}
