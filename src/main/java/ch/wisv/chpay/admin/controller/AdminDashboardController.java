package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.core.model.Statistic;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.SettingService;
import ch.wisv.chpay.core.service.StatisticsService;
import ch.wisv.chpay.core.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin")
public class AdminDashboardController extends AdminController {
  private final StatisticsService statisticsService;
  private final NotificationService notificationService;
  private final SettingService settingService;
  private final UserService userService;

  protected AdminDashboardController(
      StatisticsService statisticsService,
      NotificationService notificationService,
      SettingService settingService,
      UserService userService) {
    super();
    this.statisticsService = statisticsService;
    this.notificationService = notificationService;
    this.settingService = settingService;
    this.userService = userService;
  }

  @GetMapping
  public String adminPage(Model model, RedirectAttributes redirectAttributes) {
    model.addAttribute(MODEL_ATTR_URL_PAGE, "admin");
    model.addAttribute(MODEL_ATTR_USERS, userService.countAll());
    model.addAttribute(MODEL_ATTR_TRANSACTIONS, statisticsService.getTransactionCount());
    model.addAttribute(MODEL_ATTR_STATUS, settingService.isFrozen() ? "Frozen" : "Active");
    model.addAttribute(MODEL_ATTR_MAX_BALANCE, settingService.getMaxBalance());
    model.addAttribute(MODEL_ATTR_BALANCE, statisticsService.getCurrentBalance());
    model.addAttribute(
        MODEL_ATTR_INCOMING,
        statisticsService.getLastMonth(Statistic.StatisticType.INCOMING_FUNDS));
    model.addAttribute(
        MODEL_ATTR_OUTGOING,
        statisticsService.getLastMonth(Statistic.StatisticType.OUTGOING_FUNDS));
    return "admin";
  }
}
