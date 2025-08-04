package chpay.frontend.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.Statistic;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.frontend.customer.controller.AdministratorUserPageController;
import chpay.paymentbackend.service.StatisticsService;
import chpay.paymentbackend.service.UserService;
import chpay.shared.service.NotificationService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class AdministratorUserPageControllerTest {

  private static final String MODEL_ATTR_USER = "user";
  private static final String MODEL_ATTR_URL_PAGE = "urlPage";
  private static final String MODEL_ATTR_MAIN_STAT = "mainStat";
  private static final String MODEL_ATTR_SECOND_STAT = "secondStat";
  private static final String MODEL_ATTR_STATS = "stats";
  private static final String MODEL_ATTR_TYPE = "type";

  @Mock private UserService userService;
  @Mock private NotificationService notificationService;
  @Mock private StatisticsService statisticsService;
  @Mock private SessionRegistry sessionRegistry;
  @Mock private Model model;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private AdministratorUserPageController controller;

  @Test
  void userPageValidTest() {
    String userKey = UUID.randomUUID().toString();
    User mockUser = mock(User.class);

    when(userService.getUserById(userKey)).thenReturn(mockUser);

    String viewName = controller.showUserPage(model, userKey, redirectAttributes);

    assertEquals("admin-user", viewName, "The view name should be 'admin-user'.");
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "adminUsers");
    verify(model).addAttribute(MODEL_ATTR_USER, mockUser);
    verify(userService).getUserById(userKey);
    verify(notificationService, never()).addErrorMessage(any(), any());
    verify(redirectAttributes, never()).addFlashAttribute(any(), any());
  }

  @Test
  void userPageNoSuchElementTest() {
    String userKey = "nonExistentUser";

    when(userService.getUserById(userKey)).thenReturn(null);

    assertThrows(
        NoSuchElementException.class,
        () -> {
          controller.showUserPage(model, userKey, redirectAttributes);
        },
        "Should throw NoSuchElementException when user is not found.");

    verify(userService).getUserById(userKey);
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "adminUsers");
    verify(model, never()).addAttribute(eq(MODEL_ATTR_USER), any());
    verify(notificationService, never()).addErrorMessage(any(), any());
    verify(redirectAttributes, never()).addFlashAttribute(any(), any());
  }

  @Test
  void showUserStatPage_IncomingFunds_Success() {
    // Arrange
    String userKey = UUID.randomUUID().toString();
    String type = "incoming-funds";
    String daysBack = "30";
    User mockUser = mock(User.class);
    List<Statistic> mockStats = new ArrayList<>();
    BigDecimal averageFunds = new BigDecimal("100.00");

    when(userService.getUserById(userKey)).thenReturn(mockUser);
    when(statisticsService.getIncomeFundsPerUser(30, mockUser)).thenReturn(mockStats);
    when(statisticsService.getAverageMonthlyFundsPerUser(mockStats, 30)).thenReturn(averageFunds);

    // Act
    String viewName =
        controller.showUserStatPage(model, userKey, type, daysBack, redirectAttributes);

    // Assert
    assertEquals("admin-user-stats", viewName);
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "adminUserStats");
    verify(model).addAttribute(MODEL_ATTR_USER, mockUser);
    verify(model).addAttribute(MODEL_ATTR_TYPE, "incoming-funds");
    verify(model).addAttribute(MODEL_ATTR_MAIN_STAT, averageFunds);
    verify(model).addAttribute(MODEL_ATTR_STATS, mockStats);
    verify(statisticsService).getIncomeFundsPerUser(30, mockUser);
  }

  @Test
  void showUserStatPage_OutcomingFunds_Success() {
    // Arrange
    String userKey = UUID.randomUUID().toString();
    String type = "outcoming-funds";
    String daysBack = "30";
    User mockUser = mock(User.class);
    List<Statistic> mockStats = new ArrayList<>();
    BigDecimal averageFunds = new BigDecimal("100.00");

    when(userService.getUserById(userKey)).thenReturn(mockUser);
    when(statisticsService.getOutgoingFundsPerUser(30, mockUser)).thenReturn(mockStats);
    when(statisticsService.getAverageMonthlyFundsPerUser(mockStats, 30)).thenReturn(averageFunds);

    // Act
    String viewName =
        controller.showUserStatPage(model, userKey, type, daysBack, redirectAttributes);

    // Assert
    assertEquals("admin-user-stats", viewName);
    verify(model).addAttribute(MODEL_ATTR_URL_PAGE, "adminUserStats");
    verify(model).addAttribute(MODEL_ATTR_USER, mockUser);
    verify(model).addAttribute(MODEL_ATTR_TYPE, "outcoming-funds");
    verify(model).addAttribute(MODEL_ATTR_MAIN_STAT, averageFunds);
    verify(model).addAttribute(MODEL_ATTR_STATS, mockStats);
    verify(statisticsService).getOutgoingFundsPerUser(30, mockUser);
  }

  @Test
  void showUserStatPage_InvalidType_ThrowsException() {
    // Arrange
    String userKey = UUID.randomUUID().toString();
    String type = "invalid-type";
    User mockUser = mock(User.class);

    when(userService.getUserById(userKey)).thenReturn(mockUser);

    // Act & Assert
    assertThrows(
        NoSuchElementException.class,
        () -> controller.showUserStatPage(model, userKey, type, null, redirectAttributes));
  }

  @Test
  void showUserStatPage_UserNotFound_ThrowsException() {
    // Arrange
    String userKey = "nonExistentUser";
    String type = "incoming-funds";

    when(userService.getUserById(userKey)).thenReturn(null);

    // Act & Assert
    assertThrows(
        NoSuchElementException.class,
        () -> controller.showUserStatPage(model, userKey, type, null, redirectAttributes));
  }

  @Test
  void showUserStatPage_NegativeDays_ThrowsException() {
    // Arrange
    String userKey = UUID.randomUUID().toString();
    String type = "incoming-funds";
    String daysBack = "-1";

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.showUserStatPage(model, userKey, type, daysBack, redirectAttributes));
  }

  @Test
  void showUserStatPage_ZeroAverageFunds_SetsZero() {
    // Arrange
    String userKey = UUID.randomUUID().toString();
    String type = "incoming-funds";
    User mockUser = mock(User.class);
    List<Statistic> mockStats = new ArrayList<>();

    when(userService.getUserById(userKey)).thenReturn(mockUser);
    when(statisticsService.getIncomeFundsPerUser(90, mockUser)).thenReturn(mockStats);
    when(statisticsService.getAverageMonthlyFundsPerUser(mockStats, 90))
        .thenReturn(BigDecimal.ZERO);

    // Act
    String viewName = controller.showUserStatPage(model, userKey, type, null, redirectAttributes);

    // Assert
    assertEquals("admin-user-stats", viewName);
    verify(model).addAttribute(MODEL_ATTR_MAIN_STAT, 0);
  }

  @Test
  void banUser_Success() {
    // Arrange
    UUID userId = UUID.randomUUID();
    User mockUser = mock(User.class);
    when(mockUser.getId()).thenReturn(userId);
    when(mockUser.getBanned()).thenReturn(false);

    when(userService.getUserByIdForUpdate(userId)).thenReturn(mockUser);

    // Mock the sessionRegistry
    SessionRegistry mockSessionRegistry = mock(SessionRegistry.class);
    when(mockSessionRegistry.getAllPrincipals()).thenReturn(new ArrayList<>());

    // Set the sessionRegistry field using reflection
    try {
      java.lang.reflect.Field field =
          AdministratorUserPageController.class.getDeclaredField("sessionRegistry");
      field.setAccessible(true);
      field.set(controller, mockSessionRegistry);
    } catch (Exception e) {
      fail("Failed to set sessionRegistry field: " + e.getMessage());
    }

    // Act
    String result = controller.banUser(userId);

    // Assert
    assertEquals("redirect:/admin/users/" + userId.toString(), result);
    verify(mockUser).setBanned(true);
    verify(userService).saveAndFlush(mockUser);
  }

  @Test
  void banUser_UserNotFound_ThrowsException() {
    // Arrange
    UUID userId = UUID.randomUUID();

    when(userService.getUserByIdForUpdate(userId)).thenReturn(null);

    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> controller.banUser(userId));
  }

  @Test
  void banUser_ExpireUserSession() {
    // Arrange
    UUID userId = UUID.randomUUID();
    User mockUser = mock(User.class);
    when(mockUser.getId()).thenReturn(userId);
    when(mockUser.getOpenID()).thenReturn("test-openid");
    when(mockUser.getBanned()).thenReturn(false);

    DefaultOidcUser mockOidcUser = mock(DefaultOidcUser.class);
    when(mockOidcUser.getSubject()).thenReturn("test-openid");

    List<Object> principals = Arrays.asList(mockOidcUser);
    SessionInformation mockSession = mock(SessionInformation.class);
    List<SessionInformation> sessions = Arrays.asList(mockSession);

    when(userService.getUserByIdForUpdate(userId)).thenReturn(mockUser);

    // Mock the sessionRegistry
    SessionRegistry mockSessionRegistry = mock(SessionRegistry.class);
    when(mockSessionRegistry.getAllPrincipals()).thenReturn(principals);
    when(mockSessionRegistry.getAllSessions(mockOidcUser, false)).thenReturn(sessions);

    // Set the sessionRegistry field using reflection
    try {
      java.lang.reflect.Field field =
          AdministratorUserPageController.class.getDeclaredField("sessionRegistry");
      field.setAccessible(true);
      field.set(controller, mockSessionRegistry);
    } catch (Exception e) {
      fail("Failed to set sessionRegistry field: " + e.getMessage());
    }

    // Act
    String result = controller.banUser(userId);

    // Assert
    assertEquals("redirect:/admin/users/" + userId.toString(), result);
    verify(mockUser).setBanned(true);
    verify(userService).saveAndFlush(mockUser);
    verify(mockSession).expireNow();
  }
}
