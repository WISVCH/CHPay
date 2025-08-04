package chpay.frontend.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import chpay.shared.service.NotificationService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class NotificationTestPageControllerTest {

  @Mock private NotificationService notificationService;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private NotificationTestPageController notificationTestPageController;

  @Test
  void returnsViewTest() {
    String viewName = notificationTestPageController.notificationTest(redirectAttributes);
    assertEquals("notification-test", viewName);
  }

  @Test
  void returnsSuccessNotificationTest() {
    Map<String, String> expectedPayload = new HashMap<>();
    expectedPayload.put("type", "success");
    expectedPayload.put("message", "Positive notification");

    Map<String, String> actualPayload = notificationTestPageController.showGreenNotification();
    assertEquals(expectedPayload, actualPayload);
  }

  @Test
  void returnsInfoTest() {
    Map<String, String> expectedPayload = new HashMap<>();
    expectedPayload.put("type", "message");
    expectedPayload.put("message", "Gray notification");

    Map<String, String> actualPayload = notificationTestPageController.showGrayNotification();
    assertEquals(expectedPayload, actualPayload);
  }

  @Test
  void returnsErrorTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          notificationTestPageController.showRedNotification();
        });
  }

  @Test
  void returnsBalanceErrorTest() {
    assertThrows(
        InsufficientBalanceException.class,
        () -> {
          notificationTestPageController.throwBalanceError();
        });
  }

  @Test
  void notificaitonRedirectTest() {
    String viewName =
        notificationTestPageController.redirectWithSuccessNotification(redirectAttributes);
    assertEquals("redirect:/notification-test", viewName);
    verify(notificationService)
        .addSuccessMessage(eq(redirectAttributes), eq("Success notification after redirect"));
  }

  @Test
  void infoRedirectTest() {
    String viewName =
        notificationTestPageController.redirectWithInfoNotification(redirectAttributes);
    assertEquals("redirect:/notification-test", viewName);
    verify(notificationService)
        .addInfoMessage(eq(redirectAttributes), eq("Info notification after redirect"));
  }

  @Test
  void errorRedirectTest() {
    String viewName =
        notificationTestPageController.redirectWithErrorNotification(redirectAttributes);
    assertEquals("redirect:/notification-test", viewName);
    verify(notificationService)
        .addErrorMessage(eq(redirectAttributes), eq("Error notification after redirect"));
  }
}
