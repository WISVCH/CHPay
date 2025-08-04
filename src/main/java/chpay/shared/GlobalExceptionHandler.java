package chpay.shared;

import chpay.DatabaseHandler.Exceptions.IllegalRefundException;
import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import chpay.DatabaseHandler.Exceptions.TransactionAlreadyFulfilled;
import chpay.shared.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.view.RedirectView;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @Autowired private NotificationService notificationService;
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NumberFormatException.class)
  public RedirectView handleNumberFormatException(
      NumberFormatException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
    logger.error("NumberFormatException occurred: {}", ex.getMessage(), ex);
    // Add error notification
    notificationService.addErrorMessage(
        redirectAttributes, "Invalid amount format: " + ex.getMessage());

    // Get the request URI and referer
    String requestURI = request.getRequestURI();
    String referer = request.getHeader("Referer");

    // If the request is related to balance operations, redirect to balance page
    if (requestURI != null && requestURI.contains("/balance")) {
      return new RedirectView("/balance");
    }

    // If referer is from balance page, redirect to balance
    if (referer != null && referer.contains("/balance")) {
      return new RedirectView("/balance");
    }

    // If no referer is available, redirect to index
    if (referer == null || referer.isEmpty()) {
      return new RedirectView("/index");
    }

    // Otherwise, redirect to the previous page
    return new RedirectView(referer);
  }

  @ExceptionHandler(JsonProcessingException.class)
  public RedirectView handleJsonProcessingException(
      JsonProcessingException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    logger.error("JsonProcessingException occurred: {}", ex.getMessage(), ex);
    // Add error notification
    notificationService.addErrorMessage(
        redirectAttributes, "Error fetching object: " + ex.getMessage());

    // Get the request URI and referer
    String requestURI = request.getRequestURI();
    String referer = request.getHeader("Referer");

    // If the request is related to admin view, redirect to admin index
    if (requestURI != null && requestURI.contains("/admin")) {
      return new RedirectView("/admin");
    }

    // Otherwise, redirect to index
    return new RedirectView("/index");
  }

  @ExceptionHandler(DateTimeParseException.class)
  public RedirectView handleDateTimeParseException(
      DateTimeParseException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    logger.error("DateTimeParseException occurred: {}", ex.getMessage(), ex);
    // Add error notification
    notificationService.addErrorMessage(redirectAttributes, "Invalid Date: " + ex.getMessage());

    // Get the request URI and referer
    String requestURI = request.getRequestURI();
    String referer = request.getHeader("Referer");

    // redirect back to the view the request is related to
    if (requestURI != null && requestURI.contains("/admin/transactions")) {
      return new RedirectView("/admin/transactions");
    }
    if (requestURI != null && requestURI.contains("/admin/users")) {
      return new RedirectView("/admin/users");
    }
    if (requestURI != null && requestURI.contains("/transactions")) {
      return new RedirectView("transactions");
    }

    // Otherwise, redirect to previous page
    return new RedirectView(referer);
  }

  @ExceptionHandler({AccessDeniedException.class, ResponseStatusException.class})
  public Object handleForbiddenExceptions(
      Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
    // Log the exception (optional, useful for debugging)
    logger.error("Forbidden/AccessDenied Exception occurred: {}", ex.getMessage(), ex);
    // Handle ResponseStatusException differently if it's not FORBIDDEN
    if (ex instanceof ResponseStatusException) {
      ResponseStatusException statusEx = (ResponseStatusException) ex;
      if (statusEx.getStatusCode() != HttpStatus.FORBIDDEN) {
        // For other status codes, use a ResponseEntity
        return ResponseEntity.status(statusEx.getStatusCode())
            .body(NotificationPayload.error(statusEx.getReason()));
      }
    }

    // Get appropriate message based on exception type
    String message;
    HttpStatusCode status;
    if (ex instanceof ResponseStatusException) {
      message = ((ResponseStatusException) ex).getReason();
      status = ((ResponseStatusException) ex).getStatusCode();
    } else {
      message = ex.getMessage();
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    // Set status code and error attributes for the error page
    redirectAttributes.addFlashAttribute("statuscode", status.value());
    redirectAttributes.addFlashAttribute("errorname", HttpStatus.FORBIDDEN.getReasonPhrase());
    redirectAttributes.addFlashAttribute("message", message);

    // Redirect to the error page
    return new RedirectView("/error");
  }

  /** Handle 404 errors by redirecting to the previous page with an error notification */
  @ExceptionHandler(NoResourceFoundException.class)
  public RedirectView noResourceFoundException(
      NoResourceFoundException ex, // Changed from NoHandlerFoundException
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    logger.warn("NoResourceFoundException (404) occurred for URL: {}", request.getRequestURI());
    // Get the referer (previous page URL)
    String referer = request.getHeader("Referer");

    // If no referer is available, redirect to home page
    if (referer == null || referer.isEmpty()) {
      referer = "/index";
    }

    // Add error notification
    notificationService.addErrorMessage(
        redirectAttributes,
        "The page you requested does not exist." // Modified to avoid using ex.getRequestURL()
        );

    // Redirect to the previous page
    return new RedirectView(referer);
  }

  @ExceptionHandler({
    InsufficientBalanceException.class,
    NoSuchElementException.class,
    IllegalStateException.class,
    IllegalArgumentException.class
  })
  public RedirectView insufficientBalanceException(
      Exception ex, // Changed from NoHandlerFoundException
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    logger.error("Exception occurred: {}", ex.getMessage(), ex);
    // Get the referer (previous page URL)
    String referer = request.getHeader("Referer");

    // If no referer is available, redirect to home page
    if (referer == null || referer.isEmpty()) {
      referer = "/index";
    }

    // Add error notification
    notificationService.addErrorMessage(
        redirectAttributes, ex.getMessage() // Modified to avoid using ex.getRequestURL()
        );

    // Redirect to the previous page
    return new RedirectView(referer);
  }

  @ExceptionHandler({IllegalRefundException.class})
  public RedirectView illegalRefundException(
      IllegalRefundException ex, // Changed from NoHandlerFoundException
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {

    // Get the referer (previous page URL)
    String referer = request.getHeader("Referer");
    logger.error("IllegalRefundException occurred: {}", ex.getMessage(), ex);
    // If no referer is available, redirect to home page
    if (referer == null || referer.isEmpty()) {
      referer = "/index";
    }

    // Add error notification
    notificationService.addErrorMessage(
        redirectAttributes, ex.getMessage() // Modified to avoid using ex.getRequestURL()
        );

    // Redirect to the previous page
    return new RedirectView(referer);
  }

  @ExceptionHandler(TransactionAlreadyFulfilled.class)
  public RedirectView transactionAlreadyFulfilledException(
      TransactionAlreadyFulfilled ex, RedirectAttributes redirectAttributes) {
    logger.warn("TransactionAlreadyFulfilledException occurred: {}", ex.getMessage());
    notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
    return new RedirectView("/index");
  }

  @ExceptionHandler(OAuth2AuthenticationException.class)
  public RedirectView handleOAuth2AuthenticationException(
      OAuth2AuthenticationException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    // Log the exception (optional, useful for debugging)
    logger.error("OAuth2AuthenticationException occurred: {}", ex.getMessage(), ex);
    String message = ex.getMessage();

    HttpStatus status;
    if (ex.getMessage().contains("User not found")) {
      status = HttpStatus.NOT_FOUND;
    } else if (ex.getMessage().contains("Multiple users found")) {
      status = HttpStatus.CONFLICT;
    } else status = HttpStatus.INTERNAL_SERVER_ERROR;

    notificationService.addErrorMessage(redirectAttributes, message);

    return new RedirectView("/login");
  }

  /** Handle all other exceptions with JSON response */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleAnyException(Exception ex, WebRequest request) {
    logger.error("An unhandled exception occurred: {}", ex.getMessage(), ex);
    // Send as a red error notification
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(NotificationPayload.error(ex.getMessage()));
  }
}
