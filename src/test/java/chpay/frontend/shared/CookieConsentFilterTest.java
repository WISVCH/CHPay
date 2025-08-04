package chpay.frontend.shared;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CookieConsentFilterTest {

  private CookieConsentFilter filter;
  private HttpServletRequest request;
  private ServletResponse response;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    filter = new CookieConsentFilter();
    request = mock(HttpServletRequest.class);
    response = mock(ServletResponse.class);
    chain = mock(FilterChain.class);
  }

  @Test
  void testDoFilter_consentGiven_setsAttributeTrue() throws IOException, ServletException {
    Cookie[] cookies = {new Cookie("session", "abc123"), new Cookie("cookie_consent", "true")};

    when(request.getCookies()).thenReturn(cookies);

    filter.doFilter(request, response, chain);

    verify(request).setAttribute("cookieConsentGiven", true);
    verify(chain).doFilter(request, response);
  }

  @Test
  void testDoFilter_consentMissing_setsAttributeFalse() throws IOException, ServletException {
    Cookie[] cookies = {new Cookie("session", "abc123"), new Cookie("cookie_consent", "false")};

    when(request.getCookies()).thenReturn(cookies);

    filter.doFilter(request, response, chain);

    verify(request).setAttribute("cookieConsentGiven", false);
    verify(chain).doFilter(request, response);
  }

  @Test
  void testDoFilter_noCookies_setsAttributeFalse() throws IOException, ServletException {
    when(request.getCookies()).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(request).setAttribute("cookieConsentGiven", false);
    verify(chain).doFilter(request, response);
  }

  @Test
  void testDoFilter_unrelatedCookiesOnly_setsAttributeFalse() throws IOException, ServletException {
    Cookie[] cookies = {new Cookie("session", "xyz")};

    when(request.getCookies()).thenReturn(cookies);

    filter.doFilter(request, response, chain);

    verify(request).setAttribute("cookieConsentGiven", false);
    verify(chain).doFilter(request, response);
  }
}
