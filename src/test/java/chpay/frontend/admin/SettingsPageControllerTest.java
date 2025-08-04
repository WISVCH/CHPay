package chpay.frontend.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import chpay.paymentbackend.service.SettingService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

@ExtendWith(MockitoExtension.class)
class SettingsPageControllerTest {

  @Mock private SettingService settingService;

  @Mock private NotificationService notificationService;

  private MockMvc mockMvc;

  @InjectMocks private SettingsPageController controller;

  /** Initialize MockMvc with a view resolver for 'settings' and 'redirect:' views. */
  @BeforeEach
  void setup() {
    ViewResolver viewResolver =
        (viewName, locale) -> {
          if ("settings".equals(viewName)) {
            return new InternalResourceView("/WEB-INF/views/settings.html");
          }
          if (viewName != null && viewName.startsWith("redirect:")) {
            String url = viewName.substring("redirect:".length());
            return new RedirectView(url);
          }
          return null;
        };
    mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
  }

  /** Tests that GET /admin/settings returns the settings view with current values. */
  @Test
  void getSettings_displaysSettings() throws Exception {
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal("150.00"));
    when(settingService.isFrozen()).thenReturn(true);

    mockMvc
        .perform(get("/admin/settings"))
        .andExpect(view().name("settings"))
        .andExpect(model().attribute("maxBalance", new BigDecimal("150.00")))
        .andExpect(model().attribute("systemFrozen", true))
        .andExpect(model().attribute("urlPage", "systemSettings"));

    verify(settingService).getMaxBalance();
    verify(settingService).isFrozen();
  }

  /** Tests that POST with invalid balance redirects and sends an error message. */
  @Test
  void updateSettings_invalidBalance() throws Exception {
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal("100.00"));
    when(settingService.isFrozen()).thenReturn(false);

    mockMvc
        .perform(
            post("/admin/settings")
                .param("maximumBalance", "notANumber")
                .param("isFrozen", "false"))
        .andExpect(redirectedUrl("/admin/settings"));

    verify(notificationService).addErrorMessage(any(), eq("Invalid maximum balance"));
    verify(settingService, never()).setMaxBalance(any());
    verify(settingService, never()).setFrozen(anyBoolean());
  }

  /** Tests that POST with no changes redirects and sends an info message. */
  @Test
  void updateSettings_noChange() throws Exception {
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal("200.00"));
    when(settingService.isFrozen()).thenReturn(false);

    mockMvc.perform(post("/admin/settings")).andExpect(redirectedUrl("/admin/settings"));

    verify(notificationService).addInfoMessage(any(), eq("No settings were changed"));
    verify(settingService).getMaxBalance();
    verify(settingService).isFrozen();
    verify(settingService, never()).setMaxBalance(any());
    verify(settingService, never()).setFrozen(anyBoolean());
  }

  /** Tests that POST changing only the balance updates the setting and sends success. */
  @Test
  void updateSettings_changeBalanceOnly() throws Exception {
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal("100.00"));
    when(settingService.isFrozen()).thenReturn(false);

    mockMvc
        .perform(post("/admin/settings").param("maximumBalance", "250.00"))
        .andExpect(redirectedUrl("/admin/settings"));

    verify(settingService).setMaxBalance(new BigDecimal("250.00"));
    verify(settingService).setFrozen(false);
    verify(notificationService).addSuccessMessage(any(), eq("Settings updated successfully"));
  }

  /** Tests that POST changing only the frozen flag updates the setting and sends success. */
  @Test
  void updateSettings_changeFrozenOnly() throws Exception {
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal("300.00"));
    when(settingService.isFrozen()).thenReturn(false);

    mockMvc
        .perform(post("/admin/settings").param("maximumBalance", "").param("isFrozen", "true"))
        .andExpect(redirectedUrl("/admin/settings"));

    verify(settingService).setMaxBalance(new BigDecimal("300.00"));
    verify(settingService).setFrozen(true);
    verify(notificationService).addSuccessMessage(any(), eq("Settings updated successfully"));
  }

  /** Tests that POST changing both balance and frozen updates both and sends success. */
  @Test
  void updateSettings_changeBoth() throws Exception {
    when(settingService.getMaxBalance()).thenReturn(new BigDecimal("50.00"));
    when(settingService.isFrozen()).thenReturn(true);

    mockMvc
        .perform(
            post("/admin/settings").param("maximumBalance", "75.00").param("isFrozen", "false"))
        .andExpect(redirectedUrl("/admin/settings"));

    verify(settingService).setMaxBalance(new BigDecimal("75.00"));
    verify(settingService).setFrozen(false);
    verify(notificationService).addSuccessMessage(any(), eq("Settings updated successfully"));
  }
}
