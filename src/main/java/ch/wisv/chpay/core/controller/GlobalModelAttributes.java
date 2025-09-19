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
  public void addGlobalAttributes(Model model, Authentication authentication) {
    addCurrentUser(model, authentication);
    addAdminStatus(model, authentication);
    addSystemStatus(model);
  }

  private void addCurrentUser(Model model, Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
      String sub = oidcUser.getAttribute("sub");
      if (sub != null) {
        userRepository
            .findByOpenID(sub)
            .ifPresent(user -> model.addAttribute(PageController.MODEL_ATTR_CURRENT_USER, user));
      }
    }
  }

  private void addAdminStatus(Model model, Authentication authentication) {
    if (authentication != null) {
      if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
        boolean isAdmin =
            oidcUser.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        model.addAttribute(PageController.MODEL_ATTR_IS_ADMIN, isAdmin);
      } else if (authentication
          .getAuthorities()
          .contains(new SimpleGrantedAuthority("ROLE_API_USER"))) {
        model.addAttribute(PageController.MODEL_ATTR_IS_ADMIN, false);
      }
    }
  }

  private void addSystemStatus(Model model) {
    model.addAttribute(PageController.MODEL_ATTR_SYSTEM_FROZEN, settingService.isFrozen());
  }
}
