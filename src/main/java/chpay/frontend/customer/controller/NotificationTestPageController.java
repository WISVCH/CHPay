package chpay.frontend.customer.controller;

import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import chpay.shared.service.NotificationService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationTestPageController extends PageController {
  @Autowired private NotificationService notificationService;

  /**
   * Return notification-test.html
   *
   * @param redirectAttributes
   * @return
   */
  @GetMapping("/notification-test")
  public String notificationTest(RedirectAttributes redirectAttributes) {
    return "notification-test";
  }

  /**
   * Displays a green success notification.
   *
   * @return success notification payload
   */
  @GetMapping("/api/show-green-notification")
  @ResponseBody
  public Map<String, String> showGreenNotification() {
    return (Map<String, String>)
        NotificationService.createSuccessNotification("Positive notification");
  }

  /**
   * Displays a gray regular notification.
   *
   * @return info notification payload
   */
  @GetMapping("/api/show-gray-notification")
  @ResponseBody
  public Map<String, String> showGrayNotification() {
    return (Map<String, String>) NotificationService.createInfoNotification("Gray notification");
  }

  /**
   * Displays a red error notification.
   *
   * @return error notification payload
   */
  @GetMapping("/api/show-red-notification")
  @ResponseBody
  public Map<String, String> showRedNotification() {
    throw new RuntimeException("This is a test exception from red notification");
  }

  @GetMapping("/api/throw-balance-error")
  @ResponseBody
  public Map<String, String> throwBalanceError() {
    throw new InsufficientBalanceException("You need at least €5.00 in your wallet.");
  }

  /**
   * Example of using redirect with success notification.
   *
   * @param redirectAttributes to add flash attributes for notification
   * @return redirect to notification test page
   */
  @GetMapping("/redirect-success-notification")
  public String redirectWithSuccessNotification(RedirectAttributes redirectAttributes) {
    notificationService.addSuccessMessage(
        redirectAttributes, "Success notification after redirect");
    return "redirect:/notification-test";
  }

  /**
   * Example of using redirect with info notification.
   *
   * @param redirectAttributes to add flash attributes for notification
   * @return redirect to notification test page
   */
  @GetMapping("/redirect-info-notification")
  public String redirectWithInfoNotification(RedirectAttributes redirectAttributes) {
    notificationService.addInfoMessage(redirectAttributes, "Info notification after redirect");
    return "redirect:/notification-test";
  }

  /**
   * Example of using redirect with error notification.
   *
   * @param redirectAttributes to add flash attributes for notification
   * @return redirect to notification test page
   */
  @GetMapping("/redirect-error-notification")
  public String redirectWithErrorNotification(RedirectAttributes redirectAttributes) {
    notificationService.addErrorMessage(redirectAttributes, "Error notification after redirect");
    return "redirect:/notification-test";
  }
}
