package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.SettingService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for the system settings page. Handles displaying and updating system settings like
 * freeze status and maximum balance.
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(value = "/admin/settings")
public class AdminSettingsController extends AdminController {

  private final SettingService settingService;
  private final NotificationService notificationService;

  @Autowired
  public AdminSettingsController(
      SettingService settingService, NotificationService notificationService) {
    this.settingService = settingService;
    this.notificationService = notificationService;
  }

  /**
   * Displays the settings page with current system settings.
   *
   * @param model the model to add attributes to
   * @return the view name for the settings page
   */
  @GetMapping
  public String showSettingsPage(Model model) {
    // Add current settings to the model
    model.addAttribute(MODEL_ATTR_MAX_BALANCE, settingService.getMaxBalance());
    model.addAttribute("systemFrozen", settingService.isFrozen());
    model.addAttribute(MODEL_ATTR_URL_PAGE, "systemSettings");
    return "settings";
  }

  /**
   * Handles the form submission to update system settings. Always saves both settings, using
   * current values if no new input is provided.
   *
   * @param maximumBalance the new maximum balance (optional)
   * @param isFrozen the system freeze status (optional)
   * @param ra for flash attributes
   * @return redirect to the settings page
   */
  @PostMapping
  public String updateSettings(
      @RequestParam(required = false) String maximumBalance,
      @RequestParam(name = "isFrozen", defaultValue = "false") boolean isFrozen,
      RedirectAttributes ra) {

    BigDecimal currentMax = settingService.getMaxBalance();
    boolean currentFrozen = settingService.isFrozen();

    BigDecimal newMax = currentMax;
    if (maximumBalance != null && !maximumBalance.isBlank()) {
      try {
        newMax = new BigDecimal(maximumBalance);
      } catch (NumberFormatException ex) {
        notificationService.addErrorMessage(ra, "Invalid maximum balance");
        return "redirect:/admin/settings";
      }
    }

    if (newMax.compareTo(currentMax) == 0 && isFrozen == currentFrozen) {
      notificationService.addInfoMessage(ra, "No settings were changed");
      return "redirect:/admin/settings";
    }

    settingService.setMaxBalance(newMax);
    settingService.setFrozen(isFrozen);

    notificationService.addSuccessMessage(ra, "Settings updated successfully");
    return "redirect:/admin/settings";
  }
}
