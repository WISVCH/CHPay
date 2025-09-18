package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.service.MailService;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.TransactionService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TransactionHistoryPageController extends PageController {
  private static final String MODEL_ATTR_TRANSACTIONS = "transactions";
  private static final String MODEL_ATTR_INFO = "info";
  private static final String MODEL_ATTR_PAGE = "page";
  private static final String MODEL_ATTR_SORTBY = "sortBy";
  private static final String MODEL_ATTR_ORDER = "order";
  private static final String MODEL_ATTR_TOPUPS_TAG = "onlyTopUps";

  private final TransactionService transactionService;
  private final NotificationService notificationService;
  private final MailService mailService;

  @Autowired
  protected TransactionHistoryPageController(
      TransactionService transactionService,
      NotificationService notificationService,
      MailService mailService) {
    super();
    this.transactionService = transactionService;
    this.notificationService = notificationService;
    this.mailService = mailService;
  }

  /**
   * Gets the simplified transactions page showing only essential transaction information.
   *
   * @param model of type Model
   * @return String
   */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping(value = "/transactions")
  public String getSimplifiedTransactionsPage(Model model) {
    User currentUser = (User) model.getAttribute("currentUser");

    // Get all transactions for the user (no pagination for simplicity)
    List<ch.wisv.chpay.core.model.transaction.Transaction> transactions =
        transactionService.getTransactionsForUserPageable(currentUser, 0, Integer.MAX_VALUE);

    // Sort transactions by date in descending order (most recent first)
    transactions.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));

    model.addAttribute(MODEL_ATTR_TRANSACTIONS, transactions);
    model.addAttribute(MODEL_ATTR_URL_PAGE, "transactions");
    return "transactions";
  }

  /**
   * Sends a user their receipt
   *
   * @param id the transaction's id
   * @return
   */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @PostMapping("/transactions/email-receipt/{id}")
  public ResponseEntity<HttpStatus> emailReceipt(@PathVariable String id) {
    try {
      mailService.sendReceiptByEmail(id);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
