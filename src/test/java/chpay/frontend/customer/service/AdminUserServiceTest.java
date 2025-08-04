package chpay.frontend.customer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @InjectMocks private AdminUserServiceImpl adminUserService;

  @Mock private UserService userService;

  private PaginationInfo paginationInfo;
  private List<User> testUsers;

  @BeforeEach
  void setUp() {
    paginationInfo = new PaginationInfo(1, 10, true, false, 20);

    testUsers = new ArrayList<>();
    User user1 = new User("testUser1", "testUser1@email.com", "WISCH.11111");
    User user2 = new User("testUser2", "testUser2@email.com", "WISCH.22222");
    testUsers.add(user1);
    testUsers.add(user2);
  }

  @Test
  @DisplayName("Should return empty list when pagination has no transactions")
  void getUsersAllJSON_WithZeroTransactions_ShouldReturnEmptyList() throws JsonProcessingException {
    // Arrange
    try {
      java.lang.reflect.Field totalTransactionsField =
          PaginationInfo.class.getDeclaredField("totalTransactions");
      totalTransactionsField.setAccessible(true);
      totalTransactionsField.set(paginationInfo, 0);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    // Act
    List<String> result = adminUserService.getUsersAllJSON(userService, paginationInfo, null, null);

    // Assert
    assertTrue(result.isEmpty());
    verify(userService, never()).getAllUsers(anyInt(), anyInt());
  }

  @Test
  @DisplayName("Should return users list as JSON")
  void getUsersAllJSON_WithValidData_ShouldReturnUsersList() throws JsonProcessingException {
    // Arrange
    when(userService.getAllUsers(0, 10)).thenReturn(testUsers);

    // Act
    List<String> result = adminUserService.getUsersAllJSON(userService, paginationInfo, null, null);

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService).getAllUsers(0, 10);
  }

  @Test
  @DisplayName("Should return users filtered by balance")
  void getUsersByBalanceJSON_WithValidData_ShouldReturnFilteredUsers()
      throws JsonProcessingException {
    // Arrange
    String balanceAfter = "100";
    String balanceBefore = "200";
    when(userService.getUsersByBalancePageable(
            any(BigDecimal.class), any(BigDecimal.class), eq(0), eq(10)))
        .thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersByBalanceJSON(
            userService, paginationInfo, balanceAfter, balanceBefore, null, null);

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService)
        .getUsersByBalancePageable(
            new BigDecimal(balanceAfter), new BigDecimal(balanceBefore), 0, 10);
  }

  @Test
  @DisplayName("Should throw NumberFormatException for invalid balance values")
  void getUsersByBalanceJSON_WithInvalidBalance_ShouldThrowException() {
    // Arrange
    String invalidBalance = "invalid";

    // Act & Assert
    assertThrows(
        NumberFormatException.class,
        () ->
            adminUserService.getUsersByBalanceJSON(
                userService, paginationInfo, invalidBalance, "200", null, null));
  }

  @Test
  @DisplayName("Should return users filtered by name")
  void getUsersByNameJSON_WithValidQuery_ShouldReturnFilteredUsers()
      throws JsonProcessingException {
    // Arrange
    String nameQuery = "test";
    when(userService.getUsersByNameQueryPageable(nameQuery, 0, 10)).thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersByNameJSON(userService, paginationInfo, nameQuery, null, null);

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService).getUsersByNameQueryPageable(nameQuery, 0, 10);
  }

  @Test
  @DisplayName("Should return users filtered by email")
  void getUsersByEmailJSON_WithValidQuery_ShouldReturnFilteredUsers()
      throws JsonProcessingException {
    // Arrange
    String emailQuery = "test@example.com";
    when(userService.getUsersByEmailQueryPageable(emailQuery, 0, 10)).thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersByEmailJSON(userService, paginationInfo, emailQuery, null, null);

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService).getUsersByEmailQueryPageable(emailQuery, 0, 10);
  }

  @Test
  @DisplayName("Should return pagination info for all users")
  void getUserAllPageInfo_ShouldReturnCorrectPaginationInfo() {
    // Arrange
    when(userService.countAll()).thenReturn(25L);

    // Act
    PaginationInfo result = adminUserService.getUserAllPageInfo(userService, 1, 10);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(25, result.getTotalTransactions());
    verify(userService).countAll();
  }

  @Test
  @DisplayName("Should return pagination info for balance-filtered users")
  void getUserByBalancePageInfo_ShouldReturnCorrectPaginationInfo() {
    // Arrange
    String balanceAfter = "100";
    String balanceBefore = "200";
    when(userService.countUsersByBalance(any(BigDecimal.class), any(BigDecimal.class)))
        .thenReturn(15L);

    // Act
    PaginationInfo result =
        adminUserService.getUserByBalancePageInfo(userService, 1, 10, balanceAfter, balanceBefore);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(15, result.getTotalTransactions());
    verify(userService)
        .countUsersByBalance(new BigDecimal(balanceAfter), new BigDecimal(balanceBefore));
  }

  @Test
  @DisplayName("Should return pagination info for name-filtered users")
  void getUserByNamePageInfo_ShouldReturnCorrectPaginationInfo() {
    // Arrange
    String nameQuery = "test";
    when(userService.countUsersByNameQuery(nameQuery)).thenReturn(5L);

    // Act
    PaginationInfo result = adminUserService.getUserByNamePageInfo(userService, 1, 10, nameQuery);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(5, result.getTotalTransactions());
    verify(userService).countUsersByNameQuery(nameQuery);
  }

  @Test
  @DisplayName("Should return pagination info for email-filtered users")
  void getUserByEmailPageInfo_ShouldReturnCorrectPaginationInfo() {
    // Arrange
    String emailQuery = "test@example.com";
    when(userService.countUsersByEmailQuery(emailQuery)).thenReturn(3L);

    // Act
    PaginationInfo result = adminUserService.getUserByEmailPageInfo(userService, 1, 10, emailQuery);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(3, result.getTotalTransactions());
    verify(userService).countUsersByEmailQuery(emailQuery);
  }

  @Test
  @DisplayName("Should return users list as JSON with ascending sort by name")
  void getUsersAllJSON_WithSortByNameAsc_ShouldReturnSortedUsersList()
      throws JsonProcessingException {
    // Arrange
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    when(userService.getAllUsers(0, 10, sort)).thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersAllJSON(userService, paginationInfo, "name", "asc");

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService).getAllUsers(0, 10, sort);
  }

  @Test
  @DisplayName("Should return users list as JSON with descending sort by name")
  void getUsersAllJSON_WithSortByNameDesc_ShouldReturnSortedUsersList()
      throws JsonProcessingException {
    // Arrange
    Sort sort = Sort.by(Sort.Direction.DESC, "name");
    when(userService.getAllUsers(0, 10, sort)).thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersAllJSON(userService, paginationInfo, "name", "desc");

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService).getAllUsers(0, 10, sort);
  }

  @Test
  @DisplayName("Should return users filtered by balance with ascending sort by balance")
  void getUsersByBalanceJSON_WithSortByBalanceAsc_ShouldReturnSortedUsers()
      throws JsonProcessingException {
    // Arrange
    String balanceAfter = "100";
    String balanceBefore = "200";
    Sort sort = Sort.by(Sort.Direction.ASC, "balance");
    when(userService.getUsersByBalancePageable(
            any(BigDecimal.class), any(BigDecimal.class), eq(0), eq(10), eq(sort)))
        .thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersByBalanceJSON(
            userService, paginationInfo, balanceAfter, balanceBefore, "balance", "asc");

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService)
        .getUsersByBalancePageable(
            new BigDecimal(balanceAfter), new BigDecimal(balanceBefore), 0, 10, sort);
  }

  @Test
  @DisplayName("Should return users filtered by name with descending sort by name")
  void getUsersByNameJSON_WithSortByNameDesc_ShouldReturnSortedUsers()
      throws JsonProcessingException {
    // Arrange
    String nameQuery = "test";
    Sort sort = Sort.by(Sort.Direction.DESC, "name");
    when(userService.getUsersByNameQueryPageable(eq(nameQuery), eq(0), eq(10), eq(sort)))
        .thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersByNameJSON(userService, paginationInfo, nameQuery, "name", "desc");

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService).getUsersByNameQueryPageable(nameQuery, 0, 10, sort);
  }

  @Test
  @DisplayName("Should return users filtered by email with ascending sort by email")
  void getUsersByEmailJSON_WithSortByEmailAsc_ShouldReturnSortedUsers()
      throws JsonProcessingException {
    // Arrange
    String emailQuery = "test@example.com";
    Sort sort = Sort.by(Sort.Direction.ASC, "email");
    when(userService.getUsersByEmailQueryPageable(eq(emailQuery), eq(0), eq(10), eq(sort)))
        .thenReturn(testUsers);

    // Act
    List<String> result =
        adminUserService.getUsersByEmailJSON(
            userService, paginationInfo, emailQuery, "email", "asc");

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());
    verify(userService).getUsersByEmailQueryPageable(emailQuery, 0, 10, sort);
  }
}
