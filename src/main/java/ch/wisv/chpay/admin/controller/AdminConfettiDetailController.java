package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminConfettiService;
import ch.wisv.chpay.admin.service.ConfettiFormParser;
import ch.wisv.chpay.admin.service.ConfettiFormParser.ConfettiFormResult;
import ch.wisv.chpay.core.model.Confetti;
import ch.wisv.chpay.core.service.NotificationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/confetti/{id}")
public class AdminConfettiDetailController extends AdminController {

  private final AdminConfettiService adminConfettiService;
  private final ConfettiFormParser confettiFormParser;
  private final NotificationService notificationService;

  @Autowired
  public AdminConfettiDetailController(
      AdminConfettiService adminConfettiService,
      ConfettiFormParser confettiFormParser,
      NotificationService notificationService) {
    this.adminConfettiService = adminConfettiService;
    this.confettiFormParser = confettiFormParser;
    this.notificationService = notificationService;
  }

  @GetMapping
  public String showConfetti(@PathVariable("id") UUID id, Model model, RedirectAttributes ra) {
    Optional<Confetti> confetti = adminConfettiService.getById(id);
    if (confetti.isEmpty()) {
      notificationService.addErrorMessage(ra, "Confetti not found");
      return "redirect:/admin/confetti";
    }

    Confetti current = confetti.get();
    model.addAttribute(MODEL_ATTR_CONFETTI, current);
    model.addAttribute("confettiUserCount", adminConfettiService.getUsageCount(current));
    model.addAttribute("colorValues", current.getColors());
    model.addAttribute("scalarValue", current.getScalar());
    model.addAttribute("minTransactionsValue", current.getMinimumTransactions());
    model.addAttribute("groupValue", current.getGroup());
    model.addAttribute("groupStartsWithValue", current.isGroupStartsWith());
    model.addAttribute("hiddenValue", current.isHidden());
    model.addAttribute("defaultValue", current.isDefaultConfetti());

    model.addAttribute(
        "shapeTypes", current.getShapes().stream().map(shape -> shape.getType().name()).toList());
    model.addAttribute(
        "shapeValues",
        current.getShapes().stream()
            .map(shape -> shape.getValue() == null ? "" : shape.getValue().trim())
            .toList());

    model.addAttribute("ledColorValue", current.getLedColor());
    model.addAttribute("ledPatternValue", current.getLedPattern().name());
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminConfetti");
    return "admin-confetti";
  }

  @PostMapping
  public String updateConfetti(
      @PathVariable("id") UUID id,
      @RequestParam("name") String name,
      @RequestParam(value = "colors", required = false) List<String> colorsInput,
      @RequestParam("scalar") String scalarInput,
      @RequestParam("minTransactions") String minTransactionsInput,
      @RequestParam("group") String groupInput,
      @RequestParam(value = "groupStartsWith", defaultValue = "false") boolean groupStartsWith,
      @RequestParam(value = "hidden", defaultValue = "false") boolean hidden,
      @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault,
      @RequestParam(value = "shapeType", required = false) List<String> shapeTypes,
      @RequestParam(value = "shapeValue", required = false) List<String> shapeValues,
      @RequestParam("ledColor") String ledColor,
      @RequestParam("ledPattern") String ledPattern,
      RedirectAttributes redirectAttributes) {

    Optional<Confetti> confetti = adminConfettiService.getById(id);
    if (confetti.isEmpty()) {
      notificationService.addErrorMessage(redirectAttributes, "Confetti not found");
      return "redirect:/admin/confetti";
    }

    ConfettiFormResult result =
        confettiFormParser.parse(
            name,
            colorsInput,
            scalarInput,
            minTransactionsInput,
            groupInput,
            groupStartsWith,
            hidden,
            isDefault,
            shapeTypes,
            shapeValues,
            ledColor,
            ledPattern);
    if (!result.isValid()) {
      notificationService.addErrorMessage(redirectAttributes, result.getErrorMessage());
      return "redirect:/admin/confetti/" + id;
    }

    try {
      adminConfettiService.update(
          confetti.get(),
          result.getName(),
          result.getShapes(),
          result.getColors(),
          result.getScalar(),
          result.getMinTransactions(),
          result.getGroup(),
          result.isGroupStartsWith(),
          result.isHidden(),
          result.isDefault(),
          result.getLedColor(),
          result.getLedPattern());
    } catch (IllegalStateException ex) {
      notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
      return "redirect:/admin/confetti/" + id;
    }
    notificationService.addSuccessMessage(redirectAttributes, "Confetti updated successfully");
    return "redirect:/admin/confetti/" + id;
  }

  @PostMapping("/delete")
  public String deleteConfetti(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
    Optional<Confetti> confetti = adminConfettiService.getById(id);
    if (confetti.isEmpty()) {
      notificationService.addErrorMessage(redirectAttributes, "Confetti not found");
      return "redirect:/admin/confetti";
    }

    try {
      adminConfettiService.delete(confetti.get());
    } catch (IllegalStateException ex) {
      notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
      return "redirect:/admin/confetti/" + id;
    }
    notificationService.addSuccessMessage(redirectAttributes, "Confetti deleted successfully");
    return "redirect:/admin/confetti";
  }
}
