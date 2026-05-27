package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.model.LedPattern;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.repository.UserRepository;
import ch.wisv.chpay.core.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/led")
public class LedController extends CustomerController {

  private final UserRepository userRepository;
  private final NotificationService notificationService;

  @Autowired
  public LedController(UserRepository userRepository, NotificationService notificationService) {
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping
  public String showLedPage(@ModelAttribute("currentUser") User currentUser, Model model) {
    if (currentUser == null) {
      return "redirect:/login";
    }

    model.addAttribute("patterns", LedPattern.values());
    model.addAttribute(MODEL_ATTR_URL_PAGE, "led");
    return "led";
  }

  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @PostMapping("/save")
  @Transactional
  public String saveLedPreferences(
      @RequestParam("r") int r,
      @RequestParam("g") int g,
      @RequestParam("b") int b,
      @RequestParam("pattern") String pattern,
      @ModelAttribute("currentUser") User currentUser,
      RedirectAttributes redirectAttributes) {
    if (currentUser == null) {
      return "redirect:/login";
    }

    if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
      notificationService.addErrorMessage(redirectAttributes, "Invalid color values");
      return "redirect:/led";
    }

    LedPattern ledPattern;
    try {
      ledPattern = LedPattern.valueOf(pattern);
    } catch (IllegalArgumentException e) {
      notificationService.addErrorMessage(redirectAttributes, "Invalid pattern");
      return "redirect:/led";
    }

    User user = userRepository.findAndLockByOpenID(currentUser.getOpenID()).orElse(currentUser);
    user.setLedR(r);
    user.setLedG(g);
    user.setLedB(b);
    user.setLedPattern(ledPattern);
    userRepository.save(user);

    notificationService.addSuccessMessage(redirectAttributes, "LED preferences saved");
    return "redirect:/led";
  }
}
