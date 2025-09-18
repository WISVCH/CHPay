package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.admin.service.AdminTransactionService;
import ch.wisv.chpay.core.dto.PaginationInfo;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.service.NotificationService;
import ch.wisv.chpay.core.service.TransactionService;
import ch.wisv.chpay.core.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/transactions")
public class AdminTransactionsController extends AdminController {

  private final AdminTransactionService adminTransactionService;
  private final TransactionService transactionsService;
  private final UserService userService;
  private final NotificationService notificationService;

  @Autowired
  protected AdminTransactionsController(
      AdminTransactionService adminTransactionService,
      TransactionService transactionsService,
      UserService userService,
      NotificationService notificationService) {
    super();
    this.adminTransactionService = adminTransactionService;
    this.transactionsService = transactionsService;
    this.userService = userService;
    this.notificationService = notificationService;
  }

  /**
   * Gets the page showing all transactions for the administrator and sort it.
   *
   * @param model of type Model
   * @param page page number for the list of transactions
   * @return String
   */
  @GetMapping
  public String getPage(
      RedirectAttributes redirectAttributes,
      Model model,
      @RequestParam(defaultValue = "1") String page,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(required = false) String startPrice,
      @RequestParam(required = false) String endPrice,
      @RequestParam(required = false) String userOpenIdQuery,
      @RequestParam(required = false) String userNameQuery,
      @RequestParam(required = false) String descQuery,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "timestamp") String sortBy,
      @RequestParam(defaultValue = "desc") String order)
      throws JsonProcessingException, DateTimeParseException {

    int pageNum;
    try {
      pageNum = Integer.parseInt(page);
    } catch (Exception ex) {
      pageNum = 1;
    }

    // verify that the sort and order params are in order
    if (!sortBy.equals("timestamp") && !sortBy.equals("amount") && !sortBy.equals("description")) {
      sortBy = "timestamp";
    }

    if (!order.equals("desc") && !order.equals("asc")) {
      order = "desc";
    }
    model.addAttribute(MODEL_ATTR_SORTBY, sortBy);
    model.addAttribute(MODEL_ATTR_ORDER, order);
    String sortingParam = "&sortBy=" + sortBy + "&order=" + order;
    model.addAttribute(MODEL_ATTR_SORTING_PARAMS, sortingParam);

    PaginationInfo paginationInfo;

    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminTransactions");
    if (startDate != null || endDate != null) {
      // add the current filter setting to the context for better navigation
      model.addAttribute(
          MODEL_ATTR_PAGE_FILTERS, "&startDate=" + startDate + "&endDate=" + endDate);
      // check for if the dates have been defined and set a default date if they haven't been
      if (startDate == null || startDate.equals("undefined") || startDate.equals("null")) {
        // by default make the starting date 1970-1-1
        startDate = "1970-01-01";
      }
      if (endDate == null || endDate.equals("undefined") || endDate.equals("null")) {
        // search for transactions plus 1 day to account for any possible timezone issue
        endDate = LocalDate.now().plusDays(1).toString();
      }
      paginationInfo =
          adminTransactionService.getTransactionByDatePageInfo(
              transactionsService, pageNum, 4, startDate, endDate);
      model.addAttribute(
          MODEL_ATTR_TRANSACTIONS,
          adminTransactionService.getTransactionsByDateJSON(
              transactionsService, paginationInfo, startDate, endDate, sortBy, order));
    } else if (startPrice != null && endPrice != null) {
      // add the current filter setting to the context for better navigation
      model.addAttribute(
          MODEL_ATTR_PAGE_FILTERS, "&startPrice=" + startPrice + "&endPrice=" + endPrice);
      paginationInfo =
          adminTransactionService.getTransactionByAmountPageInfo(
              transactionsService, pageNum, 4, startPrice, endPrice);
      model.addAttribute(
          MODEL_ATTR_TRANSACTIONS,
          adminTransactionService.getTransactionsByAmountJSON(
              transactionsService, paginationInfo, startPrice, endPrice, sortBy, order));
    } else if (userOpenIdQuery != null || userNameQuery != null) {
      // add the current filter setting to the context for better navigation
      List<User> users = new ArrayList<>();
      if (userNameQuery != null) {
        model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "&userNameQuery=" + userNameQuery);
        users = userService.getUsersByNameQuery(userNameQuery);
      } else {
        model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "&userOpenIdQuery=" + userOpenIdQuery);
        users = userService.getUsersByOpenIdQuery(userOpenIdQuery);
      }
      paginationInfo =
          adminTransactionService.getTransactionByUsersInPageInfo(
              transactionsService, pageNum, 4, users);
      model.addAttribute(
          MODEL_ATTR_TRANSACTIONS,
          adminTransactionService.getTransactionsByUsersJSON(
              transactionsService, paginationInfo, users, sortBy, order));
    } else if (descQuery != null) {
      // add the current filter setting to the context for better navigation
      model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "&descQuery=" + descQuery);
      paginationInfo =
          adminTransactionService.getTransactionByDescriptionPageInfo(
              transactionsService, pageNum, 4, descQuery);
      model.addAttribute(
          MODEL_ATTR_TRANSACTIONS,
          adminTransactionService.getTransactionsByDescriptionJSON(
              transactionsService, paginationInfo, descQuery, sortBy, order));
    } else if (type != null || status != null) {
      // add the current filter setting to the context for better navigation
      model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "&type=" + type + "&status=" + status);
      paginationInfo =
          adminTransactionService.getTransactionByTypeAndStatusPageInfo(
              transactionsService, pageNum, 4, type, status);
      model.addAttribute(
          MODEL_ATTR_TRANSACTIONS,
          adminTransactionService.getTransactionsByTypeAndStatusJSON(
              transactionsService, paginationInfo, type, status, sortBy, order));
    } else {
      model.addAttribute(MODEL_ATTR_PAGE_FILTERS, "");
      paginationInfo =
          adminTransactionService.getTransactionPageInfo(transactionsService, pageNum, 4);
      model.addAttribute(
          MODEL_ATTR_TRANSACTIONS,
          adminTransactionService.getTransactionsAllJSON(
              transactionsService, paginationInfo, sortBy, order));
    }

    model.addAttribute(MODEL_ATTR_INFO, paginationInfo);
    return "admin-transaction-table";
  }
}
