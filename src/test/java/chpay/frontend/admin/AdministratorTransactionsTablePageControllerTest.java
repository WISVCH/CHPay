package chpay.frontend.admin;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import chpay.frontend.customer.controller.AdministratorTransactionsTablePageController;
import chpay.frontend.customer.service.AdminTransactionServiceImpl;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.TransactionService;
import chpay.paymentbackend.service.UserService;
import chpay.shared.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class AdministratorTransactionsTablePageControllerTest {
  private static final String MODEL_ATTR_SORTBY = "sortBy";
  private static final String MODEL_ATTR_ORDER = "order";
  @Mock private AdminTransactionServiceImpl adminTransactionService;
  @Mock private TransactionService transactionService;
  @Mock private UserService userService;
  @Mock private NotificationService notificationService;
  @Mock private Model model;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private AdministratorTransactionsTablePageController controller;

  private PaginationInfo paginationInfo;

  @BeforeEach
  void setup() {
    paginationInfo = new PaginationInfo(1, 1, false, false, 1);
  }

  @Test
  void testDateFilterBranch() throws JsonProcessingException, DateTimeParseException {
    controller.getPage(
        redirectAttributes,
        model,
        "1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "timestamp",
        "desc");
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "timestamp");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "desc");
  }

  @Test
  void testPageNumException() throws JsonProcessingException, DateTimeParseException {
    controller.getPage(
        redirectAttributes,
        model,
        "shouldntwork",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "timestamp",
        "desc");
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "timestamp");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "desc");
  }

  @Test
  void testBadOrderAndType() throws JsonProcessingException, DateTimeParseException {
    controller.getPage(
        redirectAttributes,
        model,
        "shouldntwork",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "untimestamp",
        "undesc");
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "timestamp");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "desc");
  }

  @Test
  void testBadOrderAndType3() throws JsonProcessingException, DateTimeParseException {
    controller.getPage(
        redirectAttributes,
        model,
        "shouldntwork",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "description",
        "undesc");
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "description");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "desc");
  }

  @Test
  void testBadOrderAndType4() throws JsonProcessingException, DateTimeParseException {
    controller.getPage(
        redirectAttributes,
        model,
        "shouldntwork",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "amount",
        "undesc");
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "amount");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "desc");
  }

  @Test
  void testBadOrderAndType2() throws JsonProcessingException, DateTimeParseException {
    controller.getPage(
        redirectAttributes,
        model,
        "shouldntwork",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "timestamp",
        "asc");
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "timestamp");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "asc");
  }

  @Test
  void testProper() throws JsonProcessingException, DateTimeParseException {
    controller.getPage(
        redirectAttributes,
        model,
        "shouldntwork",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "timestamp",
        "asc");
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "timestamp");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "asc");
  }

  @Test
  void testDateRangeFilter() throws JsonProcessingException, DateTimeParseException {
    when(adminTransactionService.getTransactionByDatePageInfo(
            transactionService, 1, 4, "2024-01-01", "2024-01-31"))
        .thenReturn(paginationInfo);
    when(adminTransactionService.getTransactionsByDateJSON(
            transactionService, paginationInfo, "2024-01-01", "2024-01-31", "timestamp", "desc"))
        .thenReturn(Collections.singletonList("[]"));

    controller.getPage(
        redirectAttributes,
        model,
        "1",
        "2024-01-01",
        "2024-01-31",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "timestamp",
        "desc");

    verify(model).addAttribute("pageFilters", "&startDate=2024-01-01&endDate=2024-01-31");
    verify(model).addAttribute("transactions", Collections.singletonList("[]"));
  }

  @Test
  void testPriceRangeFilter() throws JsonProcessingException, DateTimeParseException {
    when(adminTransactionService.getTransactionByAmountPageInfo(
            transactionService, 1, 4, "10", "100"))
        .thenReturn(paginationInfo);
    when(adminTransactionService.getTransactionsByAmountJSON(
            transactionService, paginationInfo, "10", "100", "timestamp", "desc"))
        .thenReturn(Collections.singletonList("[]"));

    controller.getPage(
        redirectAttributes,
        model,
        "1",
        null,
        null,
        "10",
        "100",
        null,
        null,
        null,
        null,
        null,
        "timestamp",
        "desc");

    verify(model).addAttribute("pageFilters", "&startPrice=10&endPrice=100");
    verify(model).addAttribute("transactions", Collections.singletonList("[]"));
  }

  @Test
  void testUserNameQueryFilter() throws JsonProcessingException, DateTimeParseException {
    when(userService.getUsersByNameQuery("Alice")).thenReturn(Collections.emptyList());
    when(adminTransactionService.getTransactionByUsersInPageInfo(
            transactionService, 1, 4, Collections.emptyList()))
        .thenReturn(paginationInfo);
    when(adminTransactionService.getTransactionsByUsersJSON(
            transactionService, paginationInfo, Collections.emptyList(), "timestamp", "desc"))
        .thenReturn(Collections.singletonList("[]"));

    controller.getPage(
        redirectAttributes,
        model,
        "1",
        null,
        null,
        null,
        null,
        null,
        "Alice",
        null,
        null,
        null,
        "timestamp",
        "desc");

    verify(model).addAttribute("pageFilters", "&userNameQuery=Alice");
    verify(model).addAttribute("transactions", Collections.singletonList("[]"));
  }

  @Test
  void testUserOpenIdQueryFilter() throws JsonProcessingException, DateTimeParseException {
    when(userService.getUsersByOpenIdQuery("openid123")).thenReturn(Collections.emptyList());
    when(adminTransactionService.getTransactionByUsersInPageInfo(
            transactionService, 1, 4, Collections.emptyList()))
        .thenReturn(paginationInfo);
    when(adminTransactionService.getTransactionsByUsersJSON(
            transactionService, paginationInfo, Collections.emptyList(), "timestamp", "desc"))
        .thenReturn(Collections.singletonList("[]"));

    controller.getPage(
        redirectAttributes,
        model,
        "1",
        null,
        null,
        null,
        null,
        "openid123",
        null,
        null,
        null,
        null,
        "timestamp",
        "desc");

    verify(model).addAttribute("pageFilters", "&userOpenIdQuery=openid123");
    verify(model).addAttribute("transactions", Collections.singletonList("[]"));
  }

  @Test
  void testDescriptionQueryFilter() throws JsonProcessingException, DateTimeParseException {
    when(adminTransactionService.getTransactionByDescriptionPageInfo(
            transactionService, 1, 4, "rent"))
        .thenReturn(paginationInfo);
    when(adminTransactionService.getTransactionsByDescriptionJSON(
            transactionService, paginationInfo, "rent", "timestamp", "desc"))
        .thenReturn(Collections.singletonList("[]"));

    controller.getPage(
        redirectAttributes,
        model,
        "1",
        null,
        null,
        null,
        null,
        null,
        null,
        "rent",
        null,
        null,
        "timestamp",
        "desc");

    verify(model).addAttribute("pageFilters", "&descQuery=rent");
    verify(model).addAttribute("transactions", Collections.singletonList("[]"));
  }

  @Test
  void testTypeAndStatusFilter() throws JsonProcessingException, DateTimeParseException {
    when(adminTransactionService.getTransactionByTypeAndStatusPageInfo(
            transactionService, 1, 4, "CREDIT", "SUCCESS"))
        .thenReturn(paginationInfo);
    when(adminTransactionService.getTransactionsByTypeAndStatusJSON(
            transactionService, paginationInfo, "CREDIT", "SUCCESS", "timestamp", "desc"))
        .thenReturn(Collections.singletonList("[]"));

    controller.getPage(
        redirectAttributes,
        model,
        "1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "CREDIT",
        "SUCCESS",
        "timestamp",
        "desc");

    verify(model).addAttribute("pageFilters", "&type=CREDIT&status=SUCCESS");
    verify(model).addAttribute("transactions", Collections.singletonList("[]"));
  }

  @Test
  void testDefaultNoFilters() throws JsonProcessingException, DateTimeParseException {
    when(adminTransactionService.getTransactionPageInfo(transactionService, 1, 4))
        .thenReturn(paginationInfo);
    when(adminTransactionService.getTransactionsAllJSON(
            transactionService, paginationInfo, "timestamp", "desc"))
        .thenReturn(Collections.singletonList("[]"));

    controller.getPage(
        redirectAttributes,
        model,
        "1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "timestamp",
        "desc");

    verify(model).addAttribute("pageFilters", "");
    verify(model).addAttribute("transactions", Collections.singletonList("[]"));
  }
}
