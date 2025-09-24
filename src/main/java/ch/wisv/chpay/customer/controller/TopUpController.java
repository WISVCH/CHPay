package ch.wisv.chpay.customer.controller;

import static ch.wisv.chpay.core.model.transaction.TopupTransaction.createTopUpTransaction;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.TopupTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.service.BalanceService;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.SettingService;
import ch.wisv.chpay.core.service.TransactionService;
import ch.wisv.chpay.customer.service.DepositService;
import java.math.BigDecimal;
import java.util.UUID;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/topup")
public class TopUpController extends CustomerController {
  private final DepositService depositService;
  private final TransactionService transactionsService;
  private final NotificationService notificationService;
  private final BalanceService balanceService;
  private final TransactionRepository transactionRepository;
  private final SettingService settingService;

  @Value("${mollie.transaction_fee}")
  private String transactionFee;

  @Autowired
  public TopUpController(
      DepositService depositService,
      TransactionService transactionsService,
      NotificationService notificationService,
      BalanceService balanceService,
      TransactionRepository transactionRepository,
      SettingService settingService) {
    this.depositService = depositService;
    this.transactionsService = transactionsService;
    this.notificationService = notificationService;
    this.balanceService = balanceService;
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
  public String showBalancePage(Model model) {
    // add the signature of the current page to thymeleaf context
    model.addAttribute(MODEL_ATTR_URL_PAGE, "topup");
    model.addAttribute(MODEL_ATTR_MAX_BALANCE, settingService.getMaxBalance());
    model.addAttribute(MODEL_ATTR_MIN_TOP_UP, settingService.getMinTopUp());
    model.addAttribute(MODEL_ATTR_TRANSACTION_FEE, transactionFee);
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
          redirectAttributes, "An unexpected error occurred: " + e.getMessage());
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
