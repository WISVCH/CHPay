package ch.wisv.chpay.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.wisv.chpay.api.client.model.ApiClient;
import ch.wisv.chpay.api.client.model.ApiClientRole;
import ch.wisv.chpay.api.client.repository.ApiClientRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ApiClientBearerAuthIntegrationTest {

  private MockMvc mockMvc;
  @Autowired private ApiClientRepository apiClientRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private WebApplicationContext context;

  private String externalPaymentToken;
  private String ledstripToken;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    apiClientRepository.deleteAll();
    externalPaymentToken = createClientToken("events-client", "token-ext", "secret-ext", Set.of(ApiClientRole.EXTERNAL_PAYMENT));
    ledstripToken = createClientToken("led-client", "token-led", "secret-led", Set.of(ApiClientRole.LEDSTRIP));
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
  void ledstripHandshakeRejectsClientWithoutLedstripRole() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/leds/stream/info")
                .queryParam("t", "123")
                .header("Authorization", "Bearer " + externalPaymentToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void ledstripHandshakeAllowsClientWithLedstripRole() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/leds/stream/info")
                .queryParam("t", "123")
                .header("Authorization", "Bearer " + ledstripToken))
        .andExpect(status().isOk());
  }

  private String createClientToken(
      String name, String tokenId, String secret, Set<ApiClientRole> roles) {
    ApiClient client = new ApiClient(name, tokenId, passwordEncoder.encode(secret), roles);
    apiClientRepository.saveAndFlush(client);
    return tokenId + "." + secret;
  }
}
