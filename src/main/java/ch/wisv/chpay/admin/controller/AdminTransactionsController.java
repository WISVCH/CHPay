package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.model.transaction.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/transactions")
public class AdminTransactionsController extends BaseTransactionController {

  @Autowired
  protected AdminTransactionsController(AdminTransactionService adminTransactionService) {
    super(adminTransactionService);
  }

  /**
   * Gets the page showing all transactions for a given year and month.
   *
   * @param model of type Model
   * @param yearMonth the yearMonth parameter in format "YYYY-MM" (optional)
   * @return String
   */
  @GetMapping
  public String getPage(
      RedirectAttributes redirectAttributes,
      Model model,
      HttpServletRequest request,
      @RequestParam(required = false) String yearMonth) {

    try {
      YearMonth selectedYearMonth =
          handleYearMonthParameter(
              yearMonth,
              request,
              adminTransactionService::getMostRecentYearMonth,
              ym -> "/admin/transactions?yearMonth=" + ym);

      // Get all transactions for the specified month
      List<Transaction> transactions =
          adminTransactionService.getTransactionsByYearMonth(selectedYearMonth);

      // Get all possible months for the dropdown
      List<YearMonth> allPossibleMonths = adminTransactionService.getAllPossibleMonths();

      // Add attributes to the model
      addTransactionModelAttributes(model, transactions, selectedYearMonth, allPossibleMonths);

      model.addAttribute(MODEL_ATTR_URL_PAGE, "adminTransactions");

      return "admin-transaction-table";
    } catch (BaseTransactionController.RedirectException e) {
      return "redirect:" + e.getRedirectUrl();
    }
  }

  @RequestMapping(value = "/csv")
  public ResponseEntity<InputStreamResource> fooAsCSV(@RequestParam String yearMonth) {
    try {
      YearMonth selectedYearMonth = YearMonth.parse(yearMonth);

      List<Transaction> transactions =
          adminTransactionService.getTransactionsByYearMonth(selectedYearMonth);
      String csvData =
          transactions.stream()
              .filter(
                  t ->
                      t.getStatus().equals(Transaction.TransactionStatus.SUCCESSFUL)
                          || t.getStatus().equals(Transaction.TransactionStatus.PARTIALLY_REFUNDED)
                          || t.getStatus().equals(Transaction.TransactionStatus.REFUNDED))
              .map(
                  t ->
                      t.getId().toString()
                          + ";"
                          + t.getType().toString()
                          + ";"
                          + t.getUser().getName()
                          + ";"
                          + t.getDescription()
                          + ";"
                          + t.getAmount()
                          + ";"
                          + t.getStatus().name()
                          + ";"
                          + t.getTimestamp().toString())
              .collect(Collectors.joining("\n"));
      csvData = "Id;Type;Name;Description;Amount;Status;Timestamp\n" + csvData;
      InputStream bufferedInputStream =
          new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
      InputStreamResource fileInputStream = new InputStreamResource(bufferedInputStream);

      String filename = "chpay_" + selectedYearMonth + "_export.csv";

      // setting HTTP headers
      HttpHeaders headers = new HttpHeaders();
      headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
      // defining the custom Content-Type
      headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");

      return new ResponseEntity<>(fileInputStream, headers, HttpStatus.OK);
    } catch (Exception e) {
      HttpHeaders headers = new HttpHeaders();
      headers.add("Location", "/administrator/events/");
      return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
    }
  }
}
