package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.Exceptions.IllegalRefundException;
import chpay.DatabaseHandler.transactiondb.entities.*;
import chpay.DatabaseHandler.transactiondb.entities.transactions.*;
import chpay.DatabaseHandler.transactiondb.repositories.RequestRepository;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import jakarta.persistence.LockTimeoutException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
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
class TransactionServiceTest {

  @Mock private TransactionRepository transactionRepository;

  @Mock private RequestRepository requestRepository;

  @Mock private BalanceService balanceService;

  @InjectMocks private TransactionService transactionService;

  private User testUser;
  private PaymentTransaction testTransaction;
  private UUID transactionId;

  @BeforeEach
  void setUp() {
    testUser = new User("Test User", "test@example.com", "test123", new BigDecimal("100.00"));
    transactionId = UUID.randomUUID();
    testTransaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment", null);
    testTransaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(testTransaction, transactionId);
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }
  }

  @Test
  void refundTransaction_validTransaction_refundsSuccessfully() {
    RefundTransaction refundTransaction =
        RefundTransaction.createRefund(testUser, new BigDecimal("50.00"), testTransaction);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));
    when(transactionRepository.existsByRefundOf(testTransaction)).thenReturn(false);
    when(balanceService.refund(any(), any(), any())).thenReturn(refundTransaction);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefundTransaction result = transactionService.refundTransaction(transactionId);

    assertEquals(Transaction.TransactionType.REFUND, result.getType());
    assertEquals(Transaction.TransactionStatus.SUCCESSFUL, result.getStatus());
    assertEquals(testTransaction.getAmount().abs(), result.getAmount());
    assertEquals(testTransaction, result.getRefundOf());
    assertEquals(Transaction.TransactionStatus.REFUNDED, testTransaction.getStatus());

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository).existsByRefundOf(testTransaction);
    verify(balanceService).refund(testUser, testTransaction.getAmount().negate(), testTransaction);
    verify(transactionRepository).save(any(Transaction.class));
  }

  @Test
  void refundTransaction_alreadyRefunded_throwsException() {
    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));
    when(transactionRepository.existsByRefundOf(testTransaction)).thenReturn(true);

    assertThrows(
        IllegalRefundException.class, () -> transactionService.refundTransaction(transactionId));

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository).existsByRefundOf(testTransaction);
    verify(balanceService, never()).refund(any(), any(), any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void refundTransaction_notSuccessfulTransaction_throwsException() {
    testTransaction.setStatus(Transaction.TransactionStatus.FAILED);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));

    assertThrows(
        IllegalRefundException.class, () -> transactionService.refundTransaction(transactionId));

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository, never()).existsByRefundOf(any());
    verify(balanceService, never()).refund(any(), any(), any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void refundTransaction_refundTransaction_throwsException() {
    testTransaction.setType(Transaction.TransactionType.REFUND);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));

    assertThrows(
        IllegalRefundException.class, () -> transactionService.refundTransaction(transactionId));

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository, never()).existsByRefundOf(any());
    verify(balanceService, never()).refund(any(), any(), any());
    verify(transactionRepository, never()).save(any());
  }

  /*@Test
  void refundTransaction_topUpTransaction_throwsException() {
    TopupTransaction topUpTransaction =
        TopupTransaction.createTopUpTransaction(testUser, new BigDecimal("50.00"), "Test top-up");
    topUpTransaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(topUpTransaction, transactionId);
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }

    when(transactionRepository.findByIdForUpdatePayment(transactionId)).thenReturn(topUpTransaction);

    assertThrows(
        IllegalRefundException.class, () -> transactionService.refundTransaction(transactionId));

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository, never()).existsByRefundOf(any());
    verify(balanceService, never()).refund(any(), any(), any());
    verify(transactionRepository, never()).save(any());
  }*/

  @Test
  void partialRefund_validTransaction_refundsPartially() {
    BigDecimal refundAmount = new BigDecimal("20.00");
    RefundTransaction refundTransaction =
        RefundTransaction.createRefund(testUser, refundAmount, testTransaction);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));
    when(transactionRepository.findByRefundOf(testTransaction)).thenReturn(Collections.emptyList());
    when(balanceService.refund(any(), any(), any())).thenReturn(refundTransaction);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefundTransaction result = transactionService.partialRefund(transactionId, refundAmount);

    assertEquals(Transaction.TransactionType.REFUND, result.getType());
    assertEquals(Transaction.TransactionStatus.SUCCESSFUL, result.getStatus());
    assertEquals(refundAmount, result.getAmount());
    assertEquals(testTransaction, result.getRefundOf());
    assertEquals(Transaction.TransactionStatus.PARTIALLY_REFUNDED, testTransaction.getStatus());

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository).findByRefundOf(testTransaction);
    verify(balanceService).refund(testUser, refundAmount.negate(), testTransaction);
    verify(transactionRepository, times(2)).save(any(Transaction.class));
  }

  @Test
  void partialRefund_fullAmountRefunded_marksAsRefunded() {
    BigDecimal refundAmount = new BigDecimal("50.00");
    RefundTransaction refundTransaction =
        RefundTransaction.createRefund(testUser, refundAmount, testTransaction);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));
    when(transactionRepository.findByRefundOf(testTransaction)).thenReturn(Collections.emptyList());
    when(balanceService.refund(any(), any(), any())).thenReturn(refundTransaction);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefundTransaction result = transactionService.partialRefund(transactionId, refundAmount);

    assertEquals(Transaction.TransactionStatus.REFUNDED, testTransaction.getStatus());

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository).findByRefundOf(testTransaction);
    verify(balanceService).refund(testUser, refundAmount.negate(), testTransaction);
    verify(transactionRepository, times(2)).save(any(Transaction.class));
  }

  @Test
  void partialRefund_previousPartialRefunds_calculatesRemainingCorrectly() {
    BigDecimal previousRefundAmount = new BigDecimal("20.00");
    BigDecimal newRefundAmount = new BigDecimal("30.00");

    RefundTransaction previousRefund =
        RefundTransaction.createRefund(testUser, previousRefundAmount, testTransaction);
    RefundTransaction newRefund =
        RefundTransaction.createRefund(testUser, newRefundAmount, testTransaction);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));
    when(transactionRepository.findByRefundOf(testTransaction))
        .thenReturn(Collections.singletonList(previousRefund));
    when(balanceService.refund(any(), any(), any())).thenReturn(newRefund);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RefundTransaction result = transactionService.partialRefund(transactionId, newRefundAmount);

    assertEquals(Transaction.TransactionStatus.REFUNDED, testTransaction.getStatus());

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository).findByRefundOf(testTransaction);
    verify(balanceService).refund(testUser, newRefundAmount.negate(), testTransaction);
    verify(transactionRepository, times(2)).save(any(Transaction.class));
  }

  @Test
  void partialRefund_refundExceedsOriginal_throwsException() {
    BigDecimal refundAmount = new BigDecimal("60.00");

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));
    when(transactionRepository.findByRefundOf(testTransaction)).thenReturn(Collections.emptyList());

    assertThrows(
        IllegalRefundException.class,
        () -> transactionService.partialRefund(transactionId, refundAmount));

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository).findByRefundOf(testTransaction);
    verify(balanceService, never()).refund(any(), any(), any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void partialRefund_negativeRefundAmount_throwsException() {
    BigDecimal refundAmount = new BigDecimal("-10.00");

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.ofNullable(testTransaction));

    assertThrows(
        IllegalRefundException.class,
        () -> transactionService.partialRefund(transactionId, refundAmount));

    verify(transactionRepository).findByIdForUpdatePayment(transactionId);
    verify(transactionRepository, never()).findByRefundOf(any());
    verify(balanceService, never()).refund(any(), any(), any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  void getTransactionsForUserPageable_ValidInput_ReturnsPagedTransactions() {
    int page = 0;
    int size = 10;
    Pageable pageable = PageRequest.of(page, size);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findByUser(testUser, pageable)).thenReturn(expectedTransactions);

    List<Transaction> result =
        transactionService.getTransactionsForUserPageable(testUser, page, size);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository).findByUser(testUser, pageable);
  }

  @Test
  void getTransactionsForUserIdPageable_ValidInput_ReturnsPagedTransactions() {
    int page = 0;
    int size = 10;
    UUID userId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(page, size);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findByUserId(userId, pageable)).thenReturn(expectedTransactions);

    List<Transaction> result =
        transactionService.getTransactionsForUserIdPageable(userId, page, size);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository).findByUserId(userId, pageable);
  }

  @Test
  void countTransactionsByUser_ValidUser_ReturnsCount() {
    long expectedCount = 5L;
    when(transactionRepository.countByUser(testUser)).thenReturn(expectedCount);

    long result = transactionService.countTransactionsByUser(testUser);

    assertEquals(expectedCount, result);
    verify(transactionRepository).countByUser(testUser);
  }

  @Test
  void countTransactionsByUserId_ValidUserId_ReturnsCount() {
    UUID userId = UUID.randomUUID();
    long expectedCount = 5L;
    when(transactionRepository.countByUser_Id(userId)).thenReturn(expectedCount);

    long result = transactionService.countTransactionsByUserId(userId);

    assertEquals(expectedCount, result);
    verify(transactionRepository).countByUser_Id(userId);
  }

  @Test
  void getAllTransactionsPageable_ValidInput_ReturnsPagedTransactions() {
    int page = 0;
    int size = 10;
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    Pageable pageable = PageRequest.of(page, size, sort);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findBy(pageable)).thenReturn(expectedTransactions);

    List<Transaction> result = transactionService.getAllTransactionsPageable(page, size, sort);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository).findBy(pageable);
  }

  @Test
  void getAllTransactionsByAmountPageable_ValidInput_ReturnsFilteredTransactions() {
    int page = 0;
    int size = 10;
    BigDecimal amountUnder = new BigDecimal("100.00");
    BigDecimal amountOver = new BigDecimal("0.00");
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    Pageable pageable = PageRequest.of(page, size, sort);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findTransactionByAmountBetween(amountUnder, amountOver, pageable))
        .thenReturn(expectedTransactions);

    List<Transaction> result =
        transactionService.getAllTransactionsByAmountPageable(
            amountUnder, amountOver, page, size, sort);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository).findTransactionByAmountBetween(amountUnder, amountOver, pageable);
  }

  @Test
  void getAllTransactionsByDatePageable_ValidInput_ReturnsFilteredTransactions() {
    int page = 0;
    int size = 10;
    LocalDateTime start = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now();
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    Pageable pageable = PageRequest.of(page, size, sort);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findTransactionsByTimestampBetween(start, end, pageable))
        .thenReturn(expectedTransactions);

    List<Transaction> result =
        transactionService.getAllTransactionsByDatePageable(start, end, page, size, sort);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository).findTransactionsByTimestampBetween(start, end, pageable);
  }

  @Test
  void getAllTransactionsByUserListPageable_ValidInput_ReturnsFilteredTransactions() {
    int page = 0;
    int size = 10;
    List<User> users = Arrays.asList(testUser);
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    Pageable pageable = PageRequest.of(page, size, sort);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findTransactionByUserIn(users, pageable))
        .thenReturn(expectedTransactions);

    List<Transaction> result =
        transactionService.getAllTransactionsByUserListPageable(users, page, size, sort);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository).findTransactionByUserIn(users, pageable);
  }

  @Test
  void getAllTransactionsByDescriptionPageable_ValidInput_ReturnsFilteredTransactions() {
    int page = 0;
    int size = 10;
    String description = "test";
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    Pageable pageable = PageRequest.of(page, size, sort);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findTransactionsByDescriptionContainingIgnoreCase(
            description, pageable))
        .thenReturn(expectedTransactions);

    List<Transaction> result =
        transactionService.getAllTransactionsByDescriptionPageable(description, page, size, sort);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository)
        .findTransactionsByDescriptionContainingIgnoreCase(description, pageable);
  }

  @Test
  void getTransactionsForTypeAndStatusPageable_ValidInput_ReturnsFilteredTransactions() {
    int page = 0;
    int size = 10;
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    Pageable pageable = PageRequest.of(page, size, sort);
    List<Transaction.TransactionType> types = Arrays.asList(Transaction.TransactionType.PAYMENT);
    List<Transaction.TransactionStatus> statuses =
        Arrays.asList(Transaction.TransactionStatus.SUCCESSFUL);
    List<Transaction> expectedTransactions = Arrays.asList(testTransaction);

    when(transactionRepository.findTransactionByTypeInAndStatusIn(types, statuses, pageable))
        .thenReturn(expectedTransactions);

    List<Transaction> result =
        transactionService.getTransactionsForTypeAndStatusPageable(
            types, statuses, page, size, sort);

    assertEquals(expectedTransactions, result);
    verify(transactionRepository).findTransactionByTypeInAndStatusIn(types, statuses, pageable);
  }

  @Test
  void getTransactionsForTypeAndStatusPageable_EmptyResult_ReturnsEmptyList() {
    int page = 0;
    int size = 10;
    Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
    Pageable pageable = PageRequest.of(page, size, sort);
    List<Transaction.TransactionType> types = Arrays.asList(Transaction.TransactionType.PAYMENT);
    List<Transaction.TransactionStatus> statuses =
        Arrays.asList(Transaction.TransactionStatus.SUCCESSFUL);

    when(transactionRepository.findTransactionByTypeInAndStatusIn(types, statuses, pageable))
        .thenReturn(Collections.emptyList());

    List<Transaction> result =
        transactionService.getTransactionsForTypeAndStatusPageable(
            types, statuses, page, size, sort);

    assertTrue(result.isEmpty());
    verify(transactionRepository).findTransactionByTypeInAndStatusIn(types, statuses, pageable);
  }

  @Test
  void countTransactionsByDate_ValidDates_ReturnsCount() {
    LocalDateTime start = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now();
    long expectedCount = 5L;

    when(transactionRepository.countTransactionsByTimestampBetween(start, end))
        .thenReturn(expectedCount);

    long result = transactionService.countTransactionsByDate(start, end);

    assertEquals(expectedCount, result);
    verify(transactionRepository).countTransactionsByTimestampBetween(start, end);
  }

  @Test
  void countTransactionsByAmount_ValidAmounts_ReturnsCount() {
    BigDecimal amountUnder = new BigDecimal("100.00");
    BigDecimal amountOver = new BigDecimal("0.00");
    long expectedCount = 5L;

    when(transactionRepository.countTransactionsByAmountBetween(amountUnder, amountOver))
        .thenReturn(expectedCount);

    long result = transactionService.countTransactionsByAmount(amountUnder, amountOver);

    assertEquals(expectedCount, result);
    verify(transactionRepository).countTransactionsByAmountBetween(amountUnder, amountOver);
  }

  @Test
  void countTransactionsByDescription_ValidDescription_ReturnsCount() {
    String description = "test";
    long expectedCount = 5L;

    when(transactionRepository.countTransactionsByDescriptionContainsIgnoreCase(description))
        .thenReturn(expectedCount);

    long result = transactionService.countTransactionsByDescription(description);

    assertEquals(expectedCount, result);
    verify(transactionRepository).countTransactionsByDescriptionContainsIgnoreCase(description);
  }

  @Test
  void countTransactionsByTypeAndStatus_MatchingTransactions_ReturnsCount() {
    List<Transaction.TransactionType> types =
        Arrays.asList(Transaction.TransactionType.PAYMENT, Transaction.TransactionType.REFUND);
    List<Transaction.TransactionStatus> statuses =
        Arrays.asList(
            Transaction.TransactionStatus.SUCCESSFUL, Transaction.TransactionStatus.REFUNDED);
    long expectedCount = 10L;

    when(transactionRepository.countTransactionByTypeInAndStatusIn(types, statuses))
        .thenReturn(expectedCount);

    long result = transactionService.countTransactionsByTypeAndStatus(types, statuses);

    assertEquals(expectedCount, result);
    verify(transactionRepository).countTransactionByTypeInAndStatusIn(types, statuses);
  }

  @Test
  void countTransactionsByTypeAndStatus_NoMatchingTransactions_ReturnsZero() {
    List<Transaction.TransactionType> types = Arrays.asList(Transaction.TransactionType.TOP_UP);
    List<Transaction.TransactionStatus> statuses =
        Arrays.asList(Transaction.TransactionStatus.FAILED);

    when(transactionRepository.countTransactionByTypeInAndStatusIn(types, statuses)).thenReturn(0L);

    long result = transactionService.countTransactionsByTypeAndStatus(types, statuses);

    assertEquals(0L, result);
    verify(transactionRepository).countTransactionByTypeInAndStatusIn(types, statuses);
  }

  @Test
  void fulfillInternalTransaction_singleUseRequestNotFulfilled_executesSuccessfully() {
    PaymentRequest request = new PaymentRequest(BigDecimal.TEN, "test", false);
    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment", request);

    UUID id = UUID.randomUUID();
    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(transaction, id);
    } catch (Exception e) {
      fail("Failed to set transaction ID: " + e.getMessage());
    }

    when(transactionRepository.findByIdForUpdatePayment(transaction.getId()))
        .thenReturn(Optional.of(transaction));
    when(balanceService.pay(testUser, transaction)).thenReturn(transaction);

    Transaction result = transactionService.fullfillTransaction(transaction.getId(), testUser);

    assertEquals(transaction, result);
    verify(balanceService).pay(testUser, transaction);
    verify(requestRepository).save(request);
  }

  @Test
  void fulfillInternalTransaction_multiUseRequest_doesNotSetFulfilled() {
    PaymentRequest request = new PaymentRequest(BigDecimal.TEN, "test", true); // multiUse = true
    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment", request);

    UUID id = UUID.randomUUID();
    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(transaction, id);
    } catch (Exception e) {
      fail("Failed to set transaction ID");
    }

    transaction.setStatus(Transaction.TransactionStatus.PENDING);

    when(transactionRepository.findByIdForUpdatePayment(id)).thenReturn(Optional.of(transaction));
    when(balanceService.pay(testUser, transaction)).thenReturn(transaction);

    Transaction result = transactionService.fullfillTransaction(id, testUser);

    assertEquals(transaction, result);
    verify(balanceService).pay(testUser, transaction);
    verify(requestRepository, never()).save(any());
  }

  @Test
  void fulfillInternalTransaction_singleUseRequestAlreadyFulfilled_throws() {
    PaymentRequest request = new PaymentRequest(BigDecimal.TEN, "test", false);
    request.setFulfilled(true); // already fulfilled
    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment", request);

    UUID id = UUID.randomUUID();
    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(transaction, id);
    } catch (Exception e) {
      fail("Failed to set transaction ID");
    }

    transaction.setStatus(Transaction.TransactionStatus.PENDING);

    when(transactionRepository.findByIdForUpdatePayment(id)).thenReturn(Optional.of(transaction));

    assertThrows(
        IllegalStateException.class, () -> transactionService.fullfillTransaction(id, testUser));

    verify(balanceService, never()).pay(any(), any());
    verify(requestRepository, never()).save(any());
  }

  @Test
  void getNonRefundedAmount_fullyRefunded_returnsZero() {
    testTransaction.setStatus(Transaction.TransactionStatus.REFUNDED);

    when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(testTransaction));

    BigDecimal result = transactionService.getNonRefundedAmount(transactionId);

    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  void getNonRefundedAmount_partialRefund_returnsCorrectAmount() {
    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-100.00"), "Test", null);
    UUID id = UUID.randomUUID();

    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(transaction, id);
    } catch (Exception e) {
      fail("Failed to set transaction ID");
    }

    transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    RefundTransaction refund1 =
        RefundTransaction.createRefund(testUser, new BigDecimal("30.00"), transaction);
    RefundTransaction refund2 =
        RefundTransaction.createRefund(testUser, new BigDecimal("20.00"), transaction);

    when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
    when(transactionRepository.findByRefundOf(transaction)).thenReturn(List.of(refund1, refund2));

    BigDecimal result = transactionService.getNonRefundedAmount(id);

    assertEquals(new BigDecimal("50.00"), result); // 100 - (30 + 20)
  }

  @Test
  void getNonRefundedAmount_noRefunds_returnsFullAmount() {
    PaymentTransaction transaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-80.00"), "Test", null);
    UUID id = UUID.randomUUID();

    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(transaction, id);
    } catch (Exception e) {
      fail("Failed to set transaction ID");
    }

    transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
    when(transactionRepository.findByRefundOf(transaction)).thenReturn(Collections.emptyList());

    BigDecimal result = transactionService.getNonRefundedAmount(id);

    assertEquals(new BigDecimal("80.00"), result);
  }

  @Test
  void getNonRefundedAmount_transactionNotFound_throwsException() {
    when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class, () -> transactionService.getNonRefundedAmount(transactionId));
  }

  @Test
  void partialRefund_invalidStatus_throwsIllegalRefundException() {
    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Not successful", null);
    setId(original, transactionId);
    original.setStatus(Transaction.TransactionStatus.FAILED); // invalid

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.of(original));

    assertThrows(
        IllegalRefundException.class,
        () -> transactionService.partialRefund(transactionId, new BigDecimal("10.00")));
  }

  @Test
  void partialRefund_topupTransaction_throws() {
    TopupTransaction original =
        TopupTransaction.createTopUpTransaction(testUser, new BigDecimal("100.00"), "Top-up");
    setId(original, transactionId);

    assertThrows(
        NoSuchElementException.class,
        () -> transactionService.partialRefund(transactionId, new BigDecimal("10.00")));
  }

  @Test
  void partialRefund_zeroAmount_throwsIllegalRefundException() {
    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Zero refund", null);
    setId(original, transactionId);
    original.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.of(original));

    assertThrows(
        IllegalRefundException.class,
        () -> transactionService.partialRefund(transactionId, BigDecimal.ZERO));
  }

  @Test
  void partialRefund_negativeAmount_throwsIllegalRefundException() {
    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Negative refund", null);
    setId(original, transactionId);
    original.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.of(original));

    assertThrows(
        IllegalRefundException.class,
        () -> transactionService.partialRefund(transactionId, new BigDecimal("-1.00")));
  }

  @Test
  void partialRefund_exceedsRemaining_throwsIllegalRefundException() {
    PaymentTransaction original =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-100.00"), "Over refund", null);
    setId(original, transactionId);
    original.setStatus(Transaction.TransactionStatus.SUCCESSFUL);

    RefundTransaction existing =
        RefundTransaction.createRefund(testUser, new BigDecimal("90.00"), original);

    when(transactionRepository.findByIdForUpdatePayment(transactionId))
        .thenReturn(Optional.of(original));
    when(transactionRepository.findByRefundOf(original)).thenReturn(List.of(existing));

    assertThrows(
        IllegalRefundException.class,
        () -> transactionService.partialRefund(transactionId, new BigDecimal("20.00")));
  }

  @Test
  void getTransactionById_existingTransaction_returnsOptional() {
    when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(testTransaction));

    Optional<Transaction> result = transactionService.getTransactionById(transactionId);

    assertTrue(result.isPresent());
    assertEquals(testTransaction, result.get());
    verify(transactionRepository).findById(transactionId);
  }

  @Test
  void countTransactionsByUsersIn_returnsCorrectCount() {
    List<User> users = List.of(testUser);
    when(transactionRepository.countTransactionsByUserIn(users)).thenReturn(42L);

    long result = transactionService.countTransactionsByUsersIn(users);

    assertEquals(42L, result);
    verify(transactionRepository).countTransactionsByUserIn(users);
  }

  @Test
  void countTransactionsAll_returnsTotalCount() {
    when(transactionRepository.count()).thenReturn(100L);

    long result = transactionService.countTransactionsAll();

    assertEquals(100L, result);
    verify(transactionRepository).count();
  }

  @Test
  void countTransactionsByUserWithTypes_returnsCorrectCount() {
    List<Transaction.TransactionType> types = List.of(Transaction.TransactionType.PAYMENT);
    when(transactionRepository.countByUserAndTypeIn(testUser, types)).thenReturn(7L);

    long result = transactionService.countTransactionsByUser(testUser, types);

    assertEquals(7L, result);
    verify(transactionRepository).countByUserAndTypeIn(testUser, types);
  }

  @Test
  void getTransactionsForUserPageable_withTypes_returnsTransactions() {
    Sort sort = Sort.by(Sort.Direction.ASC, "timestamp");
    List<Transaction.TransactionType> types = List.of(Transaction.TransactionType.PAYMENT);
    Pageable pageable = PageRequest.of(0, 5, sort);
    List<Transaction> expected = List.of(testTransaction);

    when(transactionRepository.findByUserAndTypeIn(testUser, types, pageable)).thenReturn(expected);

    List<Transaction> result =
        transactionService.getTransactionsForUserPageable(testUser, 0, 5, sort, types);

    assertEquals(expected, result);
  }

  @Test
  void getTransactionsForUserPageable_withoutTypes_returnsTransactions() {
    Sort sort = Sort.by(Sort.Direction.ASC, "timestamp");
    Pageable pageable = PageRequest.of(0, 5, sort);
    List<Transaction> expected = List.of(testTransaction);

    when(transactionRepository.findByUser(testUser, pageable)).thenReturn(expected);

    List<Transaction> result =
        transactionService.getTransactionsForUserPageable(testUser, 0, 5, sort);

    assertEquals(expected, result);
  }

  @Test
  void save_transaction_savesAndReturns() {
    when(transactionRepository.save(testTransaction)).thenReturn(testTransaction);

    Transaction result = transactionService.save(testTransaction);

    assertEquals(testTransaction, result);
    verify(transactionRepository).save(testTransaction);
  }

  @Test
  void getTransaction_withMollieId_returnsOptional() {
    String mollieId = "mollie-123";
    TopupTransaction topup =
        TopupTransaction.createTopUpTransaction(testUser, new BigDecimal("10.00"), "topup");
    when(transactionRepository.findTransactionByMollieId(mollieId)).thenReturn(Optional.of(topup));

    Optional<TopupTransaction> result = transactionService.getTransaction(mollieId);

    assertTrue(result.isPresent());
    assertEquals(topup, result.get());
  }

  static void setId(Transaction tx, UUID id) {
    try {
      Field idField = Transaction.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(tx, id);
    } catch (Exception e) {
      fail("Failed to set ID: " + e.getMessage());
    }
  }

  @Test
  void fulfillExternalTransaction_callsFulfillSuccessfully() {
    ExternalTransaction transaction =
        ExternalTransaction.createExternalTransaction(
            new BigDecimal("-30.00"),
            "external",
            "http://redirect.url",
            "http://webhook.url",
            "http://fallback.url");
    transaction.setStatus(Transaction.TransactionStatus.PENDING);
    UUID id = UUID.randomUUID();
    setId(transaction, id);

    when(transactionRepository.findByIdForUpdateExternal(id)).thenReturn(Optional.of(transaction));
    when(balanceService.pay(testUser, transaction)).thenReturn(transaction);

    Transaction result = transactionService.fullfillExternalTransaction(id, testUser);

    assertEquals(transaction, result);
    verify(transactionRepository).findByIdForUpdateExternal(id);
    verify(balanceService).pay(testUser, transaction);
  }

  @Test
  void fullfillExternalTransaction_shouldSucceedWhenTransactionIsValid() {
    UUID txId = UUID.randomUUID();
    User user = mock(User.class);
    ExternalTransaction tx = mock(ExternalTransaction.class);

    when(transactionRepository.findByIdForUpdateExternal(txId)).thenReturn(Optional.of(tx));
    when(tx.isFulfillable()).thenReturn(true);
    when(tx.getStatus()).thenReturn(Transaction.TransactionStatus.PENDING);
    Transaction completed = mock(Transaction.class);
    when(balanceService.pay(user, tx)).thenReturn(completed);

    Transaction result = transactionService.fullfillExternalTransaction(txId, user);

    assertEquals(completed, result);
  }

  @Test
  void fullfillExternalTransaction_shouldThrowIfTransactionNotFound() {
    UUID txId = UUID.randomUUID();
    User user = mock(User.class);

    when(transactionRepository.findByIdForUpdateExternal(txId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> transactionService.fullfillExternalTransaction(txId, user));
  }

  @Test
  void fullfillExternalTransaction_shouldThrowIfNotFulfillable() {
    UUID txId = UUID.randomUUID();
    User user = mock(User.class);
    ExternalTransaction tx = mock(ExternalTransaction.class);

    when(transactionRepository.findByIdForUpdateExternal(txId)).thenReturn(Optional.of(tx));
    when(tx.isFulfillable()).thenReturn(false);

    assertThrows(
        IllegalStateException.class,
        () -> transactionService.fullfillExternalTransaction(txId, user));
  }

  @Test
  void fullfillExternalTransaction_shouldThrowIfNotPending() {
    UUID txId = UUID.randomUUID();
    User user = mock(User.class);
    ExternalTransaction tx = mock(ExternalTransaction.class);

    when(transactionRepository.findByIdForUpdateExternal(txId)).thenReturn(Optional.of(tx));
    when(tx.isFulfillable()).thenReturn(true);
    when(tx.getStatus()).thenReturn(Transaction.TransactionStatus.SUCCESSFUL); // not pending

    assertThrows(
        IllegalStateException.class,
        () -> transactionService.fullfillExternalTransaction(txId, user));
  }

  @Test
  void recoverFromPessimisticLock_shouldMarkAsFailed() {
    UUID txId = UUID.randomUUID();
    User user = mock(User.class);
    Transaction tx = mock(Transaction.class);

    PessimisticEntityLockException ex = mock(PessimisticEntityLockException.class);
    when(ex.getMessage()).thenReturn("Simulated pessimistic lock failure");

    when(user.getId()).thenReturn(UUID.randomUUID()); // if needed for logging
    when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

    Transaction result = transactionService.recoverFromPessimisticLockException(ex, txId, user);

    verify(tx).setStatus(Transaction.TransactionStatus.FAILED);
    assertEquals(tx, result);
  }

  @Test
  void recoverFromLockTimeout_shouldMarkAsFailed() {
    UUID txId = UUID.randomUUID();
    User user = mock(User.class);
    Transaction tx = mock(Transaction.class);

    when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

    Transaction result =
        transactionService.recoverFromLockTimeout(
            new LockTimeoutException("lock wait"), txId, user);

    verify(tx).setStatus(Transaction.TransactionStatus.FAILED);
    assertEquals(tx, result);
  }
}
