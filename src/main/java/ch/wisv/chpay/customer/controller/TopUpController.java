package ch.wisv.chpay.customer.controller;

import static ch.wisv.chpay.core.model.transaction.TopupTransaction.createTopUpTransaction;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.TopupTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.SettingService;
import ch.wisv.chpay.core.service.TransactionService;
import ch.wisv.chpay.customer.service.DepositService;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/topup")
public class TopUpController extends CustomerController {
  private final DepositService depositService;
  private final TransactionService transactionsService;
  private final NotificationService notificationService;
  private final TransactionRepository transactionRepository;
  private final SettingService settingService;

  @Value("${mollie.transaction_fee}")
  private String transactionFee;

  @Autowired
  public TopUpController(
      DepositService depositService,
      TransactionService transactionsService,
      NotificationService notificationService,
      TransactionRepository transactionRepository,
      SettingService settingService) {
    this.depositService = depositService;
    this.transactionsService = transactionsService;
    this.notificationService = notificationService;
    this.transactionRepository = transactionRepository;
    this.settingService = settingService;
  }

  /**
   * Serves the topup page at /topup.html URL.
   *
   * @return String view name for topup template
   */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping
  public String showBalancePage(
      @ModelAttribute("currentUser") User currentUser,
      @RequestParam(required = false) String redirect,
      Model model) {
    // add the signature of the current page to thymeleaf context
    model.addAttribute(MODEL_ATTR_URL_PAGE, "topup");
    model.addAttribute(MODEL_ATTR_MAX_BALANCE, settingService.getMaxBalance());
    model.addAttribute(MODEL_ATTR_MIN_TOP_UP, settingService.getMinTopUp());
    model.addAttribute(MODEL_ATTR_TRANSACTION_FEE, transactionFee);
    if (redirect != null) {
      model.addAttribute("redirect", redirect);
    }
    return "topup";
  }

  /**
   * Redirects the user to the payment page
   *
   * @param topupAmount the amount they want to add to their account
   * @param currentUser the user
   * @param redirectAttributes redirect attributes
   * @return the url for the payment
   */
  @PreAuthorize("hasRole('USER') and !hasRole('BANNED')")
  @PostMapping
  public String handleTopup(
      @RequestParam("topupAmount") String topupAmount,
      @RequestParam(value = "redirect", required = false) String redirect,
      @ModelAttribute("currentUser") User currentUser,
      RedirectAttributes redirectAttributes) {
    try {
      BigDecimal amount;
      try {
        amount = new BigDecimal(topupAmount);
      } catch (NumberFormatException e) {
        notificationService.addErrorMessage(redirectAttributes, "Invalid top-up amount.");
        return "redirect:/topup";
      }
      BigDecimal maxBalance = settingService.getMaxBalance();

      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        notificationService.addErrorMessage(redirectAttributes, "Top-up amount must be positive.");
      } else if (amount.add(currentUser.getBalance()).compareTo(maxBalance) > 0) {
        notificationService.addErrorMessage(
            redirectAttributes, "Top-up amount must be less than " + maxBalance);
      } else {
        TopupTransaction transaction =
            createTopUpTransaction(currentUser, amount, "Mollie Deposit");
        Transaction redirectTx = null;
        if (redirect != null && !redirect.isBlank()) {
          try {
            UUID redirectId = UUID.fromString(redirect);
            redirectTx = transactionRepository.findById(redirectId).orElse(null);
          } catch (IllegalArgumentException ex) {
            redirectTx = null; // invalid UUID format
          }

          if (redirectTx == null) {
            notificationService.addErrorMessage(
                redirectAttributes, "Invalid redirect payment reference.");
            return "redirect:/topup";
          }

          if (redirectTx.getUser() != null
              && !redirectTx.getUser().getId().equals(currentUser.getId())) {
            notificationService.addErrorMessage(
                redirectAttributes, "Invalid redirect payment reference.");
            return "redirect:/topup";
          }
        }
        transaction.setRedirectPayment(redirectTx);
        transactionRepository.save(transaction);

        String url = depositService.getMollieUrl(transaction);
        if (url != null) {
          transactionsService.save(transaction);
          return "redirect:" + url;
        } else {
          notificationService.addErrorMessage(
              redirectAttributes, "Could not create the payment link. Please try again.");
        }
      }
    } catch (Exception e) {
      notificationService.addErrorMessage(
          redirectAttributes, "An unexpected error occurred. Please try again.");
    }
    return "redirect:/topup";
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
      @PathVariable String key, @ModelAttribute("currentUser") User currentUser, Model model) {
    Transaction t =
        transactionRepository
            .findById(UUID.fromString(key))
            .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + key));
    assertCurrentUserOwnsTransaction(currentUser, t);
    model.addAttribute(MODEL_ATTR_TRANSACTION_ID, key);
    model.addAttribute("isTopup", t.getType().equals(Transaction.TransactionType.TOP_UP));
    if (t instanceof TopupTransaction topupTransaction
        && topupTransaction.getRedirectPayment() != null
        && topupTransaction
            .getRedirectPayment()
            .getStatus()
            .equals(Transaction.TransactionStatus.PENDING)) {
      model.addAttribute("redirect", topupTransaction.getRedirectPayment().getId().toString());
    }
    return switch (t.getStatus()) {
      case Transaction.TransactionStatus.PENDING -> "pending";
      case Transaction.TransactionStatus.SUCCESSFUL -> "successful";
      case Transaction.TransactionStatus.FAILED -> "failed";
      case Transaction.TransactionStatus.CANCELLED -> "cancelled";
      default -> "error";
    };
  }

  /**
   * This is where the mollie webhook goes
   *
   * @param mollieId the id of the transaction
   * @return a http status, this isn't relevant for the user, mollie gets it
   */
  @PostMapping("/status")
  public ResponseEntity<HttpStatus> depositStatus(@RequestParam(name = "id") String mollieId) {
    if (transactionsService.getTransaction(mollieId).isEmpty())
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    TopupTransaction t = transactionsService.getTransaction(mollieId).get();
    depositService.validateTransaction(t.getId());
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
