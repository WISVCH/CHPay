package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.repository.UserRepository;
import ch.wisv.chpay.core.service.SettingService;
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
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final SettingService settingService;
  private final UserService userService;

  protected AdminDashboardController(
      TransactionRepository transactionRepository,
      UserRepository userRepository,
      SettingService settingService,
      UserService userService) {
    super();
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
    this.settingService = settingService;
    this.userService = userService;
  }

  @GetMapping
  public String adminPage(Model model, RedirectAttributes redirectAttributes) {
    model.addAttribute(MODEL_ATTR_URL_PAGE, "admin");
    model.addAttribute(MODEL_ATTR_USERS, userService.countAll());
    model.addAttribute(MODEL_ATTR_TRANSACTIONS, transactionRepository.count());
    model.addAttribute(MODEL_ATTR_STATUS, settingService.isFrozen() ? "Frozen" : "Active");
    model.addAttribute(MODEL_ATTR_MAX_BALANCE, settingService.getMaxBalance());
    model.addAttribute(MODEL_ATTR_BALANCE, userRepository.getBalanceNow());
    model.addAttribute(MODEL_ATTR_INCOMING, 0); // Removed statistics functionality
    model.addAttribute(MODEL_ATTR_OUTGOING, 0); // Removed statistics functionality
    return "admin";
  }
}
