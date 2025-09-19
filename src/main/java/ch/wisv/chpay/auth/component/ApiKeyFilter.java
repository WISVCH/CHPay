package ch.wisv.chpay.auth.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {

  @Value("${chpay.api-key}")
  private String expectedApiKey;

  /**
   * API key filter that checks the API key in the request header for API requests. If the API key
   * is valid, sets the ROLE_API_USER role and continues. If invalid, returns 401 Unauthorized. For
   * non-API requests, passes through without modification.
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
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();

    // Only apply this filter to API requests
    if (!path.startsWith("/api/")) {
      filterChain.doFilter(request, response);
      return;
    }

    String apiKey = request.getHeader("X-API-KEY");

    if (apiKey == null || !expectedApiKey.equals(apiKey)) {
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or missing API key");
      return;
    }

    // Set API_USER role for valid API key
    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_API_USER"));
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("api-user", null, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);

    filterChain.doFilter(request, response);
  }
}
