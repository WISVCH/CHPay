package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserService userService;

  private User testUser1;
  private User testUser2;

  @BeforeEach
  void setUp() {
    testUser1 = new User("Test User 1", "test1@example.com", "openid1", new BigDecimal("100.00"));
    testUser2 = new User("Test User 2", "test2@example.com", "openid2", new BigDecimal("200.00"));
  }

  @Test
  void getAllUsers_ReturnsListOfUsers() {
    List<User> expectedUsers = Arrays.asList(testUser1, testUser2);
    when(userRepository.findBy(any(Pageable.class))).thenReturn(expectedUsers);

    List<User> result = userService.getAllUsers(0, 10);

    assertEquals(expectedUsers, result);
    verify(userRepository).findBy(PageRequest.of(0, 10));
  }

  @Test
  void getUsersByOpenIdQuery_WithValidQuery_ReturnsMatchingUsers() {
    String query = "openid1 openid2";
    when(userRepository.findByOpenID("openid1")).thenReturn(Optional.of(testUser1));
    when(userRepository.findByOpenID("openid2")).thenReturn(Optional.of(testUser2));

    List<User> result = userService.getUsersByOpenIdQuery(query);

    assertEquals(2, result.size());
    assertTrue(result.contains(testUser1));
    assertTrue(result.contains(testUser2));
    verify(userRepository, times(2)).findByOpenID(any());
  }

  @Test
  void getUsersByOpenIdQuery_WithNonExistentOpenId_ReturnsNullForMissing() {
    String query = "openid1 nonexistent";
    when(userRepository.findByOpenID("openid1")).thenReturn(Optional.of(testUser1));
    when(userRepository.findByOpenID("nonexistent")).thenReturn(Optional.empty());

    List<User> result = userService.getUsersByOpenIdQuery(query);

    assertEquals(2, result.size());
    assertEquals(testUser1, result.get(0));
    assertNull(result.get(1));
  }

  @Test
  void getUsersByNameQuery_ReturnsMatchingUsers() {
    String query = "Test";
    List<User> expectedUsers = Arrays.asList(testUser1, testUser2);
    when(userRepository.findAllByNameContainingIgnoreCase(query)).thenReturn(expectedUsers);

    List<User> result = userService.getUsersByNameQuery(query);

    assertEquals(expectedUsers, result);
    verify(userRepository).findAllByNameContainingIgnoreCase(query);
  }

  @Test
  void getUsersByNameQueryPageable_ReturnsPagedResults() {
    String query = "Test";
    List<User> expectedUsers = Arrays.asList(testUser1);
    when(userRepository.findAllByNameContainingIgnoreCase(eq(query), any(Pageable.class)))
        .thenReturn(expectedUsers);

    List<User> result = userService.getUsersByNameQueryPageable(query, 0, 10);

    assertEquals(expectedUsers, result);
    verify(userRepository).findAllByNameContainingIgnoreCase(query, PageRequest.of(0, 10));
  }

  @Test
  void getUsersByEmailQueryPageable_ReturnsPagedResults() {
    String query = "test";
    List<User> expectedUsers = Arrays.asList(testUser1);
    when(userRepository.findAllByEmailContainingIgnoreCase(eq(query), any(Pageable.class)))
        .thenReturn(expectedUsers);

    List<User> result = userService.getUsersByEmailQueryPageable(query, 0, 10);

    assertEquals(expectedUsers, result);
    verify(userRepository).findAllByEmailContainingIgnoreCase(query, PageRequest.of(0, 10));
  }

  @Test
  void getUsersByBalancePageable_ReturnsPagedResults() {
    BigDecimal balanceAfter = new BigDecimal("50.00");
    BigDecimal balanceBefore = new BigDecimal("150.00");
    List<User> expectedUsers = Arrays.asList(testUser1);
    when(userRepository.findAllByBalanceBetween(
            eq(balanceAfter), eq(balanceBefore), any(Pageable.class)))
        .thenReturn(expectedUsers);

    List<User> result = userService.getUsersByBalancePageable(balanceAfter, balanceBefore, 0, 10);

    assertEquals(expectedUsers, result);
    verify(userRepository)
        .findAllByBalanceBetween(balanceAfter, balanceBefore, PageRequest.of(0, 10));
  }

  @Test
  void countAll_ReturnsCorrectCount() {
    long expectedCount = 2L;
    when(userRepository.count()).thenReturn(expectedCount);

    long result = userService.countAll();

    assertEquals(expectedCount, result);
    verify(userRepository).count();
  }

  @Test
  void countUsersByNameQuery_ReturnsCorrectCount() {
    String query = "Test";
    long expectedCount = 2L;
    when(userRepository.countAllByNameContainingIgnoreCase(query)).thenReturn(expectedCount);

    long result = userService.countUsersByNameQuery(query);

    assertEquals(expectedCount, result);
    verify(userRepository).countAllByNameContainingIgnoreCase(query);
  }

  @Test
  void countUsersByEmailQuery_ReturnsCorrectCount() {
    String query = "test";
    long expectedCount = 2L;
    when(userRepository.countAllByEmailContainingIgnoreCase(query)).thenReturn(expectedCount);

    long result = userService.countUsersByEmailQuery(query);

    assertEquals(expectedCount, result);
    verify(userRepository).countAllByEmailContainingIgnoreCase(query);
  }

  @Test
  void countUsersByBalance_ReturnsCorrectCount() {
    BigDecimal balanceAfter = new BigDecimal("50.00");
    BigDecimal balanceBefore = new BigDecimal("150.00");
    long expectedCount = 1L;
    when(userRepository.countAllByBalanceBetween(balanceAfter, balanceBefore))
        .thenReturn(expectedCount);

    long result = userService.countUsersByBalance(balanceAfter, balanceBefore);

    assertEquals(expectedCount, result);
    verify(userRepository).countAllByBalanceBetween(balanceAfter, balanceBefore);
  }

  @Test
  void getUserById_ReturnsUser() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser1));

    User result = userService.getUserById(userId.toString());

    assertEquals(testUser1, result);
    verify(userRepository).findById(userId);
  }

  @Test
  void getUserById_WithNonExistentId_ReturnsNull() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    User result = userService.getUserById(userId.toString());

    assertNull(result);
    verify(userRepository).findById(userId);
  }

  @Test
  void getAllUsers_WithSorting_ReturnsListOfUsers() {
    List<User> expectedUsers = Arrays.asList(testUser1, testUser2);
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    when(userRepository.findBy(any(Pageable.class))).thenReturn(expectedUsers);

    List<User> result = userService.getAllUsers(0, 10, sort);

    assertEquals(expectedUsers, result);
    verify(userRepository).findBy(PageRequest.of(0, 10, sort));
  }

  @Test
  void getUsersByNameQueryPageable_WithSorting_ReturnsPagedResults() {
    String query = "Test";
    List<User> expectedUsers = Arrays.asList(testUser1);
    Sort sort = Sort.by(Sort.Direction.DESC, "name");
    when(userRepository.findAllByNameContainingIgnoreCase(eq(query), any(Pageable.class)))
        .thenReturn(expectedUsers);

    List<User> result = userService.getUsersByNameQueryPageable(query, 0, 10, sort);

    assertEquals(expectedUsers, result);
    verify(userRepository).findAllByNameContainingIgnoreCase(query, PageRequest.of(0, 10, sort));
  }

  @Test
  void getUsersByEmailQueryPageable_WithSorting_ReturnsPagedResults() {
    String query = "test";
    List<User> expectedUsers = Arrays.asList(testUser1);
    Sort sort = Sort.by(Sort.Direction.ASC, "email");
    when(userRepository.findAllByEmailContainingIgnoreCase(eq(query), any(Pageable.class)))
        .thenReturn(expectedUsers);

    List<User> result = userService.getUsersByEmailQueryPageable(query, 0, 10, sort);

    assertEquals(expectedUsers, result);
    verify(userRepository).findAllByEmailContainingIgnoreCase(query, PageRequest.of(0, 10, sort));
  }

  @Test
  void getUsersByBalancePageable_WithSorting_ReturnsPagedResults() {
    BigDecimal balanceAfter = new BigDecimal("50.00");
    BigDecimal balanceBefore = new BigDecimal("150.00");
    List<User> expectedUsers = Arrays.asList(testUser1);
    Sort sort = Sort.by(Sort.Direction.DESC, "balance");
    when(userRepository.findAllByBalanceBetween(
            eq(balanceAfter), eq(balanceBefore), any(Pageable.class)))
        .thenReturn(expectedUsers);

    List<User> result =
        userService.getUsersByBalancePageable(balanceAfter, balanceBefore, 0, 10, sort);

    assertEquals(expectedUsers, result);
    verify(userRepository)
        .findAllByBalanceBetween(balanceAfter, balanceBefore, PageRequest.of(0, 10, sort));
  }
}
