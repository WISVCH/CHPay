package ch.wisv.chpay.customer.controller;


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
        return "redirect:/index";
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
