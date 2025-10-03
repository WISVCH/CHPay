package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.model.transaction.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

/**
 * Base controller for transaction-related pages that provides common year-month filtering
 * functionality.
 */
public abstract class BaseTransactionController extends AdminController {

  protected final AdminTransactionService adminTransactionService;

  @Autowired
  protected BaseTransactionController(AdminTransactionService adminTransactionService) {
    this.adminTransactionService = adminTransactionService;
  }

  /**
   * Handles year-month parameter parsing and redirection logic.
   *
   * @param yearMonth the yearMonth parameter in format "YYYY-MM" (optional)
   * @param request the HttpServletRequest for getting query parameters
   * @param getMostRecentYearMonth function to get the most recent year-month for the specific
   *     context
   * @param buildRedirectUrl function to build the redirect URL with the selected year-month
   * @return the selected YearMonth, or null if a redirect should be performed
   */
  protected YearMonth handleYearMonthParameter(
      String yearMonth,
      HttpServletRequest request,
      java.util.function.Supplier<YearMonth> getMostRecentYearMonth,
      java.util.function.Function<YearMonth, String> buildRedirectUrl) {

    // Parse yearMonth parameter or redirect to most recent
    if (yearMonth == null || yearMonth.trim().isEmpty()) {
      YearMonth selectedYearMonth = getMostRecentYearMonth.get();
      String queryString = request.getQueryString();
      String preservedParams = "";
      if (queryString != null && !queryString.isEmpty()) {
        // Remove yearMonth parameter if it exists, keep others
        preservedParams =
            "&" + queryString.replaceAll("(&?)yearMonth=[^&]*(&?)", "").replaceAll("^&|&$", "");
      }
      String redirectUrl =
          buildRedirectUrl.apply(selectedYearMonth)
              + (preservedParams.isEmpty() ? "" : "&" + preservedParams);
      throw new RedirectException(redirectUrl);
    }

    try {
      return YearMonth.parse(yearMonth);
    } catch (DateTimeParseException e) {
      // Invalid format, redirect to most recent month
      YearMonth selectedYearMonth = getMostRecentYearMonth.get();
      String queryString = request.getQueryString();
      String preservedParams = "";
      if (queryString != null && !queryString.isEmpty()) {
        // Remove yearMonth parameter if it exists, keep others
        preservedParams =
            "&" + queryString.replaceAll("(&?)yearMonth=[^&]*(&?)", "").replaceAll("^&|&$", "");
      }
      String redirectUrl =
          buildRedirectUrl.apply(selectedYearMonth)
              + (preservedParams.isEmpty() ? "" : "&" + preservedParams);
      throw new RedirectException(redirectUrl);
    }
  }

  /**
   * Adds common transaction model attributes.
   *
   * @param model the Model object
   * @param transactions the list of transactions
   * @param selectedYearMonth the selected year-month
   * @param allPossibleMonths the list of all possible months
   */
  protected void addTransactionModelAttributes(
      Model model,
      List<Transaction> transactions,
      YearMonth selectedYearMonth,
      List<YearMonth> allPossibleMonths) {
    model.addAttribute(MODEL_ATTR_TRANSACTIONS, transactions);
    model.addAttribute(MODEL_ATTR_SELECTED_YEAR_MONTH, selectedYearMonth);
    model.addAttribute(MODEL_ATTR_ALL_POSSIBLE_MONTHS, allPossibleMonths);
  }

  /** Custom exception for handling redirects in the base controller. */
  public static class RedirectException extends RuntimeException {
    private final String redirectUrl;

    public RedirectException(String redirectUrl) {
      super("Redirect to: " + redirectUrl);
      this.redirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
      return redirectUrl;
    }
  }
}
