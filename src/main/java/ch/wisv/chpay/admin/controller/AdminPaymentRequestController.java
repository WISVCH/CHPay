package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminPaymentRequestService;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.service.NotificationService;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
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
   * Displays the transaction page based on its ID for the administrator user.
   *
   * @param model the Model object to add attributes for the view
   * @param tx the unique identifier of the transaction in String format
   * @param redirectAttributes the RedirectAttributes object used for passing flash attributes
   * @return the name of the view to render the user page
   */
  @GetMapping(value = "/{tx}")
  public String showTransactionPage(
      Model model, @PathVariable String tx, RedirectAttributes redirectAttributes) {

    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminPaymentRequests");
    // get transaction object
    PaymentRequest paymentRequest =
        adminPaymentRequestService
            .getById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Payment request not found"));
    if (paymentRequest == null) {
      throw new NoSuchElementException("Payment request not found");
    }
    // check if the transaction is refund and add the id of the refund associated for navigation
    //    String refundId = null;
    //    String requestId = null;
    //    if (transaction.getClass() == RefundTransaction.class) {
    //      refundId = ((RefundTransaction) transaction).getRefundOf().getId().toString();
    //    }

    // check for payment transaction and get the request id if present
    //    if (transaction.getClass() == PaymentTransaction.class) {
    //      requestId = ((PaymentTransaction) transaction).getRequest().getRequest_id().toString();
    //    }

    model.addAttribute(MODEL_ATTR_PAYMENT_REQUEST, paymentRequest);
    //    model.addAttribute(MODEL_ATTR_REFUND_ID, refundId);
    //    model.addAttribute(MODEL_ATTR_REQUEST_ID, requestId);
    //    model.addAttribute(
    //        MODEL_ATTR_REFUND_POSSIBLE,
    // transactionService.getNonRefundedAmount(transaction.getId()));
    return "admin-payment-request";
  }
}
