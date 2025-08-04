package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.Statistic;
import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.StatisticsRepository;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

  @Mock private TransactionRepository transactionRepository;
  @Mock private UserRepository userRepository;
  @Mock private StatisticsRepository statisticsRepository;

  @InjectMocks private StatisticsService statisticsService;

  private Statistic balanceStatistic;
  private Statistic incomingFundsStatistic;
  private Statistic outgoingFundsStatistic;
  private LocalDate today;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    today = LocalDate.now();
    now = LocalDateTime.now();

    balanceStatistic = new Statistic(Statistic.StatisticType.BALANCE, new BigDecimal("1000.00"));
    incomingFundsStatistic =
        new Statistic(Statistic.StatisticType.INCOMING_FUNDS, new BigDecimal("500.00"));
    outgoingFundsStatistic =
        new Statistic(Statistic.StatisticType.OUTGOING_FUNDS, new BigDecimal("300.00"));
  }

  @Test
  void saveBalance_whenNoBalanceExistsForToday_savesNewBalance() {
    // Arrange
    StatisticsService spyService = spy(statisticsService);
    doNothing().when(spyService).saveTestRecords();

    when(statisticsRepository.getFirstByTypeIsAndDate(Statistic.StatisticType.BALANCE, today))
        .thenReturn(Optional.empty());
    when(userRepository.getBalanceNow()).thenReturn(new BigDecimal("1000.00"));

    // Act
    spyService.saveBalance();

    // Assert
    verify(statisticsRepository).getFirstByTypeIsAndDate(Statistic.StatisticType.BALANCE, today);
    verify(userRepository).getBalanceNow();
    verify(statisticsRepository).save(any(Statistic.class));
    verify(spyService).saveTestRecords();
  }

  @Test
  void saveBalance_whenBalanceExistsForToday_doesNotSaveNewBalance() {
    // Arrange
    when(statisticsRepository.getFirstByTypeIsAndDate(Statistic.StatisticType.BALANCE, today))
        .thenReturn(Optional.of(balanceStatistic));

    // Act
    statisticsService.saveBalance();

    // Assert
    verify(statisticsRepository).getFirstByTypeIsAndDate(Statistic.StatisticType.BALANCE, today);
    verify(userRepository, never()).getBalanceNow();
    verify(statisticsRepository, never()).save(any(Statistic.class));
  }

  @Test
  void saveIncomingFunds_whenNoIncomingFundsExistForToday_savesNewIncomingFunds() {
    // Arrange
    LocalDate yesterday = today.minusDays(1);
    when(statisticsRepository.getFirstByTypeIsAndDate(
            Statistic.StatisticType.INCOMING_FUNDS, today))
        .thenReturn(Optional.empty());
    when(transactionRepository.getIncomingSum(yesterday.atStartOfDay(), today.atStartOfDay()))
        .thenReturn(new BigDecimal("500.00"));

    // Act
    statisticsService.saveIncomingFunds();

    // Assert
    verify(statisticsRepository)
        .getFirstByTypeIsAndDate(Statistic.StatisticType.INCOMING_FUNDS, today);
    verify(transactionRepository).getIncomingSum(yesterday.atStartOfDay(), today.atStartOfDay());
    verify(statisticsRepository).save(any(Statistic.class));
  }

  @Test
  void saveIncomingFunds_whenIncomingFundsExistForToday_doesNotSaveNewIncomingFunds() {
    // Arrange
    when(statisticsRepository.getFirstByTypeIsAndDate(
            Statistic.StatisticType.INCOMING_FUNDS, today))
        .thenReturn(Optional.of(incomingFundsStatistic));

    // Act
    statisticsService.saveIncomingFunds();

    // Assert
    verify(statisticsRepository)
        .getFirstByTypeIsAndDate(Statistic.StatisticType.INCOMING_FUNDS, today);
    verify(transactionRepository, never()).getIncomingSum(any(), any());
    verify(statisticsRepository, never()).save(any(Statistic.class));
  }

  @Test
  void saveIncomingFunds_whenIncomingSumIsNull_savesZero() {
    // Arrange
    LocalDate yesterday = today.minusDays(1);
    when(statisticsRepository.getFirstByTypeIsAndDate(
            Statistic.StatisticType.INCOMING_FUNDS, today))
        .thenReturn(Optional.empty());
    when(transactionRepository.getIncomingSum(yesterday.atStartOfDay(), today.atStartOfDay()))
        .thenReturn(null);

    // Act
    statisticsService.saveIncomingFunds();

    // Assert
    verify(statisticsRepository)
        .getFirstByTypeIsAndDate(Statistic.StatisticType.INCOMING_FUNDS, today);
    verify(transactionRepository).getIncomingSum(yesterday.atStartOfDay(), today.atStartOfDay());
    verify(statisticsRepository).save(any(Statistic.class));
  }

  @Test
  void saveOutgoingFunds_whenNoOutgoingFundsExistForToday_savesNewOutgoingFunds() {
    // Arrange
    LocalDate yesterday = today.minusDays(1);
    when(statisticsRepository.getFirstByTypeIsAndDate(
            Statistic.StatisticType.OUTGOING_FUNDS, today))
        .thenReturn(Optional.empty());
    when(transactionRepository.getPaymentSum(yesterday.atStartOfDay(), today.atStartOfDay()))
        .thenReturn(new BigDecimal("200.00"));
    when(transactionRepository.getRefundSum(yesterday.atStartOfDay(), today.atStartOfDay()))
        .thenReturn(new BigDecimal("100.00"));

    // Act
    statisticsService.saveOutgoingFunds();

    // Assert
    verify(statisticsRepository)
        .getFirstByTypeIsAndDate(Statistic.StatisticType.OUTGOING_FUNDS, today);
    verify(transactionRepository).getPaymentSum(yesterday.atStartOfDay(), today.atStartOfDay());
    verify(transactionRepository).getRefundSum(yesterday.atStartOfDay(), today.atStartOfDay());
    verify(statisticsRepository).save(any(Statistic.class));
  }

  @Test
  void saveOutgoingFunds_whenOutgoingFundsExistForToday_doesNotSaveNewOutgoingFunds() {
    // Arrange
    when(statisticsRepository.getFirstByTypeIsAndDate(
            Statistic.StatisticType.OUTGOING_FUNDS, today))
        .thenReturn(Optional.of(outgoingFundsStatistic));

    // Act
    statisticsService.saveOutgoingFunds();

    // Assert
    verify(statisticsRepository)
        .getFirstByTypeIsAndDate(Statistic.StatisticType.OUTGOING_FUNDS, today);
    verify(transactionRepository, never()).getPaymentSum(any(), any());
    verify(transactionRepository, never()).getRefundSum(any(), any());
    verify(statisticsRepository, never()).save(any(Statistic.class));
  }

  @Test
  void saveOutgoingFunds_whenSumsAreNull_savesZero() {
    // Arrange
    LocalDate yesterday = today.minusDays(1);
    when(statisticsRepository.getFirstByTypeIsAndDate(
            Statistic.StatisticType.OUTGOING_FUNDS, today))
        .thenReturn(Optional.empty());
    when(transactionRepository.getPaymentSum(yesterday.atStartOfDay(), today.atStartOfDay()))
        .thenReturn(null);
    when(transactionRepository.getRefundSum(yesterday.atStartOfDay(), today.atStartOfDay()))
        .thenReturn(null);

    // Act
    statisticsService.saveOutgoingFunds();

    // Assert
    verify(statisticsRepository)
        .getFirstByTypeIsAndDate(Statistic.StatisticType.OUTGOING_FUNDS, today);
    verify(transactionRepository).getPaymentSum(yesterday.atStartOfDay(), today.atStartOfDay());
    verify(transactionRepository).getRefundSum(yesterday.atStartOfDay(), today.atStartOfDay());
    verify(statisticsRepository).save(any(Statistic.class));
  }

  @Test
  void getStatisticsForTimeframe_returnsStatisticsWithinTimeframe() {
    // Arrange
    int daysBack = 7;
    List<Statistic> expectedStatistics = Arrays.asList(balanceStatistic);

    when(statisticsRepository.getAllByTypeIsAndTimestampBetweenOrderByDate(
            eq(Statistic.StatisticType.BALANCE),
            any(LocalDateTime.class),
            any(LocalDateTime.class)))
        .thenReturn(expectedStatistics);

    // Act
    List<Statistic> result =
        statisticsService.getStatisticsForTimeframe(Statistic.StatisticType.BALANCE, daysBack);

    // Assert
    assertEquals(expectedStatistics, result);
    verify(statisticsRepository)
        .getAllByTypeIsAndTimestampBetweenOrderByDate(
            eq(Statistic.StatisticType.BALANCE),
            any(LocalDateTime.class),
            any(LocalDateTime.class));
  }

  @Test
  void getCurrentBalance_returnsCurrentBalance() {
    // Arrange
    BigDecimal expectedBalance = new BigDecimal("1000.00");
    when(userRepository.getBalanceNow()).thenReturn(expectedBalance);

    // Act
    BigDecimal result = statisticsService.getCurrentBalance();

    // Assert
    assertEquals(expectedBalance, result);
    verify(userRepository).getBalanceNow();
  }

  @Test
  void getAverageBalance_calculatesAverageBalance() {
    // Arrange
    BigDecimal totalBalance = new BigDecimal("1000.00");
    long userCount = 5L;

    when(userRepository.getBalanceNow()).thenReturn(totalBalance);
    when(userRepository.count()).thenReturn(userCount);

    // Act
    BigDecimal result = statisticsService.getAverageBalance();

    // Assert
    assertEquals(0, new BigDecimal("200").compareTo(result));
    verify(userRepository).getBalanceNow();
    verify(userRepository).count();
  }

  @Test
  void getCombinedIncomeFunds_returnsTotalIncomingFunds() {
    // Arrange
    int daysBack = 7;
    BigDecimal expectedIncome = new BigDecimal("500.00");

    when(transactionRepository.getIncomingSum(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(expectedIncome);

    // Act
    BigDecimal result = statisticsService.getCombinedIncomeFunds(daysBack);

    // Assert
    assertEquals(expectedIncome, result);
    verify(transactionRepository)
        .getIncomingSum(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  void getCombinedOutgoingFunds_returnsTotalOutgoingFunds() {
    // Arrange
    int daysBack = 7;
    BigDecimal paymentSum = new BigDecimal("200.00");
    BigDecimal refundSum = new BigDecimal("100.00");
    BigDecimal expectedOutgoing = new BigDecimal("300.00");

    when(transactionRepository.getPaymentSum(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(paymentSum);
    when(transactionRepository.getRefundSum(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(refundSum);

    // Act
    BigDecimal result = statisticsService.getCombinedOutgoingFunds(daysBack);

    // Assert
    assertEquals(expectedOutgoing, result);
    verify(transactionRepository).getPaymentSum(any(LocalDateTime.class), any(LocalDateTime.class));
    verify(transactionRepository).getRefundSum(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  void getCombinedOutgoingFunds_whenSumsAreNull_returnsZero() {
    // Arrange
    int daysBack = 7;

    when(transactionRepository.getPaymentSum(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(null);
    when(transactionRepository.getRefundSum(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(null);

    // Act
    BigDecimal result = statisticsService.getCombinedOutgoingFunds(daysBack);

    // Assert
    assertEquals(BigDecimal.ZERO, result);
    verify(transactionRepository).getPaymentSum(any(LocalDateTime.class), any(LocalDateTime.class));
    verify(transactionRepository).getRefundSum(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  void getIncomeFundsPerUser_returnsCorrectStatistics() {
    // Arrange
    int daysBack = 3;
    User user = new User("Test User", "test@example.com", "test-openid");
    LocalDateTime date = LocalDate.now().atStartOfDay().plusDays(1);

    // Mock transaction count to return positive value to avoid skipping days
    when(transactionRepository.countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(1L);

    // Mock incoming sum for each day
    when(transactionRepository.getIncomingSumUser(
            eq(date.minusDays(1)), eq(date.minusDays(0)), eq(user)))
        .thenReturn(new BigDecimal("100.00"));
    when(transactionRepository.getIncomingSumUser(
            eq(date.minusDays(2)), eq(date.minusDays(1)), eq(user)))
        .thenReturn(new BigDecimal("200.00"));
    when(transactionRepository.getIncomingSumUser(
            eq(date.minusDays(3)), eq(date.minusDays(2)), eq(user)))
        .thenReturn(null); // Test null handling

    // Act
    List<Statistic> result = statisticsService.getIncomeFundsPerUser(daysBack, user);

    // Assert
    assertEquals(3, result.size());
    assertEquals(Statistic.StatisticType.INCOMING_FUNDS, result.get(0).getType());
    assertEquals(0, new BigDecimal("100.00").compareTo(result.get(0).getAmount()));
    assertEquals(0, new BigDecimal("200.00").compareTo(result.get(1).getAmount()));
    assertEquals(
        0,
        BigDecimal.ZERO.compareTo(result.get(2).getAmount())); // Null should be converted to ZERO

    // Verify method calls
    verify(transactionRepository, times(1))
        .countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL));
    verify(transactionRepository, times(3))
        .getIncomingSumUser(any(LocalDateTime.class), any(LocalDateTime.class), eq(user));
  }

  @Test
  void getIncomeFundsPerUser_skipsEmptyPeriods() {
    // Arrange
    int daysBack = 20;
    User user = new User("Test User", "test@example.com", "test-openid");

    // Mock transaction count to return 0 for the first check (days 1-10)
    // and positive for the second check (days 11-20)
    when(transactionRepository.countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(0L, 1L); // First call returns 0, second call returns 1

    // Mock incoming sum for any day
    when(transactionRepository.getIncomingSumUser(
            any(LocalDateTime.class), any(LocalDateTime.class), eq(user)))
        .thenReturn(new BigDecimal("300.00"));

    // Act
    List<Statistic> result = statisticsService.getIncomeFundsPerUser(daysBack, user);

    // Assert
    // We should have statistics for some days, but not all, as some were skipped
    assertFalse(result.isEmpty(), "Result should not be empty");
    assertTrue(result.size() < daysBack, "Result should have fewer items than daysBack");
    assertEquals(Statistic.StatisticType.INCOMING_FUNDS, result.get(0).getType());
    assertEquals(0, new BigDecimal("300.00").compareTo(result.get(0).getAmount()));

    // Verify method calls
    verify(transactionRepository, atLeastOnce())
        .countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL));
    // Verify that getIncomingSumUser was called at least once
    verify(transactionRepository, atLeastOnce())
        .getIncomingSumUser(any(LocalDateTime.class), any(LocalDateTime.class), eq(user));
  }

  @Test
  void getOutgoingFundsPerUser_returnsCorrectStatistics() {
    // Arrange
    int daysBack = 3;
    User user = new User("Test User", "test@example.com", "test-openid");
    LocalDateTime date = LocalDate.now().atStartOfDay().plusDays(1);

    // Mock transaction count to return positive value to avoid skipping days
    when(transactionRepository.countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(1L);

    // Mock payment and refund sums for each day
    when(transactionRepository.getPaymentSumUser(
            eq(date.minusDays(1)), eq(date.minusDays(0)), eq(user)))
        .thenReturn(new BigDecimal("-50.00"));
    when(transactionRepository.getRefundSumUser(
            eq(date.minusDays(1)), eq(date.minusDays(0)), eq(user)))
        .thenReturn(new BigDecimal("20.00"));

    when(transactionRepository.getPaymentSumUser(
            eq(date.minusDays(2)), eq(date.minusDays(1)), eq(user)))
        .thenReturn(new BigDecimal("-100.00"));
    when(transactionRepository.getRefundSumUser(
            eq(date.minusDays(2)), eq(date.minusDays(1)), eq(user)))
        .thenReturn(null); // Test null handling

    when(transactionRepository.getPaymentSumUser(
            eq(date.minusDays(3)), eq(date.minusDays(2)), eq(user)))
        .thenReturn(null); // Test null handling
    when(transactionRepository.getRefundSumUser(
            eq(date.minusDays(3)), eq(date.minusDays(2)), eq(user)))
        .thenReturn(new BigDecimal("30.00"));

    // Act
    List<Statistic> result = statisticsService.getOutgoingFundsPerUser(daysBack, user);

    // Assert
    assertEquals(3, result.size());
    assertEquals(Statistic.StatisticType.OUTGOING_FUNDS, result.get(0).getType());
    // -50 + 20 = -30, then negated = 30
    assertEquals(0, new BigDecimal("30.00").compareTo(result.get(0).getAmount()));
    // -100 + 0 = -100, then negated = 100
    assertEquals(0, new BigDecimal("100.00").compareTo(result.get(1).getAmount()));
    // 0 + 30 = 30, then negated = -30
    assertEquals(0, new BigDecimal("-30.00").compareTo(result.get(2).getAmount()));

    // Verify method calls
    verify(transactionRepository, times(1))
        .countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL));
    verify(transactionRepository, times(3))
        .getPaymentSumUser(any(LocalDateTime.class), any(LocalDateTime.class), eq(user));
    verify(transactionRepository, times(3))
        .getRefundSumUser(any(LocalDateTime.class), any(LocalDateTime.class), eq(user));
  }

  @Test
  void getOutgoingFundsPerUser_skipsEmptyPeriods() {
    // Arrange
    int daysBack = 20;
    User user = new User("Test User", "test@example.com", "test-openid");

    // Mock transaction count to return 0 for the first check (days 1-10)
    // and positive for the second check (days 11-20)
    when(transactionRepository.countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(0L, 1L); // First call returns 0, second call returns 1

    // Mock payment and refund sums for any day
    when(transactionRepository.getPaymentSumUser(
            any(LocalDateTime.class), any(LocalDateTime.class), eq(user)))
        .thenReturn(new BigDecimal("-150.00"));
    when(transactionRepository.getRefundSumUser(
            any(LocalDateTime.class), any(LocalDateTime.class), eq(user)))
        .thenReturn(new BigDecimal("50.00"));

    // Act
    List<Statistic> result = statisticsService.getOutgoingFundsPerUser(daysBack, user);

    // Assert
    // We should have statistics for some days, but not all, as some were skipped
    assertFalse(result.isEmpty(), "Result should not be empty");
    assertTrue(result.size() < daysBack, "Result should have fewer items than daysBack");
    assertEquals(Statistic.StatisticType.OUTGOING_FUNDS, result.get(0).getType());
    // -150 + 50 = -100, then negated = 100
    assertEquals(0, new BigDecimal("100.00").compareTo(result.get(0).getAmount()));

    // Verify method calls
    verify(transactionRepository, atLeastOnce())
        .countTransactionByUserAndTimestampBetweenAndStatusIs(
            eq(user),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            eq(Transaction.TransactionStatus.SUCCESSFUL));
    // Verify that getPaymentSumUser and getRefundSumUser were called at least once
    verify(transactionRepository, atLeastOnce())
        .getPaymentSumUser(any(LocalDateTime.class), any(LocalDateTime.class), eq(user));
    verify(transactionRepository, atLeastOnce())
        .getRefundSumUser(any(LocalDateTime.class), any(LocalDateTime.class), eq(user));
  }

  @Test
  void getAverageMonthlyFundsPerUser_calculatesCorrectly() {
    // Arrange
    int daysBack = 30;
    List<Statistic> stats =
        Arrays.asList(
            new Statistic(Statistic.StatisticType.INCOMING_FUNDS, new BigDecimal("100.00")),
            new Statistic(Statistic.StatisticType.INCOMING_FUNDS, new BigDecimal("200.00")),
            new Statistic(Statistic.StatisticType.INCOMING_FUNDS, new BigDecimal("300.00")));

    // Act
    BigDecimal result = statisticsService.getAverageMonthlyFundsPerUser(stats, daysBack);

    // Assert
    // Total sum = 600, days = 30, months = 30/30.43 ≈ 1, so average = 600/1 = 600
    assertEquals(0, new BigDecimal("600").compareTo(result));
  }

  @Test
  void getAverageMonthlyFundsPerUser_returnsZeroForEmptyStats() {
    // Arrange
    int daysBack = 30;
    List<Statistic> emptyStats = List.of();

    // Act
    BigDecimal result = statisticsService.getAverageMonthlyFundsPerUser(emptyStats, daysBack);

    // Assert
    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  void getAverageMonthlyFundsPerUser_returnsZeroForZeroDaysBack() {
    // Arrange
    int daysBack = 0;
    List<Statistic> stats =
        Arrays.asList(
            new Statistic(Statistic.StatisticType.INCOMING_FUNDS, new BigDecimal("100.00")),
            new Statistic(Statistic.StatisticType.INCOMING_FUNDS, new BigDecimal("200.00")));

    // Act
    BigDecimal result = statisticsService.getAverageMonthlyFundsPerUser(stats, daysBack);

    // Assert
    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  void getAverageMonthlyFundsPerUser_returnsZeroForZeroSum() {
    // Arrange
    int daysBack = 30;
    List<Statistic> zeroStats =
        Arrays.asList(
            new Statistic(Statistic.StatisticType.INCOMING_FUNDS, BigDecimal.ZERO),
            new Statistic(Statistic.StatisticType.INCOMING_FUNDS, BigDecimal.ZERO));

    // Act
    BigDecimal result = statisticsService.getAverageMonthlyFundsPerUser(zeroStats, daysBack);

    // Assert
    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  void getLastMonth_returnsCorrectSum() {
    // Arrange
    BigDecimal expectedSum = new BigDecimal("500.00");
    Statistic.StatisticType type = Statistic.StatisticType.INCOMING_FUNDS;

    when(statisticsRepository.getSumBetweenDates(
            eq(type), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(expectedSum);

    // Act
    BigDecimal result = statisticsService.getLastMonth(type);

    // Assert
    assertEquals(expectedSum, result);
    verify(statisticsRepository)
        .getSumBetweenDates(eq(type), any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  void getLastMonth_whenSumIsNull_returnsZero() {
    // Arrange
    Statistic.StatisticType type = Statistic.StatisticType.OUTGOING_FUNDS;

    when(statisticsRepository.getSumBetweenDates(
            eq(type), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(null);

    // Act
    BigDecimal result = statisticsService.getLastMonth(type);

    // Assert
    assertEquals(BigDecimal.ZERO, result);
    verify(statisticsRepository)
        .getSumBetweenDates(eq(type), any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  void getTransactionCount_returnsCorrectCount() {
    // Arrange
    long expectedCount = 42L;
    when(transactionRepository.count()).thenReturn(expectedCount);

    // Act
    long result = statisticsService.getTransactionCount();

    // Assert
    assertEquals(expectedCount, result);
    verify(transactionRepository).count();
  }
}
