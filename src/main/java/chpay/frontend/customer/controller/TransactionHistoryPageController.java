package chpay.frontend.customer.controller;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.frontend.customer.service.BalanceHistoryService;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.MailService;
import chpay.paymentbackend.service.TransactionService;
import chpay.shared.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TransactionHistoryPageController extends PageController {
  private static final String MODEL_ATTR_TRANSACTIONS = "transactions";
  private static final String MODEL_ATTR_INFO = "info";
  private static final String MODEL_ATTR_PAGE = "page";
  private static final String MODEL_ATTR_SORTBY = "sortBy";
  private static final String MODEL_ATTR_ORDER = "order";
  private static final String MODEL_ATTR_TOPUPS_TAG = "onlyTopUps";

  private final BalanceHistoryService balanceHistoryService;
  private final TransactionService transactionService;
  private final NotificationService notificationService;
  private final MailService mailService;

  @Autowired
  protected TransactionHistoryPageController(
      BalanceHistoryService balanceHistoryService,
      TransactionService transactionService,
      NotificationService notificationService,
      MailService mailService) {
    super();
    this.balanceHistoryService = balanceHistoryService;
    this.transactionService = transactionService;
    this.notificationService = notificationService;
    this.mailService = mailService;
  }

  /**
   * Gets the page showing the users history of transactions.
   *
   * @param model of type Model
   * @param page page number for the list of transactions
   * @return String
   */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  public String getPage(
      Model model,
      @RequestParam(defaultValue = "1") String page,
      @RequestParam(defaultValue = "timestamp") String sortBy,
      @RequestParam(defaultValue = "desc") String order,
      @RequestParam(defaultValue = "false") String onlyTopUps,
      RedirectAttributes redirectAttributes)
      throws JsonProcessingException {
    int pageNum;
    try {
      pageNum = Integer.parseInt(page);
    } catch (Exception ex) {
      pageNum = 1;
    }

    // make sure the sort params are correct
    if (!sortBy.equals("timestamp") && !sortBy.equals("amount") && !sortBy.equals("description")) {
      sortBy = "timestamp";
    }

    if (!order.equals("desc") && !order.equals("asc")) {
      order = "desc";
    }

    PaginationInfo paginationInfo =
        balanceHistoryService.getTransactionPageInfo(
            ((User) model.getAttribute("currentUser")),
            transactionService,
            pageNum,
            4,
            ((boolean) onlyTopUps.equals("true")));

    model.addAttribute(MODEL_ATTR_LINKS, null);
    model.addAttribute(MODEL_ATTR_PAGE, pageNum);
    model.addAttribute(MODEL_ATTR_SORTBY, sortBy);
    model.addAttribute(MODEL_ATTR_ORDER, order);
    model.addAttribute(MODEL_ATTR_TOPUPS_TAG, onlyTopUps);

    model.addAttribute(
        MODEL_ATTR_TRANSACTIONS,
        balanceHistoryService.getTransactionsByUserAsJSON(
            ((User) model.getAttribute("currentUser")),
            transactionService,
            paginationInfo,
            sortBy,
            order,
            ((boolean) onlyTopUps.equals("true"))));

    model.addAttribute(MODEL_ATTR_INFO, paginationInfo);
    return "transactions";
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
    List<chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction> transactions = 
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
