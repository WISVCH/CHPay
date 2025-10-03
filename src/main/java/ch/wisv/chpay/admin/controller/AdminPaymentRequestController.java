package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminPaymentRequestService;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.service.NotificationService;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(value = "/admin/payment-request")
public class AdminPaymentRequestController extends AdminController {
  private final AdminPaymentRequestService adminPaymentRequestService;
  private final NotificationService notificationService;
  
  @Value("${spring.application.baseurl}")
  private String baseUrl;

  @Autowired
  protected AdminPaymentRequestController(
      AdminPaymentRequestService adminPaymentRequestService,
      NotificationService notificationService) {
    super();
    this.adminPaymentRequestService = adminPaymentRequestService;
    this.notificationService = notificationService;
  }

  @GetMapping(value = "/{tx}/expire")
  public String expirePaymentRequest(
      Model model, @PathVariable String tx, RedirectAttributes redirectAttributes) {
    PaymentRequest paymentRequest =
        adminPaymentRequestService
            .getById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Payment request not found"));
    if (paymentRequest == null) {
      throw new NoSuchElementException("Payment request not found");
    }

    adminPaymentRequestService.expireAndSave(paymentRequest);

    return "redirect:/admin/payment-request/" + paymentRequest.getRequest_id().toString();
  }

  /**
   * Displays the payment request page based on its ID for the administrator user.
   *
   * @param model the Model object to add attributes for the view
   * @param tx the unique identifier of the payment request in String format
   * @param redirectAttributes the RedirectAttributes object used for passing flash attributes
   * @return the name of the view to render the payment request page
   */
  @GetMapping(value = "/{tx}")
  public String showPaymentRequestPage(
      Model model, @PathVariable String tx, RedirectAttributes redirectAttributes) {

    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminPaymentRequests");
    
    // Get payment request object
    PaymentRequest paymentRequest =
        adminPaymentRequestService
            .getById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Payment request not found"));
    if (paymentRequest == null) {
      throw new NoSuchElementException("Payment request not found");
    }

    // Add attributes to the model
    model.addAttribute(MODEL_ATTR_PAYMENT_REQUEST, paymentRequest);
    model.addAttribute(MODEL_ATTR_BASE_URL, baseUrl);
    
    return "admin-payment-request";
  }
}
