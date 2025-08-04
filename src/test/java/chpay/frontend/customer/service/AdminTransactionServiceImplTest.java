package chpay.frontend.customer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceImplTest {

  @Mock private TransactionService transactionService;

  @InjectMocks private AdminTransactionServiceImpl adminTransactionService;

  private User testUser;
  private List<Transaction> testTransactions;
  private PaginationInfo testPaginationInfo;

  @BeforeEach
  void setUp() {
    testUser = new User("Test User", "test@example.com", "password", new BigDecimal("100.00"));
    setUserId(testUser);

    testTransactions =
        Arrays.asList(
            createTestTransaction("-50.00", "Test payment 1"),
            createTestTransaction("-25.00", "Test payment 2"));

    testPaginationInfo = new PaginationInfo(1, 10, true, false, 20);
  }

  @Test
  void getTransactionsAllJSON_ValidInput_ReturnsJSON() throws JsonProcessingException {
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    when(transactionService.getAllTransactionsPageable(0, 10, sort)).thenReturn(testTransactions);

    List<String> result =
        adminTransactionService.getTransactionsAllJSON(
            transactionService, testPaginationInfo, "timestamp", "desc");

    assertNotNull(result);
    assertEquals(2, result.size());
    verify(transactionService).getAllTransactionsPageable(0, 10, sort);
  }

  @Test
  void getTransactionsByDateJSON_ValidDates_ReturnsJSON() throws JsonProcessingException {
    String startDate = "2024-01-01";
    String endDate = "2024-12-31";
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

    when(transactionService.getAllTransactionsByDatePageable(
            any(), any(), eq(1), eq(10), any(Sort.class)))
        .thenReturn(testTransactions);

    List<String> result =
        adminTransactionService.getTransactionsByDateJSON(
            transactionService, testPaginationInfo, startDate, endDate, "timestamp", "desc");

    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  void getTransactionsByAmountJSON_ValidAmounts_ReturnsJSON() throws JsonProcessingException {
    String amountUnder = "100.00";
    String amountOver = "0.00";
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

    when(transactionService.getAllTransactionsByAmountPageable(
            any(BigDecimal.class), any(BigDecimal.class), eq(0), eq(10), any(Sort.class)))
        .thenReturn(testTransactions);

    List<String> result =
        adminTransactionService.getTransactionsByAmountJSON(
            transactionService, testPaginationInfo, amountUnder, amountOver, "timestamp", "desc");

    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  void getTransactionsByUsersJSON_ValidUsers_ReturnsJSON() throws JsonProcessingException {
    List<User> users = Arrays.asList(testUser);
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

    when(transactionService.getAllTransactionsByUserListPageable(users, 0, 10, sort))
        .thenReturn(testTransactions);

    List<String> result =
        adminTransactionService.getTransactionsByUsersJSON(
            transactionService, testPaginationInfo, users, "timestamp", "desc");

    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  void getTransactionsByDescriptionJSON_ValidDescription_ReturnsJSON()
      throws JsonProcessingException {
    String description = "Test payment";
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

    when(transactionService.getAllTransactionsByDescriptionPageable(description, 0, 10, sort))
        .thenReturn(testTransactions);

    List<String> result =
        adminTransactionService.getTransactionsByDescriptionJSON(
            transactionService, testPaginationInfo, description, "timestamp", "desc");

    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  void getTransactionPageInfo_ValidInput_ReturnsPaginationInfo() {
    when(transactionService.countTransactionsAll()).thenReturn(20L);

    PaginationInfo result =
        adminTransactionService.getTransactionPageInfo(transactionService, 1, 10);

    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(20, result.getTotalTransactions());
  }

  @Test
  void getTransactionByDatePageInfo_ValidDates_ReturnsPaginationInfo() {
    String startDate = "2024-01-01";
    String endDate = "2024-12-31";

    when(transactionService.countTransactionsByDate(any(), any())).thenReturn(20L);

    PaginationInfo result =
        adminTransactionService.getTransactionByDatePageInfo(
            transactionService, 1, 10, startDate, endDate);

    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(20, result.getTotalTransactions());
  }

  @Test
  void getTransactionByAmountPageInfo_ValidAmounts_ReturnsPaginationInfo() {
    when(transactionService.countTransactionsByAmount(any(BigDecimal.class), any(BigDecimal.class)))
        .thenReturn(20L);

    PaginationInfo result =
        adminTransactionService.getTransactionByAmountPageInfo(
            transactionService, 1, 10, "100.00", "0.00");

    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(20, result.getTotalTransactions());
  }

  @Test
  void getTransactionByUsersInPageInfo_ValidUsers_ReturnsPaginationInfo() {
    List<User> users = Arrays.asList(testUser);
    when(transactionService.countTransactionsByUsersIn(users)).thenReturn(20L);

    PaginationInfo result =
        adminTransactionService.getTransactionByUsersInPageInfo(transactionService, 1, 10, users);

    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(20, result.getTotalTransactions());
  }

  @Test
  void getTransactionByDescriptionPageInfo_ValidDescription_ReturnsPaginationInfo() {
    when(transactionService.countTransactionsByDescription(anyString())).thenReturn(20L);

    PaginationInfo result =
        adminTransactionService.getTransactionByDescriptionPageInfo(
            transactionService, 1, 10, "Test payment");

    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(20, result.getTotalTransactions());
  }

  private PaymentTransaction createTestTransaction(String amount, String description) {
    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal(amount), description, null);
    setTransactionFields(transaction);
    return transaction;
  }

  @Test
  void getTransactionsByTypeAndStatusJSON_ValidInput_ReturnsJSON() throws JsonProcessingException {
    String type = "payment";
    String status = "successful";
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

    when(transactionService.getTransactionsForTypeAndStatusPageable(
            List.of(Transaction.TransactionType.PAYMENT),
            List.of(Transaction.TransactionStatus.SUCCESSFUL),
            0,
            10,
            sort))
        .thenReturn(testTransactions);

    List<String> result =
        adminTransactionService.getTransactionsByTypeAndStatusJSON(
            transactionService, testPaginationInfo, type, status, "timestamp", "desc");

    assertNotNull(result);
    assertEquals(2, result.size());
    verify(transactionService)
        .getTransactionsForTypeAndStatusPageable(
            List.of(Transaction.TransactionType.PAYMENT),
            List.of(Transaction.TransactionStatus.SUCCESSFUL),
            0,
            10,
            sort);
  }

  @Test
  void getTransactionsByTypeAndStatusJSON_ExternalPaymentType_ReturnsJSON()
      throws JsonProcessingException {
    String type = "external_payment";
    String status = "successful";
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

    when(transactionService.getTransactionsForTypeAndStatusPageable(
            List.of(Transaction.TransactionType.EXTERNAL_PAYMENT),
            List.of(Transaction.TransactionStatus.SUCCESSFUL),
            0,
            10,
            sort))
        .thenReturn(testTransactions);

    List<String> result =
        adminTransactionService.getTransactionsByTypeAndStatusJSON(
            transactionService, testPaginationInfo, type, status, "timestamp", "desc");

    assertNotNull(result);
    assertEquals(2, result.size());
    verify(transactionService)
        .getTransactionsForTypeAndStatusPageable(
            List.of(Transaction.TransactionType.EXTERNAL_PAYMENT),
            List.of(Transaction.TransactionStatus.SUCCESSFUL),
            0,
            10,
            sort);
  }

  @Test
  void getTransactionsByTypeAndStatusJSON_EmptyResult_ReturnsEmptyList()
      throws JsonProcessingException {
    String type = "payment";
    String status = "successful";
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

    when(transactionService.getTransactionsForTypeAndStatusPageable(
            List.of(Transaction.TransactionType.PAYMENT),
            List.of(Transaction.TransactionStatus.SUCCESSFUL),
            0,
            10,
            sort))
        .thenReturn(new ArrayList<>());

    List<String> result =
        adminTransactionService.getTransactionsByTypeAndStatusJSON(
            transactionService, testPaginationInfo, type, status, "timestamp", "desc");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void getTransactionByTypeAndStatusPageInfo_ValidTypeAndStatus_ReturnsPaginationInfo() {
    String type = "payment";
    String status = "successful";

    when(transactionService.countTransactionsByTypeAndStatus(
            List.of(Transaction.TransactionType.PAYMENT),
            List.of(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(15L);

    PaginationInfo result =
        adminTransactionService.getTransactionByTypeAndStatusPageInfo(
            transactionService, 1, 5, type, status);

    assertNotNull(result);
    assertEquals(1, result.getPage());
    assertEquals(5, result.getSize());
    assertEquals(15, result.getTotalTransactions());
  }

  private void setUserId(User user) {
    try {
      java.lang.reflect.Field idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, UUID.randomUUID());
    } catch (Exception e) {
      fail("Failed to set User ID: " + e.getMessage());
    }
  }

  private void setTransactionFields(Transaction transaction) {
    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      java.lang.reflect.Field timestampField = Transaction.class.getDeclaredField("timestamp");
      idField.setAccessible(true);
      timestampField.setAccessible(true);
      idField.set(transaction, UUID.randomUUID());
      timestampField.set(transaction, LocalDateTime.now());
    } catch (Exception e) {
      fail("Failed to set Transaction fields: " + e.getMessage());
    }
  }
}
