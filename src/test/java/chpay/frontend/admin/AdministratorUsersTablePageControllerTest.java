package chpay.frontend.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import chpay.frontend.customer.controller.AdministratorUsersTablePageController;
import chpay.frontend.customer.service.AdminUserServiceImpl;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.UserService;
import chpay.shared.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public class AdministratorUsersTablePageControllerTest {

  @InjectMocks private AdministratorUsersTablePageController controller;

  @Mock private AdminUserServiceImpl adminUserService;

  @Mock private UserService userService;

  @Mock private NotificationService notificationService;

  @Mock private Model model;

  @Mock private RedirectAttributes redirectAttributes;

  @Mock private PaginationInfo paginationInfo;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void balanceFilterTest() throws JsonProcessingException, DateTimeParseException {
    when(adminUserService.getUserByBalancePageInfo(any(), eq(1), eq(4), anyString(), anyString()))
        .thenReturn(paginationInfo);
    when(adminUserService.getUsersByBalanceJSON(
            any(), any(), anyString(), anyString(), any(), any()))
        .thenReturn(Collections.singletonList("[]"));

    String result =
        controller.getPage(redirectAttributes, model, "1", "10", "100", null, null, null, null);

    verify(model).addAttribute("users", Collections.singletonList("[]"));
    verify(model).addAttribute("info", paginationInfo);
    verify(model).addAttribute("pageFilters", "&balanceAfter=10&balanceBefore=100");
    assertEquals("admin-user-table", result);
  }

  @Test
  void emailQueryTest() throws JsonProcessingException, DateTimeParseException {
    when(adminUserService.getUserByEmailPageInfo(any(), eq(1), eq(4), eq("test@example.com")))
        .thenReturn(paginationInfo);
    when(adminUserService.getUsersByEmailJSON(any(), any(), eq("test@example.com"), any(), any()))
        .thenReturn(Collections.singletonList("[]"));

    String result =
        controller.getPage(
            redirectAttributes, model, "1", null, null, "test@example.com", null, null, null);

    verify(model).addAttribute("users", Collections.singletonList("[]"));
    verify(model).addAttribute("info", paginationInfo);
    verify(model).addAttribute("pageFilters", "&emailQuery=test@example.com");
    assertEquals("admin-user-table", result);
  }

  @Test
  void nameQueryTest() throws JsonProcessingException, DateTimeParseException {
    when(adminUserService.getUserByNamePageInfo(any(), eq(1), eq(4), eq("John")))
        .thenReturn(paginationInfo);
    when(adminUserService.getUsersByNameJSON(any(), any(), eq("John"), any(), any()))
        .thenReturn(Collections.singletonList("[]"));

    String result =
        controller.getPage(redirectAttributes, model, "1", null, null, null, "John", null, null);

    verify(model).addAttribute("users", Collections.singletonList("[]"));
    verify(model).addAttribute("info", paginationInfo);
    verify(model).addAttribute("pageFilters", "&nameQuery=John");
    assertEquals("admin-user-table", result);
  }

  @Test
  void noFIlterTest() throws JsonProcessingException, DateTimeParseException {
    when(adminUserService.getUserAllPageInfo(any(), eq(1), eq(4))).thenReturn(paginationInfo);
    when(adminUserService.getUsersAllJSON(any(), any(), any(), any()))
        .thenReturn(Collections.singletonList("[]"));

    String result =
        controller.getPage(redirectAttributes, model, "1", null, null, null, null, null, null);

    verify(model).addAttribute("users", Collections.singletonList("[]"));
    verify(model).addAttribute("info", paginationInfo);
    verify(model).addAttribute("pageFilters", "");
    assertEquals("admin-user-table", result);
  }

  @Test
  void invalidTest() throws JsonProcessingException, DateTimeParseException {
    when(adminUserService.getUserAllPageInfo(any(), eq(1), eq(4))).thenReturn(paginationInfo);
    when(adminUserService.getUsersAllJSON(any(), any(), any(), any()))
        .thenReturn(Collections.singletonList("[]"));

    String result =
        controller.getPage(
            redirectAttributes, model, "1", null, null, null, null, "invalid", "invalid");

    verify(model).addAttribute("sortBy", null);
    verify(model).addAttribute("order", null);
    verify(model).addAttribute("sortingParams", "&sortBy=null&order=null");
    assertEquals("admin-user-table", result);
  }

  @Test
  void invalidPageNumTest() throws JsonProcessingException, DateTimeParseException {
    when(adminUserService.getUserAllPageInfo(any(), eq(1), eq(4))).thenReturn(paginationInfo);
    when(adminUserService.getUsersAllJSON(any(), any(), any(), any()))
        .thenReturn(Collections.singletonList("[]"));

    String result =
        controller.getPage(
            redirectAttributes, model, "invalid", null, null, null, null, null, null);

    verify(adminUserService).getUserAllPageInfo(userService, 1, 4);
    assertEquals("admin-user-table", result);
  }
}
