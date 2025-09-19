package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.core.model.transaction.PaymentTransaction;
import ch.wisv.chpay.core.model.transaction.RefundTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(value = "/admin/transaction")
public class AdminTransactionController extends AdminController {
  private final TransactionService transactionService;
  private final NotificationService notificationService;

  @Autowired
  protected AdminTransactionController(
      TransactionService transactionService, NotificationService notificationService) {
    super();
    this.transactionService = transactionService;
    this.notificationService = notificationService;
  }

  @GetMapping(value = "/{tx}/refund")
  public String processRefund(
      Model model,
      @PathVariable String tx,
      @RequestParam(required = false) String amount,
      RedirectAttributes redirectAttributes) {
    Transaction transaction =
        transactionService
            .getTransactionById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
    if (transaction == null) {
      throw new NoSuchElementException("Transaction not found");
    }
    Transaction refund;
    if (amount == null) {
      refund = transactionService.refundTransaction(transaction.getId());
    } else {
      refund = transactionService.partialRefund(transaction.getId(), new BigDecimal(amount));
    }
    return "redirect:/admin/transaction/" + refund.getId().toString();
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

    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminTransactions");
    // get transaction object
    Transaction transaction =
        transactionService
            .getTransactionById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
    if (transaction == null) {
      throw new NoSuchElementException("Transaction not found");
    }
    // check if the transaction is refund and add the id of the refund associated for navigation
    String refundId = null;
    String requestId = null;
    if (transaction.getClass() == RefundTransaction.class) {
      refundId = ((RefundTransaction) transaction).getRefundOf().getId().toString();
    }

    // check for payment transaction and get the request id if present
    if (transaction.getClass() == PaymentTransaction.class) {
      requestId = ((PaymentTransaction) transaction).getRequest().getRequest_id().toString();
    }

    model.addAttribute(MODEL_ATTR_TRANSACTION, transaction);
    model.addAttribute(MODEL_ATTR_REFUND_ID, refundId);
    model.addAttribute(MODEL_ATTR_REQUEST_ID, requestId);
    model.addAttribute(
        MODEL_ATTR_REFUND_POSSIBLE, transactionService.getNonRefundedAmount(transaction.getId()));
    return "admin-transaction";
  }
}
