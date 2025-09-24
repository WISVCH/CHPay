package ch.wisv.chpay.core.service;

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
}
