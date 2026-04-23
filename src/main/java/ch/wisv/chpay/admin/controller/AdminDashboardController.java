package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.model.MonthlyBalanceBreakdown;
import ch.wisv.chpay.admin.service.AdminMonthlyBalanceService;
import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.repository.UserRepository;
import ch.wisv.chpay.core.service.SettingService;
import ch.wisv.chpay.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin")
public class AdminDashboardController extends AdminController {
  private final AdminTransactionService adminTransactionService;
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final SettingService settingService;
  private final UserService userService;
  private final AdminMonthlyBalanceService adminMonthlyBalanceService;

  protected AdminDashboardController(
      AdminTransactionService adminTransactionService,
      TransactionRepository transactionRepository,
      UserRepository userRepository,
      SettingService settingService,
      UserService userService,
      AdminMonthlyBalanceService adminMonthlyBalanceService) {
    this.adminTransactionService = adminTransactionService;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
    this.settingService = settingService;
    this.userService = userService;
    this.adminMonthlyBalanceService = adminMonthlyBalanceService;
  }

  @GetMapping
  public String adminPage(
      Model model,
      RedirectAttributes redirectAttributes,
      HttpServletRequest request,
      @RequestParam(required = false) String yearMonth) {
    try {
      YearMonth selectedYearMonth =
          YearMonthSelectionSupport.resolveYearMonthOrRedirect(
              yearMonth,
              request,
              adminTransactionService::getMostRecentYearMonth,
              ym -> "/admin?yearMonth=" + ym);

      List<YearMonth> allPossibleMonths = adminTransactionService.getAllPossibleMonths();
      if (allPossibleMonths.isEmpty()) {
        allPossibleMonths = List.of(selectedYearMonth);
      }

      MonthlyBalanceBreakdown monthlyBreakdown =
          adminMonthlyBalanceService.getBreakdownForMonth(selectedYearMonth);

      model.addAttribute(MODEL_ATTR_URL_PAGE, "admin");
      model.addAttribute(MODEL_ATTR_USERS, userService.countAll());
      model.addAttribute(MODEL_ATTR_TRANSACTIONS, transactionRepository.count());
      model.addAttribute(MODEL_ATTR_STATUS, settingService.isFrozen() ? "Frozen" : "Active");
      model.addAttribute(MODEL_ATTR_MAX_BALANCE, settingService.getMaxBalance());
      model.addAttribute(MODEL_ATTR_BALANCE, userRepository.getBalanceNow());
      model.addAttribute(MODEL_ATTR_SELECTED_YEAR_MONTH, selectedYearMonth);
      model.addAttribute(MODEL_ATTR_ALL_POSSIBLE_MONTHS, allPossibleMonths);
      model.addAttribute("monthlyBreakdown", monthlyBreakdown);
      return "admin";
    } catch (YearMonthSelectionSupport.RedirectException e) {
      return "redirect:" + e.getRedirectUrl();
    }
  }
}
