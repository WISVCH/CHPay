package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.model.Statistic;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.StatisticsRepository;
import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final StatisticsRepository statisticsRepository;

  @Autowired
  StatisticsService(
      TransactionRepository transactionRepository,
      UserRepository userRepository,
      StatisticsRepository statisticsRepository) {
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
    this.statisticsRepository = statisticsRepository;
  }

  /**
   * Saves the current balance as a statistic entry if no balance entry is already recorded for the
   * current day.
   *
   * <p>The method is scheduled to run periodically using a fixed rate. For now it is set to run
   * every hour.
   *
   * <p>This method interacts with the `StatisticsRepository` to check and store statistics, and the
   * `UserRepository` to retrieve the current balance.
   */
  @Scheduled(fixedRateString = "3600000")
  @Transactional
  public void saveBalance() {
    LocalDate date = LocalDate.now();
    if (statisticsRepository
        .getFirstByTypeIsAndDate(Statistic.StatisticType.BALANCE, date)
        .isPresent()) {
      // balance already saved for today
      return;
    }
    Statistic statistic =
        new Statistic(Statistic.StatisticType.BALANCE, userRepository.getBalanceNow());
    statisticsRepository.save(statistic);
    saveTestRecords();
  }

  /** Temporary method to generate records for the purpose of testing the frontend */
  @Transactional
  public void saveTestRecords() {
    LocalDate date = LocalDate.now();
    for (int i = 1; i < 100; i++) {
      // check if records have been generated for today already
      if (statisticsRepository
          .getFirstByTypeIsAndDate(Statistic.StatisticType.BALANCE, LocalDate.now().minusDays(i))
          .isPresent()) {
        // balance already saved for today
        return;
      }

      Statistic statistic =
          new Statistic(
              Statistic.StatisticType.BALANCE,
              BigDecimal.valueOf(Math.random() * 1000),
              LocalDateTime.now().minusDays(i));
      statisticsRepository.save(statistic);
      Statistic statistic1 =
          new Statistic(
              Statistic.StatisticType.INCOMING_FUNDS,
              BigDecimal.valueOf(Math.random() * 1000),
              LocalDateTime.now().minusDays(i));
      statisticsRepository.save(statistic1);
      Statistic statistic2 =
          new Statistic(
              Statistic.StatisticType.OUTGOING_FUNDS,
              BigDecimal.valueOf(Math.random() * 1000),
              LocalDateTime.now().minusDays(i));
      statisticsRepository.save(statistic2);
    }
  }

  /**
   * Saves the total sum of incoming funds as a statistic entry if no such entry is already recorded
   * for the current day.
   *
   * <p>The method calculates the incoming funds by summing up all relevant transactions from the
   * start of the previous day to the start of the current day. It then saves this value as a
   * statistic of type INCOMING_FUNDS.
   *
   * <p>This method is scheduled to run periodically with a fixed rate, currently set to one hour.
   *
   * <p>If a statistic entry for the current day of type INCOMING_FUNDS already exists, the method
   * will exit without making changes.
   *
   * <p>This method interacts with the `TransactionRepository` to calculate the incoming funds, and
   * the `StatisticsRepository` to store the computed statistics.
   */
  @Scheduled(fixedRateString = "3600000")
  @Transactional
  public void saveIncomingFunds() {
    LocalDate date = LocalDate.now();
    LocalDate yesterday = date.minusDays(1);
    if (statisticsRepository
        .getFirstByTypeIsAndDate(Statistic.StatisticType.INCOMING_FUNDS, date)
        .isPresent()) {
      // balance already saved for today
      return;
    }
    BigDecimal incomingSum =
        transactionRepository.getIncomingSum(yesterday.atStartOfDay(), date.atStartOfDay());
    Statistic statistic =
        new Statistic(
            Statistic.StatisticType.INCOMING_FUNDS,
            Objects.requireNonNullElseGet(incomingSum, () -> BigDecimal.valueOf(0)));
    statisticsRepository.save(statistic);
  }

  /**
   * Saves the total sum of outgoing funds as a statistic entry if no such entry is already recorded
   * for the current day.
   *
   * <p>The method calculates the outgoing funds by summing up all payment and refund transactions
   * from the start of the previous day to the start of the current day. It then saves this value as
   * a statistic of type OUTGOING_FUNDS.
   *
   * <p>This method is scheduled to run periodically with a fixed rate, currently set to one hour.
   *
   * <p>If a statistic entry for the current day of type OUTGOING_FUNDS already exists, the method
   * will exit without making changes.
   *
   * <p>This method interacts with the `TransactionRepository` to calculate the outgoing funds, and
   * the `StatisticsRepository` to store the computed statistics.
   */
  @Scheduled(fixedRateString = "3600000")
  @Transactional
  public void saveOutgoingFunds() {
    LocalDate date = LocalDate.now();
    LocalDate yesterday = date.minusDays(1);
    if (statisticsRepository
        .getFirstByTypeIsAndDate(Statistic.StatisticType.OUTGOING_FUNDS, date)
        .isPresent()) {
      // balance already saved for today
      return;
    }
    BigDecimal paymentSum =
        transactionRepository.getPaymentSum(yesterday.atStartOfDay(), date.atStartOfDay());
    BigDecimal refundSum =
        transactionRepository.getRefundSum(yesterday.atStartOfDay(), date.atStartOfDay());
    if (paymentSum == null) {
      paymentSum = BigDecimal.ZERO;
    }
    if (refundSum == null) {
      refundSum = BigDecimal.ZERO;
    }
    Statistic statistic =
        new Statistic(Statistic.StatisticType.OUTGOING_FUNDS, paymentSum.add(refundSum));
    statisticsRepository.save(statistic);
  }

  /**
   * Retrieves a list of statistics of a specified type within a given timeframe.
   *
   * <p>The method calculates the starting date based on the provided number of days and fetches all
   * statistics records of the specified type that fall between the calculated start date and the
   * current date and time.
   *
   * @param type the type of statistics to retrieve (e.g., BALANCE, INCOMING_FUNDS, OUTGOING_FUNDS)
   * @param daysBack the number of days to go back from the current date to define the starting
   *     range
   * @return a list of {@link Statistic} objects that match the specified type and fall within the
   *     timeframe
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public List<Statistic> getStatisticsForTimeframe(Statistic.StatisticType type, int daysBack) {
    LocalDateTime dateBack = LocalDateTime.now().minusDays(daysBack);
    return statisticsRepository.getAllByTypeIsAndTimestampBetweenOrderByDate(
        type, dateBack, LocalDateTime.now());
  }

  /**
   * Retrieves the current total balance by aggregating the balances of all users.
   *
   * <p>The method interacts with the user repository to calculate the total balance. Access to this
   * method is restricted to users with the ADMIN role.
   *
   * @return the current total balance as a BigDecimal
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public BigDecimal getCurrentBalance() {
    return userRepository.getBalanceNow();
  }

  /**
   * Calculates the average balance of all users by dividing the total balance by the user count.
   *
   * <p>The method retrieves the total balance using the userRepository and divides it by the total
   * number of users. Access to this method is restricted to users with the ADMIN role.
   *
   * @return the average balance of all users as a BigDecimal
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public BigDecimal getAverageBalance() {
    return userRepository
        .getBalanceNow()
        .divide(BigDecimal.valueOf(userRepository.count()), BigDecimal.ROUND_UP);
  }

  /**
   * Calculates the total sum of incoming funds over a specified period of time.
   *
   * <p>The method retrieves and aggregates incoming transactions from the current time to a
   * specified number of days back. Access to this method is restricted to users with the ADMIN
   * role.
   *
   * @param daysBack the number of days to look back from the current date to calculate the incoming
   *     funds
   * @return the total sum of incoming funds within the specified timeframe as a BigDecimal
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public BigDecimal getCombinedIncomeFunds(int daysBack) {
    return transactionRepository.getIncomingSum(
        LocalDateTime.now().minusDays(daysBack), LocalDateTime.now());
  }

  /**
   * Calculates the combined sum of outgoing funds (payments and refunds) within the specified
   * timeframe. The timeframe is determined by the number of days going back from the current date
   * and time. This method requires the caller to have an ADMIN role.
   *
   * @param daysBack the number of days to go back from the current date to calculate the outgoing
   *     funds
   * @return the combined sum of payments and refunds as a BigDecimal
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public BigDecimal getCombinedOutgoingFunds(int daysBack) {
    BigDecimal paymentSum =
        transactionRepository.getPaymentSum(
            LocalDateTime.now().minusDays(daysBack), LocalDateTime.now());
    BigDecimal refundSum =
        transactionRepository.getRefundSum(
            LocalDateTime.now().minusDays(daysBack), LocalDateTime.now());
    if (paymentSum == null) {
      paymentSum = BigDecimal.ZERO;
    }
    if (refundSum == null) {
      refundSum = BigDecimal.ZERO;
    }
    return paymentSum.add(refundSum);
  }

  /**
   * Retrieves a list of income fund statistics for a specific user over a given range of past days.
   *
   * @param daysBack the number of days in the past to retrieve statistics for, starting from the
   *     current day
   * @param user the user account for which to retrieve the income fund statistics
   * @return a list of {@link Statistic} objects containing the income fund data per day for the
   *     specified user
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public List<Statistic> getIncomeFundsPerUser(int daysBack, User user) {
    LocalDateTime date = LocalDate.now().atStartOfDay().plusDays(1);
    List<Statistic> stats = new ArrayList<>();
    for (int i = 1; i <= daysBack; i++) {
      // check if there were any transactions in the last 10 days
      if (i % 10 == 1) {
        // if no transactions are found skip 10 days to limit queries
        if (transactionRepository.countTransactionByUserAndTimestampBetweenAndStatusIs(
                user,
                date.minusDays(i + 10),
                date.minusDays((i - 1)),
                Transaction.TransactionStatus.SUCCESSFUL)
            <= 0) {
          i += 10;
          continue;
        }
      }
      BigDecimal amount =
          transactionRepository.getIncomingSumUser(
              date.minusDays(i), date.minusDays((i - 1)), user);
      stats.add(
          new Statistic(
              Statistic.StatisticType.INCOMING_FUNDS,
              amount == null ? BigDecimal.ZERO : amount,
              date.minusDays(i)));
    }
    return stats;
  }

  /**
   * Retrieves a list of statistics representing the outgoing funds (payments and refunds) for a
   * specific user over a specified number of days.
   *
   * @param daysBack the number of days to look back to calculate the outgoing funds. For example,
   *     passing 7 will calculate the statistics for the last 7 days.
   * @param user the user for whom the outgoing funds statistics are being calculated.
   * @return a list of {@code Statistic} objects, where each object includes the type of statistic,
   *     the total outgoing funds (sum of payments and refunds), and the corresponding date.
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public List<Statistic> getOutgoingFundsPerUser(int daysBack, User user) {
    LocalDateTime date = LocalDate.now().atStartOfDay().plusDays(1);
    List<Statistic> stats = new ArrayList<>();
    for (int i = 1; i <= daysBack; i++) {
      // check if there were any transactions in the last 10 days
      if (i % 10 == 1) {
        // if no transactions are found skip 10 days to limit queries
        if (transactionRepository.countTransactionByUserAndTimestampBetweenAndStatusIs(
                user,
                date.minusDays(i + 10),
                date.minusDays((i - 1)),
                Transaction.TransactionStatus.SUCCESSFUL)
            <= 0) {
          i += 10;
          continue;
        }
      }
      BigDecimal payment =
          transactionRepository.getPaymentSumUser(date.minusDays(i), date.minusDays((i - 1)), user);
      payment = payment == null ? BigDecimal.ZERO : payment;
      BigDecimal refund =
          transactionRepository.getRefundSumUser(date.minusDays(i), date.minusDays((i - 1)), user);
      refund = refund == null ? BigDecimal.ZERO : refund;
      stats.add(
          new Statistic(
              Statistic.StatisticType.OUTGOING_FUNDS,
              (payment.add(refund)).negate(),
              date.minusDays(i)));
    }
    return stats;
  }

  /**
   * Computes the average monthly funds per user based on the provided statistics and time span.
   *
   * @param stats a list of Statistic objects representing the financial data.
   * @param daysBack the number of days to look back for calculating the average.
   * @return the computed average monthly funds per user as a BigDecimal. Returns BigDecimal.ZERO if
   *     the input list is empty, the number of days is less than one, or the sum of funds is zero.
   */
  @PreAuthorize("hasRole('ADMIN')")
  public BigDecimal getAverageMonthlyFundsPerUser(List<Statistic> stats, int daysBack) {
    if (daysBack < 1 || stats.isEmpty()) {
      return BigDecimal.ZERO;
    }
    BigDecimal sum = BigDecimal.ZERO;
    BigDecimal months =
        BigDecimal.valueOf(daysBack).divide(BigDecimal.valueOf(30.43), BigDecimal.ROUND_UP);
    for (Statistic stat : stats) {
      sum = sum.add(stat.getAmount());
    }
    if (sum.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return sum.divide(months, BigDecimal.ROUND_UP);
  }

  /**
   * Retrieves the total sum for a given statistic type over the last month. The method calculates
   * the period starting from one month ago to the current date and time.
   *
   * @param type the type of statistic to calculate the sum for
   * @return the total sum for the given statistic type over the last month, or BigDecimal.ZERO if
   *     no data is found
   */
  @PreAuthorize("hasRole('ADMIN')")
  public BigDecimal getLastMonth(Statistic.StatisticType type) {
    BigDecimal result =
        statisticsRepository.getSumBetweenDates(
            type, LocalDateTime.now().minusMonths(1), LocalDateTime.now());
    return result == null ? BigDecimal.ZERO : result;
  }

  /**
   * Retrieves the total number of transactions available in the repository.
   *
   * @return the total count of transactions as a long value
   */
  @PreAuthorize("hasRole('ADMIN')")
  public long getTransactionCount() {
    return transactionRepository.count();
  }
}
