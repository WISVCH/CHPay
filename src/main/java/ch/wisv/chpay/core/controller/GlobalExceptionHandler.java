package ch.wisv.chpay.core.controller;

import ch.wisv.chpay.core.dto.NotificationPayload;
import ch.wisv.chpay.core.exception.IllegalRefundException;
import ch.wisv.chpay.core.exception.InsufficientBalanceException;
import ch.wisv.chpay.core.exception.TransactionAlreadyFulfilled;
import ch.wisv.chpay.core.service.NotificationService;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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
    notificationService.addErrorMessage(redirectAttributes, "Invalid amount format.");

    // Get the request URI and referer
    String requestURI = request.getRequestURI();
    String referer = request.getHeader("Referer");

    // If the request is related to topup operations, redirect to topup page
    if (requestURI != null && requestURI.contains("/topup")) {
      return new RedirectView("/topup");
    }

    // If referer is from topup page, redirect to topup
    if (referer != null && referer.contains("/topup")) {
      return new RedirectView("/topup");
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
    notificationService.addErrorMessage(redirectAttributes, "Error fetching object.");

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
    notificationService.addErrorMessage(redirectAttributes, "Invalid date.");

    // Get the request URI and referer
    String requestURI = request.getRequestURI();
    String referer = request.getHeader("Referer");

    // redirect back to the view the request is related to
    if (requestURI != null && requestURI.contains("/admin/transaction")) {
      return new RedirectView("/admin/transactions");
    }
    if (requestURI != null && requestURI.contains("/admin/user")) {
      return new RedirectView("/admin/users");
    }
    if (requestURI != null && requestURI.contains("/transaction")) {
      return new RedirectView("transactions");
    }

    // Otherwise, redirect to previous page
    return new RedirectView(referer);
  }

  @ExceptionHandler({AccessDeniedException.class, ResponseStatusException.class})
  public Object handleForbiddenExceptions(
      Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
    logger.error("Forbidden/AccessDenied Exception occurred: {}", ex.getMessage(), ex);
    HttpStatusCode status =
        ex instanceof ResponseStatusException responseStatusException
            ? responseStatusException.getStatusCode()
            : HttpStatus.FORBIDDEN;
    String safeMessage = safeMessageForStatus(status);

    // API requests should receive proper HTTP responses instead of redirects.
    if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/")) {
      return ResponseEntity.status(status).body(NotificationPayload.error(safeMessage));
    }

    // Set status code and error attributes for the error page
    redirectAttributes.addFlashAttribute("statuscode", status.value());
    HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
    redirectAttributes.addFlashAttribute(
        "errorname", resolvedStatus != null ? resolvedStatus.getReasonPhrase() : "Error");
    redirectAttributes.addFlashAttribute("message", safeMessage);

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
    notificationService.addErrorMessage(redirectAttributes, safeMessageForDomainException(ex));

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
    notificationService.addErrorMessage(redirectAttributes, "Refund could not be processed.");

    // Redirect to the previous page
    return new RedirectView(referer);
  }

  @ExceptionHandler(TransactionAlreadyFulfilled.class)
  public RedirectView transactionAlreadyFulfilledException(
      TransactionAlreadyFulfilled ex, RedirectAttributes redirectAttributes) {
    logger.warn("TransactionAlreadyFulfilledException occurred: {}", ex.getMessage());
    notificationService.addErrorMessage(
        redirectAttributes, "This transaction has already been completed.");
    return new RedirectView("/index");
  }

  @ExceptionHandler(OAuth2AuthenticationException.class)
  public RedirectView handleOAuth2AuthenticationException(
      OAuth2AuthenticationException ex,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes) {
    logger.error("OAuth2AuthenticationException occurred: {}", ex.getMessage(), ex);
    notificationService.addErrorMessage(
        redirectAttributes, "Authentication failed. Please try again.");

    return new RedirectView("/login");
  }

  /** Handle all other exceptions with JSON response */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleAnyException(Exception ex, WebRequest request) {
    logger.error("An unhandled exception occurred: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(NotificationPayload.error("An unexpected error occurred."));
  }

  private String safeMessageForStatus(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> "Invalid request.";
      case 401 -> "Authentication is required.";
      case 403 -> "Access denied.";
      case 404 -> "The requested resource was not found.";
      case 409 -> "Request conflicts with the current state.";
      default -> "Request could not be processed.";
    };
  }

  private String safeMessageForDomainException(Exception ex) {
    if (ex instanceof InsufficientBalanceException) {
      return "Insufficient balance.";
    }
    if (ex instanceof NoSuchElementException) {
      return "The requested resource was not found.";
    }
    if (ex instanceof IllegalArgumentException) {
      return "Invalid request.";
    }
    if (ex instanceof IllegalStateException) {
      return "This action is not allowed right now.";
    }
    return "Request could not be processed.";
  }
}
