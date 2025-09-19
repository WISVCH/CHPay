package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.customer.service.MailService;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.TransactionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TransactionHistoryController extends CustomerController {

  private final TransactionService transactionService;
  private final NotificationService notificationService;
  private final MailService mailService;

  @Autowired
  protected TransactionHistoryController(
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
   * @param model the model containing current user information
   * @return ResponseEntity with appropriate HTTP status
   */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @PostMapping("/transactions/email-receipt/{id}")
  public ResponseEntity<HttpStatus> emailReceipt(@PathVariable String id, Model model) {
    try {
      // Get current user from model
      User currentUser = (User) model.getAttribute("currentUser");
      if (currentUser == null) {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
      }

      // Check if user is admin
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      boolean isAdmin = authentication.getAuthorities().stream()
          .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

      // Get the transaction to verify ownership
      Optional<Transaction> transactionOpt = transactionService.getTransactionById(UUID.fromString(id));
      if (transactionOpt.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      Transaction transaction = transactionOpt.get();

      // Check if user owns the transaction or is admin
      if (!isAdmin && !transaction.getUser().getId().equals(currentUser.getId())) {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
      }

      // If authorized, send the receipt
      mailService.sendReceiptByEmail(id);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
