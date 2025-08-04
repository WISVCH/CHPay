package chpay.frontend.customer.controller;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.ExternalTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.frontend.events.service.ExternalPaymentServiceImpl;
import chpay.paymentbackend.service.TransactionService;
import chpay.shared.service.NotificationService;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PayRequestPageController extends PageController {
  private final TransactionService transactionService;
  private final TransactionRepository transactionRepository;
  private final NotificationService notificationService;
  private final ExternalPaymentServiceImpl externalPaymentServiceImpl;

  protected PayRequestPageController(
      TransactionService transactionService,
      TransactionRepository transactionRepository,
      NotificationService notificationService,
      ExternalPaymentServiceImpl externalPaymentServiceImpl) {
    super();
    this.transactionService = transactionService;
    this.transactionRepository = transactionRepository;
    this.notificationService = notificationService;
    this.externalPaymentServiceImpl = externalPaymentServiceImpl;
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
  @GetMapping(value = "payTransaction")
  public String getPage(
      Model model, @RequestParam(name = "tx") String tx, RedirectAttributes redirectAttributes) {
    Transaction transaction =
        transactionService
            .getTransactionById(UUID.fromString(tx))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
    if (transaction.getType().equals(Transaction.TransactionType.EXTERNAL_PAYMENT)) {
      return "redirect:/external/" + transaction.getId();
    }

    transactionService.fullfillTransaction(
        transaction.getId(), (User) model.getAttribute("currentUser"));

    notificationService.addSuccessMessage(redirectAttributes, "Authorized Transaction");
    return "redirect:/payment/complete/" + tx;
  }

  /**
   * Processes an external transaction, coming from events by fulfilling it and redirecting the user
   * back to the events payment complete page, by calling {@code
   * externalPaymentServiceImpl.postToWebhook()}. Errors during the processing lead to a redirect to
   * the fallback URL, currently the error page.
   *
   * @param id The id of the external transaction to process.
   * @return A redirect string to the events payment complete page.
   * @throws RestClientException If an error occurs while calling {@code
   *     externalPaymentServiceImpl.postToWebhook()}.
   */
  @GetMapping("/external/{id}")
  public String completeExternalTransaction(@PathVariable String id) throws RestClientException {
    ExternalTransaction transaction =
        (ExternalTransaction)
            transactionRepository
                .findById(UUID.fromString(id))
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));

    if (transaction.getStatus() != ExternalTransaction.TransactionStatus.PENDING) {
      return "redirect:" + transaction.getFallbackUrl(); // already paid, just redirect to events.
    }

    transactionService.fullfillExternalTransaction(transaction.getId(), transaction.getUser());

    return externalPaymentServiceImpl.postToWebhook(id, transaction);
  }
}
