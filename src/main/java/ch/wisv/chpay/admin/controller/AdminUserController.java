package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.model.BalanceEntry;
import ch.wisv.chpay.admin.service.AdminUserService;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.service.UserService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/user")
public class AdminUserController extends AdminController {

  private final UserService userService;
  private final AdminUserService adminUserService;

  @Autowired
  protected AdminUserController(UserService userService, AdminUserService adminUserService) {
    super();
    this.userService = userService;
    this.adminUserService = adminUserService;
  }

  @Autowired private SessionRegistry sessionRegistry;

  /**
   * Displays the user page based on its ID for the administrator user.
   *
   * @param model the Model object to add attributes for the view
   * @param userKey the unique identifier of the user in String format
   * @param redirectAttributes the RedirectAttributes object used for passing flash attributes
   * @return the name of the view to render the user page
   */
  @GetMapping(value = "/{userKey}")
  public String showUserPage(
      Model model, @PathVariable String userKey, RedirectAttributes redirectAttributes) {
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminUsers");
    User user = userService.getUserById(userKey);
    if (user == null) {
      throw new NoSuchElementException("User not found");
    }
    model.addAttribute(MODEL_ATTR_USER, user);
    return "admin-user";
  }

  @GetMapping(value = "/{userKey}/stats")
  public String showUserStatPage(
      Model model, @PathVariable String userKey, RedirectAttributes redirectAttributes) {
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminUsers");

    // get the user instance
    User user = userService.getUserById(userKey);
    if (user == null) {
      throw new NoSuchElementException("User not found");
    }
    model.addAttribute(MODEL_ATTR_USER, user);

    // Calculate balance over time using AdminUserService
    List<BalanceEntry> balanceHistory = adminUserService.calculateUserBalanceHistory(user);
    model.addAttribute("balanceHistory", balanceHistory);

    // Enable charts for this page
    model.addAttribute("hasCharts", true);

    return "admin-user-stats";
  }

  /**
   * Bans a user with the specified id
   *
   * @param id the user's id
   * @return back to the user's page
   */
  @Transactional
  @PostMapping(value = "/{id}/ban")
  public String banUser(@PathVariable("id") UUID id) {
    User user = userService.getUserByIdForUpdate(id);
    if (user == null) {
      throw new NoSuchElementException("User not found");
    }
    user.setBanned(!user.getBanned());
    userService.saveAndFlush(user);

    List<Object> principals = sessionRegistry.getAllPrincipals();
    for (Object principal : principals) {
      if (principal instanceof DefaultOidcUser oidcUser) {
        String subject = oidcUser.getSubject();
        if (subject.equals(user.getOpenID())) {
          List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
          for (SessionInformation sessionInfo : sessions) {
            sessionInfo.expireNow();
          }
        }
      }
    }
    return "redirect:/admin/user/" + user.getId().toString();
  }
}
