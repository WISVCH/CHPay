package chpay.paymentbackend.Auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyFilterTest {

  private ApiKeyFilter apiKeyFilter;
  private FilterChain filterChain;

  private final String validApiKey = "expected-api-key";

  @BeforeEach
  void setUp() {
    apiKeyFilter = new ApiKeyFilter();
    // Manually inject the value since @Value is not processed in unit tests
    TestUtils.setField(apiKeyFilter, "expectedApiKey", validApiKey);
    filterChain = mock(FilterChain.class);
  }

  @Test
  void allowsRequestWithValidApiKey() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events/42");
    request.addHeader("X-API-KEY", validApiKey);
    MockHttpServletResponse response = new MockHttpServletResponse();

    apiKeyFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertEquals(200, response.getStatus());
  }

  @Test
  void blocksRequestWithInvalidApiKey() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events/42");
    request.addHeader("X-API-KEY", "wrong-key");
    MockHttpServletResponse response = new MockHttpServletResponse();

    apiKeyFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertEquals(401, response.getStatus());
  }

  @Test
  void allowsRequestToStatusEndpoint() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events/status");
    MockHttpServletResponse response = new MockHttpServletResponse();

    apiKeyFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertEquals(200, response.getStatus());
  }

  @Test
  void allowsRequestToUnrelatedEndpoint() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/unrelated");
    MockHttpServletResponse response = new MockHttpServletResponse();

    apiKeyFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertEquals(200, response.getStatus());
  }
}
