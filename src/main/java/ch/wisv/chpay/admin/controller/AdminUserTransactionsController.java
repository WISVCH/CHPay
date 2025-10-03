package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/user/{userId}/transaction")
public class AdminUserTransactionsController extends BaseTransactionController {

  private final UserService userService;

  @Autowired
  protected AdminUserTransactionsController(
      AdminTransactionService adminTransactionService, UserService userService) {
    super(adminTransactionService);
    this.userService = userService;
  }

  /**
   * Gets the page showing all transactions for a specific user.
   *
   * @param model of type Model
   * @param userId the UUID of the user
   * @param yearMonth the yearMonth parameter in format "YYYY-MM" (optional)
   * @return String
   */
  @GetMapping
  public String getPage(
      RedirectAttributes redirectAttributes,
      Model model,
      HttpServletRequest request,
      @PathVariable String userId,
      @RequestParam(required = false) String yearMonth) {

    // Get the user
    User user = userService.getUserById(userId);
    if (user == null) {
      throw new NoSuchElementException("User not found");
    }

    UUID userUuid = UUID.fromString(userId);

    try {
      YearMonth selectedYearMonth =
          handleYearMonthParameter(
              yearMonth,
              request,
              () -> adminTransactionService.getMostRecentYearMonthForUser(userUuid),
              ym -> "/admin/user/" + userId + "/transaction?yearMonth=" + ym);

      // Get all transactions for the specified user and month
      List<Transaction> transactions =
          adminTransactionService.getTransactionsByUserIdAndYearMonth(userUuid, selectedYearMonth);

      // Get all possible months for the dropdown
      List<YearMonth> allPossibleMonths =
          adminTransactionService.getAllPossibleMonthsForUser(userUuid);

      // Add attributes to the model
      addTransactionModelAttributes(model, transactions, selectedYearMonth, allPossibleMonths);
      model.addAttribute(MODEL_ATTR_USER, user);
      model.addAttribute(MODEL_ATTR_TRANSACTION_PAGE_TYPE, "user");
      model.addAttribute(MODEL_ATTR_BREADCRUMB_USER_ID, userId);
      model.addAttribute(MODEL_ATTR_BREADCRUMB_USER_NAME, user.getName());

      model.addAttribute(MODEL_ATTR_URL_PAGE, "adminUsers");

      return "admin-transaction-table";
    } catch (BaseTransactionController.RedirectException e) {
      return "redirect:" + e.getRedirectUrl();
    }
  }
}
