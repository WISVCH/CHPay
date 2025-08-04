package chpay.frontend.customer.controller;

import chpay.frontend.customer.service.AdminUserServiceImpl;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.UserService;
import chpay.shared.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdministratorUsersTablePageController extends PageController {
  private static final String MODEL_ATTR_USERS = "users";
  private static final String MODEL_ATTR_INFO = "info";
  private static final String MODEL_ATTR_PAGE_FILTERS = "pageFilters";
  private static final String MODEL_ATTR_SORTING_PARAMS = "sortingParams";
  private static final String MODEL_ATTR_SORTBY = "sortBy";
  private static final String MODEL_ATTR_ORDER = "order";

  private final AdminUserServiceImpl adminUserService;
  private final UserService userService;
  private final NotificationService notificationService;

  @Autowired
  protected AdministratorUsersTablePageController(
      AdminUserServiceImpl adminUserService,
      UserService userService,
      NotificationService notificationService) {
    super();
    this.adminUserService = adminUserService;
    this.userService = userService;
    this.notificationService = notificationService;
  }

  /**
   * Gets the page showing all users for the administrator and sort it.
   *
   * @param model of type Model
   * @param page page number for the list of transactions
   * @return String
   */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping(value = "/admin/users")
  public String getPage(
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam(defaultValue = "1") String page,
      @RequestParam(required = false) String balanceAfter,
      @RequestParam(required = false) String balanceBefore,
      @RequestParam(required = false) String emailQuery,
      @RequestParam(required = false) String nameQuery,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String order)
      throws JsonProcessingException, DateTimeParseException {
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminUsers");
    int pageNum;
    try {
      pageNum = Integer.parseInt(page);
    } catch (Exception ex) {
      pageNum = 1;
    }

    if (sortBy != null) {
      if (!sortBy.equals("name") && !sortBy.equals("openID") && !sortBy.equals("balance")) {
        sortBy = null;
      }
    }

    if (order != null) {
      if (!order.equals("desc") && !order.equals("asc")) {
        order = null;
      }
    }
    model.addAttribute(MODEL_ATTR_SORTBY, sortBy);
    model.addAttribute(MODEL_ATTR_ORDER, order);
    String sortingParam = "&sortBy=" + sortBy + "&order=" + order;
    model.addAttribute(MODEL_ATTR_SORTING_PARAMS, sortingParam);

    PaginationInfo paginationInfo;

    if (balanceAfter != null && balanceBefore != null) {
      // add the current filter setting to the context for better navigation
      model.addAttribute(
          MODEL_ATTR_PAGE_FILTERS,
          "&balanceAfter=" + balanceAfter + "&balanceBefore=" + balanceBefore);
      paginationInfo =
          adminUserService.getUserByBalancePageInfo(
              userService, pageNum, 4, balanceAfter, balanceBefore);
      model.addAttribute(
          MODEL_ATTR_USERS,
          adminUserService.getUsersByBalanceJSON(
              userService, paginationInfo, balanceAfter, balanceBefore, sortBy, order));
    } else if (emailQuery != null) {
      model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "&emailQuery=" + emailQuery);
      paginationInfo = adminUserService.getUserByEmailPageInfo(userService, pageNum, 4, emailQuery);
      model.addAttribute(
          MODEL_ATTR_USERS,
          adminUserService.getUsersByEmailJSON(
              userService, paginationInfo, emailQuery, sortBy, order));
    } else if (nameQuery != null) {
      // add the current filter setting to the context for better navigation
      model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "&nameQuery=" + nameQuery);
      paginationInfo = adminUserService.getUserByNamePageInfo(userService, pageNum, 4, nameQuery);
      model.addAttribute(
          MODEL_ATTR_USERS,
          adminUserService.getUsersByNameJSON(
              userService, paginationInfo, nameQuery, sortBy, order));
    } else {
      model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "");
      paginationInfo = adminUserService.getUserAllPageInfo(userService, pageNum, 4);
      model.addAttribute(
          MODEL_ATTR_USERS,
          adminUserService.getUsersAllJSON(userService, paginationInfo, sortBy, order));
    }

    model.addAttribute(MODEL_ATTR_INFO, paginationInfo);
    return "admin-user-table";
  }
}
