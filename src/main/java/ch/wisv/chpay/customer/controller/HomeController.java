package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.controller.PageController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

  @GetMapping("/")
  public String root(Authentication authentication) {
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      return "redirect:/index";
    }
    return "login";
  }

  @PreAuthorize("hasAnyRole('USER', 'BANNED')")
  @GetMapping("/index")
  public String index() {
    return "index"; // Renders templates/index.html
  }

  @GetMapping("/error")
  public String handleError() {
    return "error"; // refers to templates/error.html
  }
}
