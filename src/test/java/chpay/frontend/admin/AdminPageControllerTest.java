package chpay.frontend.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.Statistic;
import chpay.frontend.customer.controller.AdminPageController;
import chpay.paymentbackend.service.SettingService;
import chpay.paymentbackend.service.StatisticsService;
import chpay.paymentbackend.service.UserService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class AdminPageControllerTest {

  @Mock private StatisticsService statisticsService;

  @Mock private NotificationService notificationService;

  @Mock private SettingService settingService;

  @Mock private UserService userService;

  @Mock private Model model;

  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private AdminPageController adminPageController;

  @BeforeEach
  void setUp() {
    when(userService.countAll()).thenReturn(10L);
    when(statisticsService.getTransactionCount()).thenReturn(200L);
    when(settingService.isFrozen()).thenReturn(false);
    when(settingService.getMaxBalance()).thenReturn(BigDecimal.valueOf(5000.0));
    when(statisticsService.getCurrentBalance()).thenReturn(BigDecimal.valueOf(3500.0));
    when(statisticsService.getLastMonth(Statistic.StatisticType.INCOMING_FUNDS))
        .thenReturn(BigDecimal.valueOf(1200.0));
    when(statisticsService.getLastMonth(Statistic.StatisticType.OUTGOING_FUNDS))
        .thenReturn(BigDecimal.valueOf(800.0));
  }

  @Test
  void shouldReturnAdminViewAndPopulateModelCorrectly() {
    String viewName = adminPageController.adminPage(model, redirectAttributes);

    assertEquals("admin", viewName);

    verify(model).addAttribute(eq("urlPage"), eq("admin"));
    verify(model).addAttribute(eq("users"), eq(10L));
    verify(model).addAttribute(eq("transactions"), eq(200L));
    verify(model).addAttribute(eq("status"), eq("Active"));
    verify(model).addAttribute(eq("maxBalance"), eq(BigDecimal.valueOf(5000.0)));
    verify(model).addAttribute(eq("balanceAvailable"), eq(BigDecimal.valueOf(3500.0)));
    verify(model).addAttribute(eq("incomingFunds"), eq(BigDecimal.valueOf(1200.0)));
    verify(model).addAttribute(eq("outgoingFunds"), eq(BigDecimal.valueOf(800.0)));
  }
}
