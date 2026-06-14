package ch.wisv.chpay.auth.controller;

import ch.wisv.chpay.core.controller.PageController;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HomeController is a Spring MVC controller that handles requests to various endpoints of the
 * application. It provides methods to direct users to login, dashboard, or error pages based on
 * specific conditions, such as authentication status or request parameters.
 *
 * <p>- The root endpoint ("/") redirects users to either the dashboard or login page depending on
 * their authentication status. - The dashboard endpoint ("/dashboard") serves the main dashboard
 * page upon successful login. - The login endpoint ("/login") starts SSO for normal requests and
 * serves the login page only for recovery states. - The error endpoint ("/error") serves a generic
 * error page. - The logout-success endpoint ("/logout-success") serves a page confirming that
 * logout has been completed successfully.
 */
@Controller
public class LoginController extends PageController {

  private static final String SSO_AUTHORIZATION_PATH = "/oauth2/authorization/wisvchconnect";
  private static final String NOTIFICATION_TYPE = "notificationType";
  private static final String NOTIFICATION_MESSAGE = "notificationMessage";

  @GetMapping("/login")
  public String loginPage(
      @RequestParam(value = "logout", required = false) String logout,
      @RequestParam(value = "error", required = false) String error,
      @RequestParam(value = "expired", required = false) String expired,
      Model model) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      return "redirect:/index";
    }

    if (logout != null) {
      model.addAttribute(MODEL_ATTR_LOGOUT_MESSAGE, "You have been successfully logged out.");
      return "login";
    }

    if (error != null) {
      model.addAttribute(NOTIFICATION_TYPE, "error");
      model.addAttribute(NOTIFICATION_MESSAGE, "Authentication failed. Please try again.");
      return "login";
    }

    if (expired != null) {
      model.addAttribute(NOTIFICATION_TYPE, "message");
      model.addAttribute(
          NOTIFICATION_MESSAGE, "You have been banned/unbanned. Please contact support.");
      return "login";
    }

    return "redirect:" + SSO_AUTHORIZATION_PATH;
  }

  @GetMapping("/expired")
  public String expiredPage() {
    return "redirect:/login?expired";
  }

  @GetMapping("/logout-success")
  public String logoutSuccess(Model model) {
    // Check if user is not authenticated (which indicates successful logout)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      // User is still authenticated, redirect to login
      return "redirect:/login";
    }

    // User is not authenticated, show logout success page
    return "logout-success";
  }
}
