package ch.wisv.chpay.auth.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

  private static final Logger logger = LoggerFactory.getLogger(OAuth2FailureHandler.class);

  private static final String GENERIC_AUTH_FAILURE_MESSAGE =
      "Authentication failed. Please try again.";

  public OAuth2FailureHandler() {}

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

    logAuthenticationFailure(request, ex);

    FlashMap flashMap = new FlashMap();
    flashMap.put("notificationType", "error");
    flashMap.put("notificationMessage", GENERIC_AUTH_FAILURE_MESSAGE);

    new SessionFlashMapManager().saveOutputFlashMap(flashMap, request, response);

    response.sendRedirect("/login?error");
  }

  private void logAuthenticationFailure(HttpServletRequest request, AuthenticationException ex) {
    HttpSession session = request.getSession(false);
    String errorCode =
        ex instanceof OAuth2AuthenticationException oauth2Exception
            ? oauth2Exception.getError().getErrorCode()
            : ex.getClass().getSimpleName();

    logger.warn(
        "OAuth2 authentication failed: errorCode={}, message={}, requestUri={}, queryPresent={}, "
            + "stateHash={}, requestedSessionIdPresent={}, requestedSessionIdValid={}, "
            + "currentSessionPresent={}, currentSessionIdHash={}, userAgent={}, forwardedProto={}, "
            + "forwardedHost={}, host={}",
        errorCode,
        ex.getMessage(),
        request.getRequestURI(),
        request.getQueryString() != null,
        hashForLog(request.getParameter("state")),
        request.getRequestedSessionId() != null,
        request.isRequestedSessionIdValid(),
        session != null,
        session == null ? null : hashForLog(session.getId()),
        request.getHeader("User-Agent"),
        request.getHeader("X-Forwarded-Proto"),
        request.getHeader("X-Forwarded-Host"),
        request.getHeader("Host"));
  }

  private String hashForLog(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 8);
    } catch (NoSuchAlgorithmException ex) {
      return "unavailable";
    }
  }
}
