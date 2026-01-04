package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.model.PaymentRequestMonthlyStats;
import ch.wisv.chpay.admin.service.AdminPaymentRequestService;
import ch.wisv.chpay.core.model.PaymentRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(value = "/admin/payment-request")
public class AdminPaymentRequestController extends AdminController {
  private final AdminPaymentRequestService adminPaymentRequestService;

  @Value("${spring.application.baseurl}")
  private String baseUrl;

  @Autowired
  protected AdminPaymentRequestController(AdminPaymentRequestService adminPaymentRequestService) {
    super();
    this.adminPaymentRequestService = adminPaymentRequestService;
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

    adminPaymentRequestService.expireNow(paymentRequest);

    return "redirect:/admin/payment-request/" + paymentRequest.getRequest_id().toString();
  }

  @PostMapping(value = "/{tx}/expire-date")
  public String updateExpireDate(
      Model model,
      @PathVariable String tx,
      @RequestParam("expireDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate expireDate,
      RedirectAttributes redirectAttributes) {

    PaymentRequest paymentRequest =
        adminPaymentRequestService
            .getById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Payment request not found"));
    if (paymentRequest == null) {
      throw new NoSuchElementException("Payment request not found");
    }

    if (paymentRequest.isExpired()) {
      throw new IllegalStateException("Request has expired");
    }

    if (expireDate == null || !expireDate.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("Expire date must be in the future.");
    }

    adminPaymentRequestService.updateExpireDate(paymentRequest, expireDate);

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

  @GetMapping(value = "/{tx}/stats")
  public String showPaymentRequestStatsPage(
      Model model, @PathVariable String tx, RedirectAttributes redirectAttributes) {

    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminPaymentRequests");

    PaymentRequest paymentRequest =
        adminPaymentRequestService
            .getById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Payment request not found"));
    if (paymentRequest == null) {
      throw new NoSuchElementException("Payment request not found");
    }

    List<PaymentRequestMonthlyStats> monthlyStats =
        adminPaymentRequestService.getFulfilmentsByMonth(paymentRequest.getRequest_id());
    long monthlyStatsTotal =
        monthlyStats.stream().mapToLong(PaymentRequestMonthlyStats::fulfilments).sum();

    model.addAttribute(MODEL_ATTR_PAYMENT_REQUEST, paymentRequest);
    model.addAttribute("monthlyStats", monthlyStats);
    model.addAttribute("monthlyStatsTotal", monthlyStatsTotal);

    return "admin-payment-request-stats";
  }
}
