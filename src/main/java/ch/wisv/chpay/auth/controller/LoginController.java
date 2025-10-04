package ch.wisv.chpay.auth.controller;

import ch.wisv.chpay.core.controller.PageController;
import ch.wisv.chpay.core.service.NotificationService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * HomeController is a Spring MVC controller that handles requests to various endpoints of the
 * application. It provides methods to direct users to login, dashboard, or error pages based on
 * specific conditions, such as authentication status or request parameters.
 *
 * <p>- The root endpoint ("/") redirects users to either the dashboard or login page depending on
 * their authentication status. - The dashboard endpoint ("/dashboard") serves the main dashboard
 * page upon successful login. - The login endpoint ("/login") serves the login form and optionally
 * shows a logout message when accessed with a "logout" parameter. - The error endpoint ("/error")
 * serves a generic error page. - The logout-success endpoint ("/logout-success") serves a page
 * confirming that logout has been completed successfully.
 */
@Controller
public class LoginController extends PageController {

  private final NotificationService notificationService;

  public LoginController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/login")
  public String loginPage(
      @RequestParam(value = "logout", required = false) String logout,
      @RequestParam(value = "error", required = false) String error,
      @RequestParam(value = "message", required = false) String errorMessage,
      Model model,
      RedirectAttributes redirectAttributes) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      return "redirect:/index";
    }

    if (logout != null) {
      model.addAttribute(MODEL_ATTR_LOGOUT_MESSAGE, "You have been successfully logged out.");
    }

    if (error != null) {
      // Use notification service for error message
      String message =
          errorMessage != null ? errorMessage : "Authentication failed. Please try again.";
      notificationService.addErrorMessage(redirectAttributes, message);
      return "redirect:/login";
    }

    return "login";
  }

  @GetMapping("/expired")
  public String expiredPage(RedirectAttributes redirectAttributes) {
    notificationService.addInfoMessage(
        redirectAttributes, "You have been banned/unbanned. Please contact support.");
    return "redirect:/login";
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
