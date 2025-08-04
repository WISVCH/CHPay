package chpay.frontend.customer.controller;

import chpay.DatabaseHandler.transactiondb.entities.Statistic;
import chpay.paymentbackend.service.StatisticsService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdministratorStatisticPage extends PageController {
  private final StatisticsService statisticsService;
  private final NotificationService notificationService;

  private final String MODEL_ATTR_MAIN_STAT = "mainStat";
  private final String MODEL_ATTR_SECOND_STAT = "secondStat";
  private final String MODEL_ATTR_STATS = "stats";
  private final String MODEL_ATTR_TYPE = "type";

  @Autowired
  protected AdministratorStatisticPage(
      StatisticsService statisticsService, NotificationService notificationService) {
    super();
    this.statisticsService = statisticsService;
    this.notificationService = notificationService;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping(value = "/admin/stats/{type}")
  public String getPage(
      Model model,
      @PathVariable String type,
      @RequestParam(required = false) String daysBack,
      RedirectAttributes redirectAttributes) {
    int days = 90;
    if (daysBack != null) {
      days = Integer.parseInt(daysBack);
    }
    if (days < 1) {
      throw new IllegalArgumentException("Days cannot be less than 1");
    }

    if (type.equals("balance")) {
      model.addAttribute(MODEL_ATTR_TYPE, "balance");
      model.addAttribute(MODEL_ATTR_MAIN_STAT, statisticsService.getCurrentBalance());
      model.addAttribute(MODEL_ATTR_SECOND_STAT, statisticsService.getAverageBalance());
      model.addAttribute(
          MODEL_ATTR_STATS,
          statisticsService.getStatisticsForTimeframe(Statistic.StatisticType.BALANCE, days));
      model.addAttribute(MODEL_ATTR_URL_PAGE, "adminStatistics");
    } else if (type.equals("incoming-funds")) {
      model.addAttribute(MODEL_ATTR_TYPE, "incoming-funds");
      BigDecimal incomingFunds = statisticsService.getCombinedIncomeFunds(days);
      if (incomingFunds != null && incomingFunds.compareTo(BigDecimal.ZERO) > 0) {
        model.addAttribute(MODEL_ATTR_MAIN_STAT, incomingFunds);
        model.addAttribute(
            MODEL_ATTR_SECOND_STAT,
            incomingFunds.divide(BigDecimal.valueOf(days), BigDecimal.ROUND_UP));
      } else {
        model.addAttribute(MODEL_ATTR_MAIN_STAT, 0);
        model.addAttribute(MODEL_ATTR_SECOND_STAT, 0);
      }
      model.addAttribute(
          MODEL_ATTR_STATS,
          statisticsService.getStatisticsForTimeframe(
              Statistic.StatisticType.INCOMING_FUNDS, days));
      model.addAttribute(MODEL_ATTR_URL_PAGE, "adminStatistics");
    } else if (type.equals("outcoming-funds")) {
      model.addAttribute(MODEL_ATTR_TYPE, "outcoming-funds");
      BigDecimal outcomingFunds = statisticsService.getCombinedOutgoingFunds(days);
      if (outcomingFunds != null && outcomingFunds.compareTo(BigDecimal.ZERO) > 0) {
        model.addAttribute(MODEL_ATTR_MAIN_STAT, outcomingFunds);
        model.addAttribute(
            MODEL_ATTR_SECOND_STAT,
            outcomingFunds.divide(BigDecimal.valueOf(days), BigDecimal.ROUND_UP));
      } else {
        model.addAttribute(MODEL_ATTR_MAIN_STAT, 0);
        model.addAttribute(MODEL_ATTR_SECOND_STAT, 0);
      }
      model.addAttribute(
          MODEL_ATTR_STATS,
          statisticsService.getStatisticsForTimeframe(
              Statistic.StatisticType.OUTGOING_FUNDS, days));
      model.addAttribute(MODEL_ATTR_URL_PAGE, "adminStatistics");
    } else {
      throw new NoSuchElementException("Statistic not found");
    }
    return "statistics";
  }
}
