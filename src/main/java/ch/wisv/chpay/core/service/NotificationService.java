package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.dto.NotificationPayload;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Service
public class NotificationService {

  private static final String NOTIFICATION_TYPE = "notificationType";
  private static final String NOTIFICATION_MESSAGE = "notificationMessage";

  /** Add a success notification to be displayed after redirect */
  public void addSuccessMessage(RedirectAttributes redirectAttributes, String message) {
    redirectAttributes.addFlashAttribute(NOTIFICATION_TYPE, "success");
    redirectAttributes.addFlashAttribute(NOTIFICATION_MESSAGE, message);
  }

  /** Add an info notification to be displayed after redirect */
  public void addInfoMessage(RedirectAttributes redirectAttributes, String message) {
    redirectAttributes.addFlashAttribute(NOTIFICATION_TYPE, "message");
    redirectAttributes.addFlashAttribute(NOTIFICATION_MESSAGE, message);
  }

  /** Add an error notification to be displayed after redirect */
  public void addErrorMessage(RedirectAttributes redirectAttributes, String message) {
    redirectAttributes.addFlashAttribute(NOTIFICATION_TYPE, "error");
    redirectAttributes.addFlashAttribute(NOTIFICATION_MESSAGE, message);
  }

  /** Create a success notification payload for AJAX responses */
  public static Object createSuccessNotification(String message) {
    return NotificationPayload.success(message);
  }

  /** Create an info notification payload for AJAX responses */
  public static Object createInfoNotification(String message) {
    return NotificationPayload.info(message);
  }

  /** Create an error notification payload for AJAX responses */
  public static Object createErrorNotification(String message) {
    return NotificationPayload.error(message);
  }
}
