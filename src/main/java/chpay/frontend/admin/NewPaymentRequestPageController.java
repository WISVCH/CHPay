package chpay.frontend.admin;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.paymentbackend.service.RequestService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class NewPaymentRequestPageController {

  private final RequestService requestService;

  @Autowired
  public NewPaymentRequestPageController(RequestService requestService) {
    this.requestService = requestService;
  }

  /**
   * @return the create-payment-request.html template
   */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin/createPaymentRequest")
  public String showNewPaymentRequestPageController(Model model) {
    model.addAttribute("urlPage", "createPaymentRequest");
    return "create-payment-request";
  }

  /**
   * Handles the creation of a new payment request and redirects to a QR code page for the request.
   *
   * @param description The description of the payment request.
   * @param amount The amount to be paid.
   * @param multiUse Indicates whether the payment request can be used multiple times. Default is
   *     false.
   * @return A redirect URL to the page displaying the QR code for the generated payment request.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/admin/createPaymentRequest")
  public String createPaymentRequest(
      @RequestParam("description") String description,
      @RequestParam("amount") BigDecimal amount,
      @RequestParam(value = "multiUse", required = false, defaultValue = "false")
          boolean multiUse) {

    PaymentRequest paymentRequest = requestService.createRequest(amount, description, multiUse);

    return "redirect:/qr/" + paymentRequest.getRequest_id();
  }
}
