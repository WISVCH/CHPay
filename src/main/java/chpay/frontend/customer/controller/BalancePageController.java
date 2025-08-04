package chpay.frontend.customer.controller;

import static chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction.createTopUpTransaction;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.paymentbackend.service.BalanceService;
import chpay.paymentbackend.service.DepositService;
import chpay.paymentbackend.service.SettingService;
import chpay.paymentbackend.service.TransactionService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
import java.util.UUID;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BalancePageController extends PageController {
  private final DepositService depositService;
  private final TransactionService transactionsService;
  private final NotificationService notificationService;
  private final BalanceService balanceService;
  private final TransactionRepository transactionRepository;
  private final SettingService settingService;

  @Value("${mollie.transactionFee}")
  private String transactionFee;

  @Autowired
  public BalancePageController(
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
   * Serves the balance page at /balance.html URL.
   *
   * @return String view name for balance template
   */
  @GetMapping("/balance")
  public String showBalancePage(Model model) {
    // add the signature of the current page to thymeleaf context
    model.addAttribute(MODEL_ATTR_URL_PAGE, "balance");
    model.addAttribute("maxBalance", settingService.getMaxBalance());
    model.addAttribute("minTopUp", settingService.getMinTopUp());
    model.addAttribute("transactionFee", transactionFee);
    return "balance";
  }

  /**
   * Redirects the user to the payment page
   *
   * @param topupAmount the amount they want to add to their account
   * @param currentUser the user
   * @param redirectAttributes redirect attributes
   * @return the url for the payment
   */
  @SuppressWarnings("PMD.SystemPrintln") // Suppress PMD violation for System.out usage
  @PostMapping("/balance/topup")
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
        return "redirect:/balance";
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
    return "redirect:/balance";
  }

  /**
   * Redirects the user to a payment status page
   *
   * @param key transaction's id
   * @param redirectAttributes
   * @return either a purgatory page, a success or fail page
   */
  @GetMapping("/payment/complete/{key}")
  public String depositSuccess(
      @PathVariable String key, RedirectAttributes redirectAttributes, Model model)
      throws NotFoundException {
    Transaction t =
        transactionRepository
            .findById(UUID.fromString(key))
            .orElseThrow(() -> new NotFoundException(key));
    model.addAttribute("transactionID", key);
    return switch (t.getStatus()) {
      case Transaction.TransactionStatus.PENDING -> "pending";
      case Transaction.TransactionStatus.SUCCESSFUL -> "successful";
      case Transaction.TransactionStatus.FAILED -> "failed";
      default -> "error";
    };
  }
}
