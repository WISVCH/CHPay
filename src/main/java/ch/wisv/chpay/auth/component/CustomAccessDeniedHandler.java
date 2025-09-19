package ch.wisv.chpay.auth.component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {

    request.setAttribute("statuscode", HttpStatus.FORBIDDEN.value());
    request.setAttribute("errorname", HttpStatus.FORBIDDEN.getReasonPhrase());
    request.setAttribute("message", "Access denied: Admin privileges required");
    request.getRequestDispatcher("/error").forward(request, response);
  }
}
