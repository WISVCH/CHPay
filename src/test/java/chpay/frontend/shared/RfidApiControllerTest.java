package chpay.frontend.shared;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.paymentbackend.service.RfidPaymentService;
import chpay.paymentbackend.service.UserService;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RfidApiControllerTest {

  @Mock private UserRepository userRepository;
  @Mock private RfidPaymentService paymentService;
  @Mock private UserService userService;
  @InjectMocks private RfidApiController controller;

  private final String testRfid = "test-rfid";
  private final String testOpenId = "user-123";
  private final UUID requestId = UUID.randomUUID();
  private User user;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setOpenID(testOpenId);
    user.setRfid(testRfid);
    reset(userRepository, paymentService, userService);
  }

  /** Tests bad weather scenario where no user is tied to the scanned RFID. */
  @Test
  void payWithRfid_UserNotFound_Returns404() {
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.empty());

    ResponseEntity<Map<String, String>> response = controller.payWithRfid(testRfid, requestId);

    assertEquals(404, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertTrue(response.getBody().get("message").contains(testRfid));
  }

  /**
   * Tests good weather behavior for paying with an RFID.
   *
   * @throws Exception
   */
  @Test
  void payWithRfid_Success_Returns200() throws Exception {
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.of(user));
    when(paymentService.payFromRequest(user, requestId)).thenReturn("Alice");

    ResponseEntity<Map<String, String>> response = controller.payWithRfid(testRfid, requestId);

    assertEquals(200, response.getStatusCode().value());
    assertEquals("message", response.getBody().get("type"));
    assertTrue(response.getBody().get("message").contains("Alice's payment is processing"));
  }

  /**
   * Tests bad weather scenario where the user tied to the RFID has insufficient balance.
   *
   * @throws Exception
   */
  @Test
  void payWithRfid_InsufficientBalance_Returns400() throws Exception {
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.of(user));
    doThrow(new InsufficientBalanceException("Insufficient balance"))
        .when(paymentService)
        .payFromRequest(user, requestId);

    ResponseEntity<Map<String, String>> response = controller.payWithRfid(testRfid, requestId);

    assertEquals(400, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertEquals("Insufficient balance", response.getBody().get("message"));
  }

  /**
   * Tests generic bad weather scenario.
   *
   * @throws Exception
   */
  @Test
  void payWithRfid_GenericException_Returns500() throws Exception {
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.of(user));
    doThrow(new RuntimeException("Boom")).when(paymentService).payFromRequest(user, requestId);

    ResponseEntity<Map<String, String>> response = controller.payWithRfid(testRfid, requestId);

    assertEquals(500, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertEquals("Unexpected server error", response.getBody().get("message"));
  }

  /**
   * Tests bad weather scenario for assigning a new RFID to a user, where the RFID is already taken.
   */
  @Test
  void changeUserRfid_RfidAlreadyTakenByOther_Returns400() {
    User other = new User();
    other.setOpenID("other-id");
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.of(other));

    ResponseEntity<Map<String, String>> response = controller.changeUserRfid(testRfid, testOpenId);

    assertEquals(400, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertEquals("RFID is already taken", response.getBody().get("message"));
  }

  /** Tests good weather scenario for adding an RFID to a user. */
  @Test
  void changeUserRfid_Success_Returns200() {
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.empty());
    // No exception thrown by service

    ResponseEntity<Map<String, String>> response = controller.changeUserRfid(testRfid, testOpenId);

    verify(userService).changeUserRfid(testOpenId, testRfid);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("success", response.getBody().get("type"));
    assertEquals("User RFID updated successfully", response.getBody().get("message"));
  }

  /** Tests bad weather scenario for assigning a new RFID to a user where the user is not found. */
  @Test
  void changeUserRfid_NoSuchElementException_Returns404() {
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.empty());
    doThrow(new NoSuchElementException("User not found"))
        .when(userService)
        .changeUserRfid(testOpenId, testRfid);

    ResponseEntity<Map<String, String>> response = controller.changeUserRfid(testRfid, testOpenId);

    assertEquals(404, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertEquals("User not found", response.getBody().get("message"));
  }

  /** Tests generic bad weather scenario. */
  @Test
  void changeUserRfid_GenericException_Returns500() {
    when(userRepository.findByRfid(testRfid)).thenReturn(Optional.empty());
    doThrow(new RuntimeException("DB down")).when(userService).changeUserRfid(testOpenId, testRfid);

    ResponseEntity<Map<String, String>> response = controller.changeUserRfid(testRfid, testOpenId);

    assertEquals(500, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertEquals("Unexpected server error", response.getBody().get("message"));
  }

  /** Tests successful clearing of a user's RFID. */
  @Test
  void clearUserRfid_Success_Returns200() {
    doNothing().when(userService).clearUserRfid(testOpenId);

    ResponseEntity<Map<String, String>> response = controller.clearUserRfid(testOpenId);

    verify(userService).clearUserRfid(testOpenId);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("success", response.getBody().get("type"));
    assertEquals("User RFID cleared successfully", response.getBody().get("message"));
  }

  /** Tests clearing RFID when the user does not exist. */
  @Test
  void clearUserRfid_UserNotFound_Returns404() {
    doThrow(new NoSuchElementException("User not found: " + testOpenId))
        .when(userService)
        .clearUserRfid(testOpenId);

    ResponseEntity<Map<String, String>> response = controller.clearUserRfid(testOpenId);

    assertEquals(404, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertEquals("User not found: " + testOpenId, response.getBody().get("message"));
  }

  /** Tests generic failure when clearing RFID. */
  @Test
  void clearUserRfid_GenericException_Returns500() {
    doThrow(new RuntimeException("DB down")).when(userService).clearUserRfid(testOpenId);

    ResponseEntity<Map<String, String>> response = controller.clearUserRfid(testOpenId);

    assertEquals(500, response.getStatusCode().value());
    assertEquals("error", response.getBody().get("type"));
    assertEquals("Unexpected server error", response.getBody().get("message"));
  }
}
