package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.core.model.Statistic;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.StatisticsService;
import ch.wisv.chpay.core.service.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
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
  private final StatisticsService statisticsService;
  private final NotificationService notificationService;

  @Autowired
  protected AdminUserController(
      UserService userService,
      NotificationService notificationService,
      StatisticsService statisticsService) {
    super();
    this.userService = userService;
    this.notificationService = notificationService;
    this.statisticsService = statisticsService;
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

  @GetMapping(value = "/{userKey}/stats/{type}")
  public String showUserStatPage(
      Model model,
      @PathVariable String userKey,
      @PathVariable String type,
      @RequestParam(required = false) String daysBack,
      RedirectAttributes redirectAttributes) {
    // get how back the stats extend
    int days = 90;
    if (daysBack != null) {
      days = Integer.parseInt(daysBack);
    }
    if (days < 1) {
      throw new IllegalArgumentException("Days cannot be less than 1");
    }

    // get the user instance
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminUserStats");
    User user = userService.getUserById(userKey);
    if (user == null) {
      throw new NoSuchElementException("User not found");
    }
    model.addAttribute(MODEL_ATTR_USER, user);

    // generate the statistics for the user
    List<Statistic> stats = new ArrayList<>();
    if (type.equals("incoming-funds")) {

      model.addAttribute(MODEL_ATTR_TYPE, "incoming-funds");
      stats = statisticsService.getIncomeFundsPerUser(days, user);

    } else if (type.equals("outcoming-funds")) {

      model.addAttribute(MODEL_ATTR_TYPE, "outcoming-funds");
      stats = statisticsService.getOutgoingFundsPerUser(days, user);

    } else {
      throw new NoSuchElementException("Statistic not found");
    }
    BigDecimal averageFunds = statisticsService.getAverageMonthlyFundsPerUser(stats, days);

    if (averageFunds != null && averageFunds.compareTo(BigDecimal.ZERO) > 0) {
      model.addAttribute(MODEL_ATTR_MAIN_STAT, averageFunds);
    } else {
      model.addAttribute(MODEL_ATTR_MAIN_STAT, 0);
    }

    model.addAttribute(MODEL_ATTR_STATS, stats);
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
