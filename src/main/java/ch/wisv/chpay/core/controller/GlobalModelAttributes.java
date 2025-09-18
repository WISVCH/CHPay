package ch.wisv.chpay.core.controller;

import ch.wisv.chpay.core.repository.UserRepository;
import ch.wisv.chpay.core.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

  private final UserRepository userRepository;
  private final SettingService settingService;

  @Autowired
  public GlobalModelAttributes(UserRepository userRepository, SettingService settingService) {
    this.userRepository = userRepository;
    this.settingService = settingService;
  }

  @ModelAttribute
  public void addCurrentUserToModel(Model model, Authentication authentication) {
    if (authentication != null) {
      if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
        String sub = oidcUser.getAttribute("sub");
        if (sub != null) {
          userRepository
              .findByOpenID(sub)
              .ifPresent(user -> model.addAttribute("currentUser", user));
        }

        if (oidcUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
          model.addAttribute("isAdmin", true);
        } else {
          model.addAttribute("isAdmin", false);
        }
      } else if (authentication
          .getAuthorities()
          .contains(new SimpleGrantedAuthority("ROLE_API_USER"))) {
        // API user authentication - no currentUser or admin status
        model.addAttribute("isAdmin", false);
      }
    }
    model.addAttribute("systemFrozen", settingService.isFrozen());
  }
}
