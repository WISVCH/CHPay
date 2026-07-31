package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.service.TransactionService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TransactionHistoryController extends CustomerController {

  private final TransactionService transactionService;
  private final ch.wisv.chpay.core.service.OfxExportService ofxExportService;
  private final ch.wisv.chpay.core.service.CsvExportService csvExportService;

  @Autowired
  protected TransactionHistoryController(
      TransactionService transactionService,
      ch.wisv.chpay.core.service.OfxExportService ofxExportService,
      ch.wisv.chpay.core.service.CsvExportService csvExportService) {
    super();
    this.transactionService = transactionService;
    this.ofxExportService = ofxExportService;
    this.csvExportService = csvExportService;
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

    // Get all transactions for the user
    List<ch.wisv.chpay.core.model.transaction.Transaction> transactions =
        transactionService.getTransactionsForUser(currentUser);

    // Sort transactions by date in descending order (most recent first)
    transactions.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));

    model.addAttribute(MODEL_ATTR_TRANSACTIONS, transactions);
    model.addAttribute(MODEL_ATTR_URL_PAGE, "transactions");
    return "transactions";
  }

  /** Export all of the current user's transactions as an OFX file. */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping(value = "/transactions/export/ofx")
  public ResponseEntity<byte[]> exportTransactionsOfx(Model model) {
    User currentUser = (User) model.getAttribute("currentUser");
    List<Transaction> transactions =
        new java.util.ArrayList<>(
            transactionService.getTransactionsForUser(currentUser).stream()
                .filter(
                    t ->
                        t.getStatus() == Transaction.TransactionStatus.SUCCESSFUL
                            || t.getStatus() == Transaction.TransactionStatus.PARTIALLY_REFUNDED
                            || t.getStatus() == Transaction.TransactionStatus.REFUNDED)
                .toList());
    transactions.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));

    byte[] ofxBytes = ofxExportService.generateOfx(currentUser, transactions);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("application", "x-ofx"));
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.ofx");
    headers.setContentLength(ofxBytes.length);
    return new ResponseEntity<>(ofxBytes, headers, HttpStatus.OK);
  }

  /** Export all of the current user's transactions as a CSV file. */
  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping(value = "/transactions/export/csv")
  public ResponseEntity<byte[]> exportTransactionsCsv(Model model) {
    User currentUser = (User) model.getAttribute("currentUser");
    List<Transaction> transactions =
        new java.util.ArrayList<>(
            transactionService.getTransactionsForUser(currentUser).stream()
                .filter(
                    t ->
                        t.getStatus() == Transaction.TransactionStatus.SUCCESSFUL
                            || t.getStatus() == Transaction.TransactionStatus.PARTIALLY_REFUNDED
                            || t.getStatus() == Transaction.TransactionStatus.REFUNDED)
                .toList());
    transactions.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));

    byte[] csvBytes = csvExportService.generateCsv(transactions);

    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv");
    headers.setContentLength(csvBytes.length);
    return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
  }
}
