package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminConfettiService;
import ch.wisv.chpay.admin.service.ConfettiFormParser;
import ch.wisv.chpay.admin.service.ConfettiFormParser.ConfettiFormResult;
import ch.wisv.chpay.core.model.Confetti;
import ch.wisv.chpay.core.model.LedPattern;
import ch.wisv.chpay.core.service.NotificationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/confetti/new")
public class AdminConfettiCreateController extends AdminController {

  private final AdminConfettiService adminConfettiService;
  private final ConfettiFormParser confettiFormParser;
  private final NotificationService notificationService;

  @Autowired
  public AdminConfettiCreateController(
      AdminConfettiService adminConfettiService,
      ConfettiFormParser confettiFormParser,
      NotificationService notificationService) {
    this.adminConfettiService = adminConfettiService;
    this.confettiFormParser = confettiFormParser;
    this.notificationService = notificationService;
  }

  @GetMapping
  public String showCreateForm(Model model) {
    model.addAttribute("confettiName", "");
    model.addAttribute("colorValues", List.of("#ff0000"));
    model.addAttribute("scalarValue", Confetti.DEFAULT_SCALAR);
    model.addAttribute("minTransactionsValue", 0);
    model.addAttribute("groupValue", "");
    model.addAttribute("groupStartsWithValue", false);
    model.addAttribute("hiddenValue", false);
    model.addAttribute("defaultValue", false);
    model.addAttribute("shapeTypes", List.of("SQUARE"));
    model.addAttribute("shapeValues", List.of(""));
    model.addAttribute("ledColorValue", "#ffffff");
    model.addAttribute("ledPatternValue", LedPattern.oplopen.name());
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminConfetti");
    return "admin-confetti-create";
  }

  @PostMapping
  public String createConfetti(
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
      return "redirect:/admin/confetti/new";
    }

    Confetti confetti =
        adminConfettiService.create(
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
    notificationService.addSuccessMessage(redirectAttributes, "Confetti created successfully");
    return "redirect:/admin/confetti/" + confetti.getId();
  }
}
