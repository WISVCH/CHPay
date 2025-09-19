package ch.wisv.chpay.api.rfid.contoller;

import ch.wisv.chpay.api.rfid.service.RfidPaymentService;
import ch.wisv.chpay.core.exception.InsufficientBalanceException;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.repository.UserRepository;
import ch.wisv.chpay.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/rfid", produces = MediaType.APPLICATION_JSON_VALUE)
public class RfidApiController {
  private static final Logger log = LoggerFactory.getLogger(RfidApiController.class);

  private final UserRepository userRepository;
  private final RfidPaymentService paymentService;
  private final UserService userService;

  public RfidApiController(
      UserRepository userRepository, RfidPaymentService paymentService, UserService userService) {
    this.userRepository = userRepository;
    this.paymentService = paymentService;
    this.userService = userService;
  }

  /**
   * Handles an RFID-based payment request for a specific payment request ID.
   *
   * <p>This endpoint attempts to find the user associated with the given RFID tag, and if found,
   * initiates a payment using the specified request ID. Returns different response statuses based
   * on the outcome:
   *
   * <p>200 OK - if the payment is successfully initiated 400 Bad Request - if there is an issue
   * such as insufficient balance or invalid request 404 Not Found - if no user is found for the
   * given RFID 500 Internal Server Error - for any unexpected server-side error
   *
   * @param rfid the RFID tag used to identify the user
   * @param requestId the UUID of the payment request
   * @return a {@code ResponseEntity} containing a message and status information
   */
  @PreAuthorize("hasRole('API_USER')")
  @PostMapping("/{rfid}/pay/{requestId}")
  public ResponseEntity<Map<String, String>> payWithRfid(
      @PathVariable String rfid, @PathVariable UUID requestId) {
    log.info("RFID payment requested: rfid={} requestId={}", rfid, requestId);
    User user = userRepository.findByRfid(rfid).orElse(null);
    if (user == null) {
      log.warn("No user found for RFID {}", rfid);
      return ResponseEntity.status(404)
          .body(Map.of("type", "error", "message", "No user found for RFID " + rfid));
    }

    try {
      String userName = paymentService.payFromRequest(user, requestId);
      return ResponseEntity.ok(
          Map.of("type", "message", "message", userName + "'s payment is processing..."));

    } catch (InsufficientBalanceException ibe) {
      // Now this will catch our pre‐check
      return ResponseEntity.badRequest()
          .body(Map.of("type", "error", "message", "Insufficient balance"));

    } catch (NoSuchElementException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("type", "error", "message", e.getMessage()));

    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(Map.of("type", "error", "message", "Unexpected server error"));
    }
  }

  /**
   * Changes a given user's RFID with the new RFID provided by JavaScript. Checks if the RFID is
   * already taken to better handle exceptions that will be thrown due to the unique constraint.
   *
   * @param rfid to use
   * @param openId of the user to change the RFID for
   * @return error/success message JSONs
   */
  @PreAuthorize("hasRole('API_USER')")
  @PostMapping("/{rfid}/change/{openId}")
  public ResponseEntity<Map<String, String>> changeUserRfid(
      @PathVariable String rfid, @PathVariable String openId) {
    log.info("RFID change requested: rfid={} openId={}", rfid, openId);

    // Check uniqueness
    Optional<User> existing = userRepository.findByRfid(rfid);
    if (existing.isPresent() && !existing.get().getOpenID().equals(openId)) {
      log.warn("RFID {} already in use", rfid);
      return ResponseEntity.badRequest()
          .body(Map.of("type", "error", "message", "RFID is already taken"));
    }

    try {
      userService.changeUserRfid(openId, rfid);
      return ResponseEntity.ok(
          Map.of("type", "success", "message", "User RFID updated successfully"));

    } catch (NoSuchElementException e) {
      log.warn(e.getMessage());
      return ResponseEntity.status(404).body(Map.of("type", "error", "message", e.getMessage()));

    } catch (Exception e) {
      log.error("Unexpected error changing RFID", e);
      return ResponseEntity.status(500)
          .body(Map.of("type", "error", "message", "Unexpected server error"));
    }
  }

  /**
   * Deletes the RFID association for the user identified by the given OpenID.
   *
   * <p>Path: DELETE /api/rfid/clear/{openId}
   *
   * <p>Returns 200 with {"type":"success","message":"User RFID cleared successfully"} if the
   * operation succeeds.
   *
   * <p>If no such user exists, returns 404 with {"type":"error","message":"User not found:
   * <openId>"}. On any other failure, returns 500 with {"type":"error","message":"Unexpected server
   * error"}.
   *
   * @param openId the OpenID of the user whose RFID should be removed
   * @return a JSON response indicating success or failure
   */
  @PreAuthorize("hasRole('API_USER')")
  @DeleteMapping("/clear/{openId}")
  public ResponseEntity<Map<String, String>> clearUserRfid(@PathVariable String openId) {
    log.info("RFID clear requested for openId={}", openId);
    try {
      userService.clearUserRfid(openId);
      return ResponseEntity.ok(
          Map.of("type", "success", "message", "User RFID cleared successfully"));
    } catch (NoSuchElementException e) {
      log.warn("Unable to clear RFID: {}", e.getMessage());
      return ResponseEntity.status(404).body(Map.of("type", "error", "message", e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error clearing RFID for {}:", openId, e);
      return ResponseEntity.status(500)
          .body(Map.of("type", "error", "message", "Unexpected server error"));
    }
  }
}
