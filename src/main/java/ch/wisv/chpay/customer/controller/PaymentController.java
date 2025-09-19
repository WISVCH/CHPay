package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.api.external_payment.service.ExternalPaymentServiceImpl;
import ch.wisv.chpay.core.controller.PageController;
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
import javassist.NotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;
import java.util.UUID;

@Controller
@PreAuthorize("hasRole('USER') and !hasRole('BANNED')")
@RequestMapping("/payment")
public class PaymentController extends PageController {
  /** Model attr of the Transaction. */
  private static final String MODEL_ATTR_TX = MODEL_ATTR_TRANSACTION;

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
            ExternalPaymentServiceImpl externalPaymentServiceImpl
          ) {
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
  @GetMapping(value = "/transaction/{tx}")
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
  @PreAuthorize("hasRole('USER') and !hasRole('BANNED')")
  @GetMapping(value = "pay")
  public String getPage(
          Model model, @RequestParam(name = "tx") String tx, RedirectAttributes redirectAttributes) {
    Transaction transaction =
            transactionService
                    .getTransactionById(UUID.fromString(tx))
                    .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
    if (transaction.getType().equals(Transaction.TransactionType.EXTERNAL_PAYMENT)) {
      return "redirect:/payment/externalcomplete/" + transaction.getId();
    }

    transactionService.fullfillTransaction(
            transaction.getId(), (User) model.getAttribute("currentUser"));

    notificationService.addSuccessMessage(redirectAttributes, "Authorized Transaction");
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
          @PathVariable String key, RedirectAttributes redirectAttributes, Model model)
          throws NotFoundException {
    Transaction t =
            transactionRepository
                    .findById(UUID.fromString(key))
                    .orElseThrow(() -> new NotFoundException(key));
    model.addAttribute(MODEL_ATTR_TRANSACTION_ID, key);
    return switch (t.getStatus()) {
      case Transaction.TransactionStatus.PENDING -> "pending";
      case Transaction.TransactionStatus.SUCCESSFUL -> "successful";
      case Transaction.TransactionStatus.FAILED -> "failed";
      default -> "error";
    };
  }

  /**
   * Processes an external transaction, coming from events by fulfilling it and redirecting the user
   * back to the events payment complete page, by calling {@code
   * externalPaymentServiceImpl.postToWebhook()}. Errors during the processing lead to a redirect to
   * the fallback URL, currently the error page.
   *
   * @param id The id of the external transaction to process.
   * @param model the model holding attributes for the current HTTP session
   * @return A redirect string to the events payment complete page.
   * @throws RestClientException If an error occurs while calling {@code
   *     externalPaymentServiceImpl.postToWebhook()}.
   */
  @PreAuthorize("hasRole('USER') and !hasRole('BANNED')")
  @GetMapping("/externalcomplete/{id}")
  public String completeExternalTransaction(@PathVariable String id, Model model)
          throws RestClientException {
    ExternalTransaction transaction =
            (ExternalTransaction)
                    transactionRepository
                            .findById(UUID.fromString(id))
                            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

    if (transaction.getStatus() != ExternalTransaction.TransactionStatus.PENDING) {
      return "redirect:" + transaction.getFallbackUrl(); // already paid, just redirect to events.
    }

    User currentUser = (User) model.getAttribute("currentUser");

    // If transaction doesn't have a user (anonymous), link the current user to it
    if (transaction.getUser() == null) {
      transaction.linkUser(currentUser);
      transactionRepository.save(transaction);
    }

    transactionService.fullfillExternalTransaction(transaction.getId(), transaction.getUser());

    return externalPaymentServiceImpl.postToWebhook(id, transaction);
  }
}
