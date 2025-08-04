package chpay.frontend.shared;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class CookieConsentFilter implements Filter {

  /**
   * Filters incoming HTTP requests to check for a cookie named "cookie_consent" with the value
   * "true". If the consent is provided, it sets a request attribute "cookieConsentGiven" to true,
   * otherwise false. Passes the request and response to the next filter in the chain.
   *
   * @param request the {@link ServletRequest} object that contains the client request
   * @param response the {@link ServletResponse} object that contains the response sent to the
   *     client
   * @param chain the {@link FilterChain} object that enables invoking the next filter in the chain
   * @throws IOException if an I/O error occurs
   * @throws ServletException if a servlet-specific error occurs
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;

    Cookie[] cookies = req.getCookies();
    if (cookies == null) cookies = new Cookie[0];

    boolean consentGiven = false;
    for (Cookie c : cookies) {
      if ("cookie_consent".equals(c.getName()) && "true".equals(c.getValue())) {
        consentGiven = true;
        break;
      }
    }

    // make it visible to Thymeleaf
    req.setAttribute("cookieConsentGiven", consentGiven);

    chain.doFilter(request, response);
  }
}
