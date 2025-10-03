package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminPaymentRequestService;
import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.transaction.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/payment-request/{tx}/transactions")
public class AdminPaymentRequestTransactionsController extends BaseTransactionController {

  private final AdminPaymentRequestService adminPaymentRequestService;

  @Autowired
  protected AdminPaymentRequestTransactionsController(
      AdminTransactionService adminTransactionService,
      AdminPaymentRequestService adminPaymentRequestService) {
    super(adminTransactionService);
    this.adminPaymentRequestService = adminPaymentRequestService;
  }

  /**
   * Gets the page showing all transactions for a given year and month.
   *
   * @param model of type Model
   * @param tx the payment request ID
   * @param yearMonth the yearMonth parameter in format "YYYY-MM" (optional)
   * @return String
   */
  @GetMapping
  public String getPage(
      RedirectAttributes redirectAttributes,
      Model model,
      HttpServletRequest request,
      @PathVariable String tx,
      @RequestParam(required = false) String yearMonth) {

    UUID requestUuid = UUID.fromString(tx);

    PaymentRequest paymentRequest = adminPaymentRequestService.getById(requestUuid).orElse(null);

    if (paymentRequest == null) {
      throw new IllegalArgumentException("Payment request not found");
    }

    try {
      YearMonth selectedYearMonth =
          handleYearMonthParameter(
              yearMonth,
              request,
              () -> adminTransactionService.getMostRecentYearMonthForRequest(requestUuid),
              ym -> "/admin/payment-request/" + tx + "/transactions?yearMonth=" + ym);

      // Get all transactions for the specified payment request and month
      List<Transaction> transactions =
          adminTransactionService.getTransactionsByRequestIdAndYearMonth(
              requestUuid, selectedYearMonth);

      // Get all possible months for the dropdown
      List<YearMonth> allPossibleMonths =
          adminTransactionService.getAllPossibleMonthsForRequest(requestUuid);

      // Add attributes to the model
      addTransactionModelAttributes(model, transactions, selectedYearMonth, allPossibleMonths);
      model.addAttribute(MODEL_ATTR_PAYMENT_REQUEST, paymentRequest);
      model.addAttribute(MODEL_ATTR_TRANSACTION_PAGE_TYPE, "paymentRequest");
      model.addAttribute(MODEL_ATTR_BREADCRUMB_REQUEST_ID, tx);
      model.addAttribute(
          MODEL_ATTR_BREADCRUMB_REQUEST_DESCRIPTION, paymentRequest.getDescription());

      model.addAttribute(MODEL_ATTR_URL_PAGE, "adminPaymentRequests");

      return "admin-transaction-table";
    } catch (BaseTransactionController.RedirectException e) {
      return "redirect:" + e.getRedirectUrl();
    }
  }
}
