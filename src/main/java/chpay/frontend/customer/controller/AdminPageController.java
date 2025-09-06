package chpay.frontend.customer.controller;

import chpay.DatabaseHandler.transactiondb.entities.Statistic;
import chpay.paymentbackend.service.SettingService;
import chpay.paymentbackend.service.StatisticsService;
import chpay.paymentbackend.service.UserService;
import chpay.shared.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminPageController extends PageController {
  private final StatisticsService statisticsService;
  private final NotificationService notificationService;
  private final SettingService settingService;
  private final UserService userService;

  private final String MODEL_ATTR_USERS = "users";
  private final String MODEL_ATTR_TRANSACTIONS = "transactions";
  private final String MODEL_ATTR_STATUS = "status";
  private final String MODEL_ATTR_MAX_BALANCE = "maxBalance";
  private final String MODEL_ATTR_BALANCE = "balanceAvailable";
  private final String MODEL_ATTR_INCOMING = "incomingFunds";
  private final String MODEL_ATTR_OUTGOING = "outgoingFunds";

  protected AdminPageController(
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

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping(value = "/admin")
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
