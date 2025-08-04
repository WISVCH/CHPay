package chpay.frontend.customer.controller;

import chpay.DatabaseHandler.Exceptions.TransactionAlreadyFulfilled;
import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.paymentbackend.service.RequestService;
import chpay.paymentbackend.service.TransactionService;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentPageController extends PageController {
  /** Model attr of the Transaction. */
  private static final String MODEL_ATTR_TX = "transaction";

  private final RequestService requestService;
  private final TransactionService transactionService;

  protected PaymentPageController(
      RequestService requestService, TransactionService transactionService) {
    super();
    this.requestService = requestService;
    this.transactionService = transactionService;
  }

  /**
   * Creates a pending payment transaction and redirects the user to the payment transaction page
   * for the generated transaction.
   *
   * @param model the Model object used to add attributes for rendering the view
   * @param key the unique identifier of the payment request in String format
   * @param redirectAttributes the RedirectAttributes object used for passing flash attributes
   *     between redirects
   * @return a redirect URL to the payment transaction page associated with the specified payment
   *     request
   */
  @GetMapping(value = "/payment/{key}")
  public String redirectToPayTransaction(
      Model model, @PathVariable String key, RedirectAttributes redirectAttributes) {
    PaymentRequest paymentRequest =
        requestService
            .getRequestById(UUID.fromString(key))
            .orElseThrow(() -> new NoSuchElementException("Request not found"));

    if (paymentRequest.isFulfilled()) {
      throw new IllegalStateException("Request is already fulfilled!");
    }

    User user = (User) model.getAttribute("currentUser");
    PaymentTransaction tx = requestService.transactionFromRequest(UUID.fromString(key), user);

    model.addAttribute(MODEL_ATTR_TX, tx);
    return "redirect:/payment/transaction/" + tx.getId();
  }

  /**
   * Displays the payment page for a specific transaction based on its ID.
   *
   * @param model the Model object to add attributes for the view
   * @param tx the unique identifier of the transaction in String format
   * @param redirectAttributes the RedirectAttributes object used for passing flash attributes
   * @return the name of the view to render the payment page
   */
  @GetMapping(value = "/payment/transaction/{tx}")
  public String showPaymentPage(
      Model model, @PathVariable String tx, RedirectAttributes redirectAttributes) {

    Transaction transaction =
        transactionService
            .getTransactionById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

    if (transaction.getStatus().equals(Transaction.TransactionStatus.FAILED)
        || transaction.getStatus().equals(Transaction.TransactionStatus.SUCCESSFUL)) {
      throw new TransactionAlreadyFulfilled("This payment has already been fulfilled, or failed.");
    }

    model.addAttribute("transaction", transaction);
    model.addAttribute(MODEL_ATTR_URL_PAGE, "payment");
    return "payment";
  }
}
