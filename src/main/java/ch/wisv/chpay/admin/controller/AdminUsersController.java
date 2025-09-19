package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.UserService;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/users")
public class AdminUsersController extends AdminController {

  private final UserService userService;
  private final NotificationService notificationService;

  @Autowired
  protected AdminUsersController(UserService userService, NotificationService notificationService) {
    super();
    this.userService = userService;
    this.notificationService = notificationService;
  }

  /**
   * Gets the page showing all users for the administrator and sort it.
   *
   * @param model of type Model
   * @return String
   */
  @GetMapping
  public String getPage(RedirectAttributes redirectAttributes, Model model)
      throws DateTimeParseException {

    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminUsers");
    model.addAttribute(MODEL_ATTR_USERS, userService.getAllUsers());
    return "admin-user-table";
  }
}
