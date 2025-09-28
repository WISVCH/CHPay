package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminPaymentRequestService;
import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.transaction.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
@RequestMapping("/admin/payment-request/{tx}/transactions")
public class AdminPaymentRequestTransactionsController extends AdminController {

  private final AdminTransactionService adminTransactionService;
  private final AdminPaymentRequestService adminPaymentRequestService;

  @Autowired
  protected AdminPaymentRequestTransactionsController(
      AdminTransactionService adminTransactionService,
      AdminPaymentRequestService adminPaymentRequestService) {
    super();
    this.adminTransactionService = adminTransactionService;
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
      RedirectAttributes redirectAttributes,
      Model model,
      HttpServletRequest request,
      @PathVariable String tx) {

    // Get all transactions for the specified month
    List<Transaction> transactions =
        adminTransactionService.getTransactionsByRequestId(UUID.fromString(tx));

    PaymentRequest paymentRequest =
        adminPaymentRequestService.getById(UUID.fromString(tx)).orElse(null);

    if (paymentRequest == null) {
      throw new IllegalArgumentException("Payment request not found");
    }
    // Add attributes to the model
    model.addAttribute(MODEL_ATTR_TRANSACTIONS, transactions);
    model.addAttribute(MODEL_ATTR_PAYMENT_REQUEST, paymentRequest);

    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminPaymentRequestTransactions");

    return "admin-payment-request-transaction-table";
  }
}
