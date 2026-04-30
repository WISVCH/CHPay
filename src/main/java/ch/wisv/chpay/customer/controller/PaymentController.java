package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.api.external_payment.service.ExternalPaymentServiceImpl;
import ch.wisv.chpay.core.exception.TransactionAlreadyFulfilled;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.ExternalTransaction;
import ch.wisv.chpay.core.model.transaction.PaymentTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.RequestService;
import ch.wisv.chpay.core.service.TransactionService;
import java.util.NoSuchElementException;
import java.util.UUID;
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
@PreAuthorize("hasRole('USER') and !hasRole('BANNED')")
@RequestMapping("/payment")
public class PaymentController extends CustomerController {
  /** Model attr of the Transaction. */
  private static final String MODEL_ATTR_TX = MODEL_ATTR_TRANSACTION;

  private enum PaymentAction {
    PAY,
    CANCEL
  }

  private final RequestService requestService;
  private final TransactionService transactionService;
  private final NotificationService notificationService;
  private final TransactionRepository transactionRepository;
  private final ExternalPaymentServiceImpl externalPaymentServiceImpl;

  protected PaymentController(
      RequestService requestService,
      TransactionService transactionService,
      NotificationService notificationService,
      TransactionRepository transactionRepository,
      ExternalPaymentServiceImpl externalPaymentServiceImpl) {
    super();
    this.requestService = requestService;
    this.transactionService = transactionService;
    this.notificationService = notificationService;
    this.transactionRepository = transactionRepository;
    this.externalPaymentServiceImpl = externalPaymentServiceImpl;
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
  @GetMapping(value = "/request/{key}")
  public String redirectToPayTransaction(
      Model model, @PathVariable String key, RedirectAttributes redirectAttributes) {
    PaymentRequest paymentRequest =
        requestService
            .getRequestById(UUID.fromString(key))
            .orElseThrow(() -> new NoSuchElementException("Request not found"));

    if (paymentRequest.isExpired()) {
      throw new IllegalStateException("Request has expired");
    }

    if (paymentRequest.getFulfilments() > 0 && !paymentRequest.isMultiUse()) {
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
  @GetMapping(value = "/transaction/{tx}")
  public String showPaymentPage(
      Model model, @PathVariable String tx, RedirectAttributes redirectAttributes) {

    Transaction transaction =
        transactionService
            .getTransactionById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
    User currentUser = (User) model.getAttribute("currentUser");
    if (!isClaimableExternalCheckout(transaction)) {
      assertCurrentUserOwnsTransaction(currentUser, transaction);
    }

    if (transaction.getStatus().equals(Transaction.TransactionStatus.FAILED)
        || transaction.getStatus().equals(Transaction.TransactionStatus.CANCELLED)
        || transaction.getStatus().equals(Transaction.TransactionStatus.SUCCESSFUL)) {
      throw new TransactionAlreadyFulfilled("This payment has already been fulfilled, or failed.");
    }

    model.addAttribute(MODEL_ATTR_TRANSACTION, transaction);
    model.addAttribute(MODEL_ATTR_URL_PAGE, "payment");
    return "payment";
  }

  /**
   * Processes a payment transaction by fulfilling the specified transaction and redirecting the
   * user to the main page with a success notification.
   *
   * @param model the model holding attributes for the current HTTP session
   * @param tx the unique transaction identifier provided as a request parameter
   * @param redirectAttributes attributes used to pass temporary data during a redirect
   * @return a redirect string to the main index page after processing the transaction
   */
  @PostMapping(value = "pay")
  public String processPayment(
      Model model, @RequestParam(name = "tx") String tx, RedirectAttributes redirectAttributes) {
    Transaction transaction =
        transactionService
            .getTransactionById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
    User currentUser = (User) model.getAttribute("currentUser");
    if (transaction.isExternalPayment()) {
      return handleExternalPaymentAction(
          tx, (ExternalTransaction) transaction, currentUser, PaymentAction.PAY);
    }
    return handleInternalPaymentAction(
        tx, transaction, currentUser, redirectAttributes, PaymentAction.PAY);
  }

  @PostMapping(value = "cancel")
  public String cancelPayment(
      Model model, @RequestParam(name = "tx") String tx, RedirectAttributes redirectAttributes) {
    Transaction transaction =
        transactionService
            .getTransactionById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
    User currentUser = (User) model.getAttribute("currentUser");

    if (transaction.isExternalPayment()) {
      return handleExternalPaymentAction(
          tx, (ExternalTransaction) transaction, currentUser, PaymentAction.CANCEL);
    }
    return handleInternalPaymentAction(
        tx, transaction, currentUser, redirectAttributes, PaymentAction.CANCEL);
  }

  private String handleExternalPaymentAction(
      String tx, ExternalTransaction transaction, User currentUser, PaymentAction action) {
    if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
      return "redirect:" + transaction.getFallbackUrl();
    }

    if (isClaimableExternalCheckout(transaction)) {
      transaction.setUser(currentUser);
      transaction = transactionRepository.save(transaction);
    }
    assertCurrentUserOwnsTransaction(currentUser, transaction);

    if (action == PaymentAction.PAY) {
      transactionService.fullfillExternalTransaction(transaction.getId(), currentUser);
    } else {
      transactionService.cancelTransaction(transaction.getId(), currentUser);
    }
    return externalPaymentServiceImpl.postToWebhook(tx, transaction);
  }

  private String handleInternalPaymentAction(
      String tx,
      Transaction transaction,
      User currentUser,
      RedirectAttributes redirectAttributes,
      PaymentAction action) {
    assertCurrentUserOwnsTransaction(currentUser, transaction);
    if (action == PaymentAction.PAY) {
      transactionService.fullfillTransaction(transaction.getId(), currentUser);
      notificationService.addSuccessMessage(redirectAttributes, "Authorized Transaction");
    } else {
      transactionService.cancelTransaction(transaction.getId(), currentUser);
      notificationService.addSuccessMessage(redirectAttributes, "Cancelled Transaction");
    }
    return "redirect:/payment/complete/" + tx;
  }

  /**
   * Redirects the user to a payment status page
   *
   * @param key transaction's id
   * @return either a purgatory page, a success or fail page
   */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping("/complete/{key}")
  public String depositSuccess(
      @PathVariable String key, RedirectAttributes redirectAttributes, Model model) {
    Transaction t =
        transactionRepository
            .findById(UUID.fromString(key))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + key));
    User currentUser = (User) model.getAttribute("currentUser");
    assertCurrentUserOwnsTransaction(currentUser, t);
    model.addAttribute(MODEL_ATTR_TRANSACTION_ID, key);
    model.addAttribute("isTopup", t.getType().equals(Transaction.TransactionType.TOP_UP));
    if (t.supportsRequest() && t.getRequest() != null) {
      model.addAttribute("paymentRequest", t.getRequest());
    }
    return switch (t.getStatus()) {
      case Transaction.TransactionStatus.PENDING -> "pending";
      case Transaction.TransactionStatus.SUCCESSFUL -> "successful";
      case Transaction.TransactionStatus.FAILED -> "failed";
      case Transaction.TransactionStatus.CANCELLED -> "cancelled";
      default -> "error";
    };
  }

  private boolean isClaimableExternalCheckout(Transaction transaction) {
    return transaction.isExternalPayment()
        && transaction.getUser() == null
        && transaction.getStatus() == Transaction.TransactionStatus.PENDING;
  }
}
