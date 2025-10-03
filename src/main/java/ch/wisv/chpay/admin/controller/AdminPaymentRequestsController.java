package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminPaymentRequestService;
import ch.wisv.chpay.core.model.PaymentRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/payment-requests")
public class AdminPaymentRequestsController extends AdminController {

  private final AdminPaymentRequestService adminPaymentRequestService;
  
  @Value("${spring.application.baseurl}")
  private String baseUrl;

  @Autowired
  protected AdminPaymentRequestsController(AdminPaymentRequestService adminPaymentRequestService) {
    super();
    this.adminPaymentRequestService = adminPaymentRequestService;
  }

  /**
   * Gets the page showing all transactions for a given year and month.
   *
   * @param model of type Model
   * @return String
   */
  @GetMapping
  public String getPage(
      RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {

    // Get all payment requests
    List<PaymentRequest> paymentRequests = adminPaymentRequestService.getAll();

    // Add attributes to the model
    model.addAttribute(MODEL_ATTR_PAYMENT_REQUESTS, paymentRequests);
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminPaymentRequests");
    model.addAttribute(MODEL_ATTR_BASE_URL, baseUrl);

    return "admin-payment-request-table";
  }
}
