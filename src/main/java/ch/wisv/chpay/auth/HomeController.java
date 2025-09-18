package ch.wisv.chpay.auth;

import ch.wisv.chpay.core.controller.PageController;
import ch.wisv.chpay.core.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
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
public class HomeController extends PageController {

  private final NotificationService notificationService;

  public HomeController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/")
  public String root() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {

      /* HOW TO ACCESS THE USER ATTRIBUTES:
      // Retreive principal and cast it to OAuth2User which is the type of authentication we use
      OAuth2User principal = (OAuth2User) authentication.getPrincipal();
      // principal will have getAttributes() method
      System.out.println("attributes: " + principal.getAttributes());
      */

      return "redirect:/index";
    }
    return "redirect:/login";
  }

  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping("/index")
  public String index() {
    return "index"; // Renders templates/index.html
  }

  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping("/dashboard")
  public String home() {
    return "dashboard"; // refers to templates/dashboard.html
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

  @GetMapping("/error")
  public String handleError() {
    return "error"; // refers to templates/error.html
  }

  @GetMapping("/logout-success")
  public String logoutSuccess(@RequestParam(required = false) Long ts) {

    System.out.println("Logout-success accessed with ts=" + ts);

    // If timestamp or token are missing, return forbidden http status
    if (ts == null) {
      // FORBIDDEN, redirect to error page with FORBIDDEN status
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Tried to access logout success page without logging out");
    }

    // Validate the timestamp is recent (within last 5 seconds)
    long currentTime = System.currentTimeMillis();
    long maxAge = 5000; // 5 seconds

    if (currentTime - ts > maxAge) {
      // Timestamp is too old, redirect to login
      return "redirect:/login";
    }

    // Valid logout-success access
    return "logout-success";
  }
}
