package chpay.frontend.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import chpay.DatabaseHandler.transactiondb.entities.Statistic;
import chpay.frontend.customer.controller.AdministratorStatisticPage;
import chpay.paymentbackend.service.StatisticsService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class AdministratorStatisticPageTest {

  @Mock private StatisticsService statisticsService;
  @Mock private NotificationService notificationService;
  @Mock private Model model;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private AdministratorStatisticPage controller;
  private final BigDecimal dummyValue = BigDecimal.valueOf(900);

  @BeforeEach
  void setup() {
    Mockito.lenient()
        .when(statisticsService.getStatisticsForTimeframe(any(), anyInt()))
        .thenReturn(Collections.emptyList());
  }

  @Test
  void testBalanceStats() {
    when(statisticsService.getCurrentBalance()).thenReturn(dummyValue);
    when(statisticsService.getAverageBalance()).thenReturn(dummyValue);

    String view = controller.getPage(model, "balance", "30", redirectAttributes);

    verify(model).addAttribute("type", "balance");
    verify(model).addAttribute("mainStat", dummyValue);
    verify(model).addAttribute("secondStat", dummyValue);
    verify(model).addAttribute("stats", Collections.emptyList());
    verify(model).addAttribute("urlPage", "adminStatistics");

    assertEquals("statistics", view);
  }

  @Test
  void testIncomingFundsStatsWithPositiveValue() {
    when(statisticsService.getCombinedIncomeFunds(30)).thenReturn(dummyValue);

    String view = controller.getPage(model, "incoming-funds", "30", redirectAttributes);

    verify(model).addAttribute("type", "incoming-funds");
    verify(model).addAttribute("mainStat", dummyValue);
    verify(model)
        .addAttribute("secondStat", dummyValue.divide(BigDecimal.valueOf(30), BigDecimal.ROUND_UP));
    verify(model).addAttribute("stats", Collections.emptyList());
    verify(model).addAttribute("urlPage", "adminStatistics");

    assertEquals("statistics", view);
  }

  @Test
  void testIncomingFundsStatsWithZeroValue() {
    when(statisticsService.getCombinedIncomeFunds(30)).thenReturn(BigDecimal.ZERO);

    String view = controller.getPage(model, "incoming-funds", "30", redirectAttributes);

    verify(model).addAttribute("mainStat", 0);
    verify(model).addAttribute("secondStat", 0);
    assertEquals("statistics", view);
  }

  @Test
  void testOutcomingFundsStatsWithPositiveValue() {
    when(statisticsService.getCombinedOutgoingFunds(30)).thenReturn(dummyValue);

    String view = controller.getPage(model, "outcoming-funds", "30", redirectAttributes);

    verify(model).addAttribute("type", "outcoming-funds");
    verify(model).addAttribute("mainStat", dummyValue);
    verify(model)
        .addAttribute("secondStat", dummyValue.divide(BigDecimal.valueOf(30), BigDecimal.ROUND_UP));
    verify(model).addAttribute("stats", Collections.emptyList());
    verify(model).addAttribute("urlPage", "adminStatistics");

    assertEquals("statistics", view);
  }

  @Test
  void testOutcomingFundsStatsWithNullValue() {
    when(statisticsService.getCombinedOutgoingFunds(30)).thenReturn(null);

    String view = controller.getPage(model, "outcoming-funds", "30", redirectAttributes);

    verify(model).addAttribute("mainStat", 0);
    verify(model).addAttribute("secondStat", 0);
    assertEquals("statistics", view);
  }

  @Test
  void testDefaultDaysBack() {
    when(statisticsService.getCurrentBalance()).thenReturn(dummyValue);
    when(statisticsService.getAverageBalance()).thenReturn(dummyValue);

    String view = controller.getPage(model, "balance", null, redirectAttributes);

    verify(statisticsService).getStatisticsForTimeframe(Statistic.StatisticType.BALANCE, 90);
    assertEquals("statistics", view);
  }

  @Test
  void testInvalidDaysBack() {
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.getPage(model, "balance", "0", redirectAttributes));
  }

  @Test
  void testInvalidTypeThrowsException() {
    assertThrows(
        NoSuchElementException.class,
        () -> controller.getPage(model, "invalid-type", "30", redirectAttributes));
  }
}
