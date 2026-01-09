package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.model.Confetti;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.repository.ConfettiRepository;
import ch.wisv.chpay.core.repository.UserRepository;
import ch.wisv.chpay.core.service.ConfettiEligibilityService;
import ch.wisv.chpay.core.service.NotificationService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/confetti")
public class ConfettiController extends CustomerController {

  private final ConfettiRepository confettiRepository;
  private final UserRepository userRepository;
  private final ConfettiEligibilityService confettiEligibilityService;
  private final NotificationService notificationService;

  @Autowired
  public ConfettiController(
      ConfettiRepository confettiRepository,
      UserRepository userRepository,
      ConfettiEligibilityService confettiEligibilityService,
      NotificationService notificationService) {
    this.confettiRepository = confettiRepository;
    this.userRepository = userRepository;
    this.confettiEligibilityService = confettiEligibilityService;
    this.notificationService = notificationService;
  }

  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping
  public String showConfettiPage(@ModelAttribute("currentUser") User currentUser, Model model) {
    if (currentUser == null) {
      return "redirect:/login";
    }

    long eligibleTransactions = confettiEligibilityService.countEligibleTransactions(currentUser);

    List<ConfettiCard> cards =
        confettiRepository.findAll().stream()
            .map(
                confetti ->
                    new ConfettiCard(
                        confetti,
                        confettiEligibilityService.isConfettiUnlockedForUser(
                            confetti, eligibleTransactions, currentUser.getGroups())))
            .filter(card -> card.unlocked || !card.confetti.isHidden())
            .sorted(
                Comparator.comparingInt((ConfettiCard card) -> card.confetti.getMinimumTransactions())
                    .thenComparing(
                        card -> normalizeGroup(card.confetti),
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(card -> card.confetti.getName()))
            .toList();

    UUID selectedConfettiId =
        currentUser.getConfetti() != null ? currentUser.getConfetti().getId() : null;

    model.addAttribute("confettiCards", cards);
    model.addAttribute("selectedConfettiId", selectedConfettiId);
    model.addAttribute(MODEL_ATTR_URL_PAGE, "confetti");
    return "confetti";
  }

  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @PostMapping("/{id}/equip")
  @Transactional
  public String equipConfetti(
      @PathVariable("id") UUID id,
      @ModelAttribute("currentUser") User currentUser,
      RedirectAttributes redirectAttributes) {
    if (currentUser == null) {
      return "redirect:/login";
    }

    Optional<Confetti> confetti = confettiRepository.findById(id);
    if (confetti.isEmpty()) {
      notificationService.addErrorMessage(redirectAttributes, "Confetti not found");
      return "redirect:/confetti";
    }

    long eligibleTransactions = confettiEligibilityService.countEligibleTransactions(currentUser);
    if (!confettiEligibilityService.isConfettiUnlockedForUser(
        confetti.get(), eligibleTransactions, currentUser.getGroups())) {
      notificationService.addErrorMessage(redirectAttributes, "Confetti is locked");
      return "redirect:/confetti";
    }

    User user =
        userRepository.findAndLockByOpenID(currentUser.getOpenID()).orElse(currentUser);
    user.setConfetti(confetti.get());
    userRepository.save(user);

    notificationService.addSuccessMessage(redirectAttributes, "Confetti equipped");
    return "redirect:/confetti";
  }

  static final class ConfettiCard {
    private final Confetti confetti;
    private final boolean unlocked;

    private ConfettiCard(Confetti confetti, boolean unlocked) {
      this.confetti = confetti;
      this.unlocked = unlocked;
    }

    public Confetti getConfetti() {
      return confetti;
    }

    public boolean isUnlocked() {
      return unlocked;
    }
  }

  private static String normalizeGroup(Confetti confetti) {
    if (confetti == null) {
      return null;
    }
    String group = confetti.getGroup();
    if (group == null || group.isBlank()) {
      return null;
    }
    return group;
  }
}
