package chpay.paymentbackend.Auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {

  @Value("${chpay.api-key}")
  @NonNull
  private String expectedApiKey;

  /**
   * API key filter that checks the API key in the request header. Only applies to requests on the
   * /api/events/** mappings. If the API key is invalid, an HTTP 401 Unauthorized response is
   * returned. If the API key is valid, the request is passed to the next filter in the chain. If
   * the request is not a /api/events/** mapping, or is a /api/events/status mapping, the request is
   * passed to the next filter in the chain.
   *
   * @param request the {@code HttpServletRequest} object that contains the client request
   * @param response the {@code HttpServletResponse} object that contains the response to the client
   * @param filterChain the {@code FilterChain} object that enables invoking the next filter in the
   *     chain
   * @throws ServletException if an error occurs while processing the request or response.
   * @throws IOException if an I/O error occurs while processing the request or response.
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();

    if (path.equals("/api/events/status")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Only apply this filter to /api/events/**
    if (!path.startsWith("/api/events")) {
      filterChain.doFilter(request, response); // do nothing for other routes
      return;
    }

    String apiKey = request.getHeader("X-API-KEY");

    if (!expectedApiKey.equals(apiKey)) {
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API key");
      return;
    }

    filterChain.doFilter(request, response); // valid API key: continue
  }
}
