package chpay.frontend.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

public class BalanceHistoryServiceTest {
  @Mock private TransactionRepository transactionRepository;

  @InjectMocks private TransactionService transactionsService;

  private User testUser;
  private List<Transaction> testTransactions = new ArrayList<>();
  private List<String> transactionStrings = new ArrayList<>();
  private UUID userId;
  private List<Transaction.TransactionType> allTransactionTypes = new ArrayList<>();
  private List<Transaction.TransactionType> topUpTransactionTypes = new ArrayList<>();

  @BeforeEach
  void setUp() {
    transactionsService = Mockito.mock(TransactionService.class);
    transactionRepository = Mockito.mock(TransactionRepository.class);

    testUser = new User("Test User", "test@example.com", "test123", new BigDecimal("100.00"));
    userId = UUID.randomUUID();
    try {
      java.lang.reflect.Field idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(testUser, userId);
    } catch (Exception e) {
      fail("Failed to set User ID: " + e.getMessage());
    }
    testTransactions.add(
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment 1", null));
    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      java.lang.reflect.Field dateField = Transaction.class.getDeclaredField("timestamp");
      dateField.setAccessible(true);
      idField.setAccessible(true);
      dateField.set(testTransactions.get(0), LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0, 0));
      idField.set(testTransactions.get(0), UUID.randomUUID());
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }
    testTransactions.add(
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-20.00"), "Test payment 2", null));
    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      java.lang.reflect.Field dateField = Transaction.class.getDeclaredField("timestamp");
      dateField.setAccessible(true);
      idField.setAccessible(true);
      dateField.set(testTransactions.get(1), LocalDateTime.of(2025, Month.JANUARY, 2, 0, 0, 0));
      idField.set(testTransactions.get(1), UUID.randomUUID());
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }
    testTransactions.add(
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-10.00"), "Test payment 3", null));
    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      java.lang.reflect.Field dateField = Transaction.class.getDeclaredField("timestamp");
      dateField.setAccessible(true);
      idField.setAccessible(true);
      dateField.set(testTransactions.get(2), LocalDateTime.of(2025, Month.JANUARY, 3, 0, 0, 0));
      idField.set(testTransactions.get(2), UUID.randomUUID());
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }
    testTransactions.add(
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-20.00"), "Test payment 4", null));
    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      java.lang.reflect.Field dateField = Transaction.class.getDeclaredField("timestamp");
      dateField.setAccessible(true);
      idField.setAccessible(true);
      dateField.set(testTransactions.get(3), LocalDateTime.of(2025, Month.JANUARY, 4, 0, 0, 0));
      idField.set(testTransactions.get(3), UUID.randomUUID());
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }
    testTransactions.add(
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-2.00"), "Test payment 5", null));
    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      java.lang.reflect.Field dateField = Transaction.class.getDeclaredField("timestamp");
      dateField.setAccessible(true);
      idField.setAccessible(true);
      dateField.set(testTransactions.get(4), LocalDateTime.of(2025, Month.JANUARY, 5, 0, 0, 0));
      idField.set(testTransactions.get(4), UUID.randomUUID());
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }

    testTransactions.get(2).setType(Transaction.TransactionType.REFUND);
    testTransactions.get(3).setType(Transaction.TransactionType.TOP_UP);
    testTransactions.get(4).setType(Transaction.TransactionType.TOP_UP);
    for (Transaction transaction : testTransactions) {
      String str =
          "{\"id\":\""
              + transaction.getId().toString()
              + "\",\"userOpenID\":\""
              + testUser.getOpenID()
              + "\",\"amount\":"
              + transaction.getAmount().toString()
              + ",\"description\":\""
              + transaction.getDescription()
              + "\",\"status\":\""
              + transaction.getStatus().toString()
              + "\",\"date\":\""
              + transaction.getTimestamp().format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))
              + "\",\"refundOfId\":null}";
      transactionStrings.add(str);
      allTransactionTypes =
          List.of(
              Transaction.TransactionType.TOP_UP,
              Transaction.TransactionType.REFUND,
              Transaction.TransactionType.PAYMENT,
              Transaction.TransactionType.EXTERNAL_PAYMENT);
      topUpTransactionTypes = List.of(Transaction.TransactionType.TOP_UP);
    }
  }

  @Test
  void getTransactionsByUserAsJSONTest() throws JsonProcessingException {
    when(transactionsService.getTransactionsForUserPageable(
            testUser, 0, 3, Sort.by(Sort.Direction.ASC, "timestamp"), allTransactionTypes))
        .thenReturn(testTransactions.subList(0, 3));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo = new PaginationInfo(1, 3, true, true, 5);
    List<String> res =
        balanceHistoryService.getTransactionsByUserAsJSON(
            testUser, transactionsService, paginationInfo, "timestamp", "asc", false);

    assertEquals(transactionStrings.get(0), res.get(0));
    assertEquals(transactionStrings.get(1), res.get(1));
    assertEquals(transactionStrings.get(2), res.get(2));
    assertEquals(3, res.size());
  }

  @Test
  void getTransactionsByUserAsJSONTestEmpty() throws JsonProcessingException {
    testTransactions = new ArrayList<>();
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.getTransactionsForUserPageable(
            testUser, 0, 3, Sort.by(Sort.Direction.ASC, "timestamp"), allTransactionTypes))
        .thenReturn(testTransactions);
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo = new PaginationInfo(1, 3, true, true, 0);
    List<String> res =
        balanceHistoryService.getTransactionsByUserAsJSON(
            testUser, transactionsService, paginationInfo, "timestamp", "asc", false);

    assertEquals(new ArrayList<>(), res);
  }

  @Test
  void getTransactionsByUserAsJSONPagesTest() throws JsonProcessingException {
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.getTransactionsForUserPageable(
            testUser, 1, 3, Sort.by(Sort.Direction.ASC, "timestamp"), allTransactionTypes))
        .thenReturn(testTransactions.subList(3, 5));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo = new PaginationInfo(2, 3, true, true, 5);
    List<String> res =
        balanceHistoryService.getTransactionsByUserAsJSON(
            testUser, transactionsService, paginationInfo, "timestamp", "asc", false);

    assertEquals(transactionStrings.get(3), res.get(0));
    assertEquals(transactionStrings.get(4), res.get(1));
    assertEquals(2, res.size());
  }

  @Test
  void getTransactionsByUserAsJSONOnlyTopUpsTest() throws JsonProcessingException {
    when(transactionsService.getTransactionsForUserPageable(
            testUser, 0, 3, Sort.by(Sort.Direction.ASC, "timestamp"), topUpTransactionTypes))
        .thenReturn(testTransactions.subList(4, 5));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo = new PaginationInfo(1, 3, true, true, 5);
    List<String> res =
        balanceHistoryService.getTransactionsByUserAsJSON(
            testUser, transactionsService, paginationInfo, "timestamp", "asc", true);

    assertEquals(transactionStrings.get(4), res.get(0));
    assertEquals(1, res.size());
  }

  @Test
  void getTransactionsByUserSortedByAmountDescAsJSONTest() throws JsonProcessingException {
    testTransactions.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
    when(transactionsService.getTransactionsForUserPageable(
            testUser, 0, 3, Sort.by(Sort.Direction.DESC, "amount"), allTransactionTypes))
        .thenReturn(testTransactions.subList(0, 3));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo = new PaginationInfo(1, 3, true, true, 5);
    List<String> res =
        balanceHistoryService.getTransactionsByUserAsJSON(
            testUser, transactionsService, paginationInfo, "amount", "desc", false);

    assertEquals(transactionStrings.get(4), res.get(0));
    assertEquals(transactionStrings.get(2), res.get(1));
    assertEquals(transactionStrings.get(1), res.get(2));
    assertEquals(3, res.size());
  }

  @Test
  void getTransactionsByUserSortedByAmountAscJSONTest() throws JsonProcessingException {
    testTransactions.sort((a, b) -> a.getAmount().compareTo(b.getAmount()));
    when(transactionsService.getTransactionsForUserPageable(
            testUser, 0, 3, Sort.by(Sort.Direction.ASC, "amount"), allTransactionTypes))
        .thenReturn(testTransactions.subList(0, 3));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo = new PaginationInfo(1, 3, true, true, 5);
    List<String> res =
        balanceHistoryService.getTransactionsByUserAsJSON(
            testUser, transactionsService, paginationInfo, "amount", "asc", false);

    assertEquals(transactionStrings.get(0), res.get(0));
    assertEquals(transactionStrings.get(1), res.get(1));
    assertEquals(transactionStrings.get(3), res.get(2));
    assertEquals(3, res.size());
  }

  @Test
  void getTransactionPageInfoTest() {
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.countTransactionsByUser(testUser, allTransactionTypes))
        .thenReturn(((long) 5));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo =
        balanceHistoryService.getTransactionPageInfo(testUser, transactionsService, 1, 3, false);
    assertEquals(false, paginationInfo.isHasPrev());
    assertEquals(true, paginationInfo.isHasNext());
    assertEquals(1, paginationInfo.getPage());
    assertEquals(5, paginationInfo.getTotalTransactions());
    assertEquals(3, paginationInfo.getSize());
  }

  @Test
  void getTransactionPageInfoTestSecondPage() {
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.countTransactionsByUser(testUser, allTransactionTypes))
        .thenReturn(((long) 5));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo =
        balanceHistoryService.getTransactionPageInfo(testUser, transactionsService, 2, 3, false);
    assertEquals(true, paginationInfo.isHasPrev());
    assertEquals(false, paginationInfo.isHasNext());
    assertEquals(2, paginationInfo.getPage());
    assertEquals(5, paginationInfo.getTotalTransactions());
    assertEquals(3, paginationInfo.getSize());
  }

  @Test
  void getTransactionPageInfoTestBigPage() {
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.countTransactionsByUser(testUser, allTransactionTypes))
        .thenReturn(((long) 5));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo =
        balanceHistoryService.getTransactionPageInfo(testUser, transactionsService, 1, 20, false);
    assertEquals(false, paginationInfo.isHasPrev());
    assertEquals(false, paginationInfo.isHasNext());
    assertEquals(1, paginationInfo.getPage());
    assertEquals(5, paginationInfo.getTotalTransactions());
    assertEquals(20, paginationInfo.getSize());
  }

  @Test
  void getTransactionPageInfoTestNegativePage() {
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.countTransactionsByUser(testUser, allTransactionTypes))
        .thenReturn(((long) 5));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo =
        balanceHistoryService.getTransactionPageInfo(testUser, transactionsService, -1, 3, false);
    assertEquals(false, paginationInfo.isHasPrev());
    assertEquals(true, paginationInfo.isHasNext());
    assertEquals(1, paginationInfo.getPage());
    assertEquals(5, paginationInfo.getTotalTransactions());
    assertEquals(3, paginationInfo.getSize());
  }

  @Test
  void getTransactionPageInfoTestOutOfIndexPage() {
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.countTransactionsByUser(testUser, allTransactionTypes))
        .thenReturn(((long) 5));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo =
        balanceHistoryService.getTransactionPageInfo(testUser, transactionsService, 100, 3, false);
    assertEquals(true, paginationInfo.isHasPrev());
    assertEquals(false, paginationInfo.isHasNext());
    assertEquals(2, paginationInfo.getPage());
    assertEquals(5, paginationInfo.getTotalTransactions());
    assertEquals(3, paginationInfo.getSize());
  }

  @Test
  void getTransactionPageInfoOnlyTopUpTest() {
    when(transactionRepository.findByUserId(testUser.getId())).thenReturn(testTransactions);
    when(transactionsService.countTransactionsByUser(testUser, topUpTransactionTypes))
        .thenReturn(((long) 1));
    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo =
        balanceHistoryService.getTransactionPageInfo(testUser, transactionsService, 1, 3, true);
    assertEquals(false, paginationInfo.isHasPrev());
    assertEquals(false, paginationInfo.isHasNext());
    assertEquals(1, paginationInfo.getPage());
    assertEquals(1, paginationInfo.getTotalTransactions());
    assertEquals(3, paginationInfo.getSize());
  }

  @Test
  void getTransactionsByUserAsJSONNullReturnTest() throws JsonProcessingException {
    when(transactionsService.getTransactionsForUserPageable(
            testUser, 0, 3, Sort.by(Sort.Direction.ASC, "timestamp"), allTransactionTypes))
        .thenReturn(null);

    BalanceHistoryServiceImpl balanceHistoryService = new BalanceHistoryServiceImpl();
    PaginationInfo paginationInfo = new PaginationInfo(1, 3, true, true, 5);

    List<String> res =
        balanceHistoryService.getTransactionsByUserAsJSON(
            testUser, transactionsService, paginationInfo, "timestamp", "asc", false);

    assertEquals(0, res.size());
  }
}
