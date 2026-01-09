package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminConfettiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/confetti")
public class AdminConfettiController extends AdminController {

  private final AdminConfettiService adminConfettiService;

  @Autowired
  public AdminConfettiController(AdminConfettiService adminConfettiService) {
    this.adminConfettiService = adminConfettiService;
  }

  @GetMapping
  public String showConfettiOverview(Model model) {
    model.addAttribute(MODEL_ATTR_CONFETTIS, adminConfettiService.getAll());
    model.addAttribute("confettiUsageCounts", adminConfettiService.getUsageCounts());
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminConfetti");
    return "admin-confetti-table";
  }
}
