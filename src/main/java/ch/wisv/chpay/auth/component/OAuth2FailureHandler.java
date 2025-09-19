package ch.wisv.chpay.auth.component;

import ch.wisv.chpay.core.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

  private final NotificationService notificationService;

  public OAuth2FailureHandler(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /**
   * Handles authentication failure scenarios by redirecting the user to the login page with an
   * error notification message.
   *
   * @param request the {@code HttpServletRequest} object that contains the client request
   * @param response the {@code HttpServletResponse} object that contains the response to the client
   * @param ex the {@code AuthenticationException} instance that contains details about the
   *     authentication failure
   * @throws IOException if an input or output error occurs while the handler redirects the response
   */
  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {

    FlashMap flashMap = new FlashMap();
    flashMap.put("notificationType", "error");
    flashMap.put("notificationMessage", ex.getMessage());

    new SessionFlashMapManager().saveOutputFlashMap(flashMap, request, response);

    response.sendRedirect("/login");
  }
}
