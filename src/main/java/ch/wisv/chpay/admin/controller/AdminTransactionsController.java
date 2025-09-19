package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.model.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/transactions")
public class AdminTransactionsController extends AdminController {

  private final AdminTransactionService adminTransactionService;

  @Autowired
  protected AdminTransactionsController(AdminTransactionService adminTransactionService) {
    super();
    this.adminTransactionService = adminTransactionService;
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

    YearMonth selectedYearMonth;

    // Parse yearMonth parameter or redirect to most recent
    if (yearMonth == null || yearMonth.trim().isEmpty()) {
      selectedYearMonth = adminTransactionService.getMostRecentYearMonth();
      String queryString = request.getQueryString();
      String preservedParams = "";
      if (queryString != null && !queryString.isEmpty()) {
        // Remove yearMonth parameter if it exists, keep others
        preservedParams = "&" + queryString.replaceAll("(&?)yearMonth=[^&]*(&?)", "").replaceAll("^&|&$", "");
      }
      return "redirect:/admin/transactions?yearMonth=" + selectedYearMonth + preservedParams;
    }

    try {
      selectedYearMonth = YearMonth.parse(yearMonth);
    } catch (DateTimeParseException e) {
      // Invalid format, redirect to most recent month
      selectedYearMonth = adminTransactionService.getMostRecentYearMonth();
      String queryString = request.getQueryString();
      String preservedParams = "";
      if (queryString != null && !queryString.isEmpty()) {
        // Remove yearMonth parameter if it exists, keep others
        preservedParams = "&" + queryString.replaceAll("(&?)yearMonth=[^&]*(&?)", "").replaceAll("^&|&$", "");
      }
      return "redirect:/admin/transactions?yearMonth=" + selectedYearMonth + preservedParams;
    }

    // Get all transactions for the specified month
    List<Transaction> transactions = adminTransactionService.getTransactionsByYearMonth(selectedYearMonth);

    // Get all possible months for the dropdown
    List<YearMonth> allPossibleMonths = adminTransactionService.getAllPossibleMonths();

    // Add attributes to the model
    model.addAttribute(MODEL_ATTR_TRANSACTIONS, transactions);
    model.addAttribute(MODEL_ATTR_SELECTED_YEAR_MONTH, selectedYearMonth);
    model.addAttribute(MODEL_ATTR_ALL_POSSIBLE_MONTHS, allPossibleMonths);
 
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminTransactions");

    return "admin-transaction-table";
  }
}
