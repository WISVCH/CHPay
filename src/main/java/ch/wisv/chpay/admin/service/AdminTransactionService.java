package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTransactionService {

  private final TransactionRepository transactionRepository;

  @Autowired
  public AdminTransactionService(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  /**
   * Gets all transactions for a given YearMonth.
   *
   * @param yearMonth the YearMonth to filter transactions
   * @return a list of Transaction objects for the specified month
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getTransactionsByYearMonth(YearMonth yearMonth) {
    return transactionRepository.findTransactionsByYearAndMonth(
        yearMonth.getYear(), yearMonth.getMonthValue());
  }

  /**
   * Gets all possible months that have transactions.
   *
   * @return a list of YearMonth objects containing available year-month combinations
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<YearMonth> getAllPossibleMonths() {
    List<Object[]> yearMonthCombinations =
        transactionRepository.findDistinctYearMonthCombinations();
    return yearMonthCombinations.stream()
        .map(obj -> YearMonth.of((Integer) obj[0], (Integer) obj[1]))
        .collect(Collectors.toList());
  }

  /**
   * Gets the most recent month that has transactions.
   *
   * @return the most recent YearMonth with transactions, or current month if none exist
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public YearMonth getMostRecentYearMonth() {
    List<YearMonth> availableMonths = getAllPossibleMonths();
    if (!availableMonths.isEmpty()) {
      return availableMonths.get(0); // First item since ordered DESC
    }
    return YearMonth.now(); // Fallback to current month
  }

  public List<Transaction> getTransactionsByRequestId(UUID requestId) {
    return transactionRepository.findAllByRequestId(requestId);
  }

  /**
   * Gets all transactions for a given user ID.
   *
   * @param userId the UUID of the user
   * @return a list of Transaction objects for the specified user
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getTransactionsByUserId(UUID userId) {
    return transactionRepository.findAllByUserId(userId);
  }

  /**
   * Gets all transactions for a given user ID and year-month.
   *
   * @param userId the UUID of the user
   * @param yearMonth the YearMonth to filter transactions
   * @return a list of Transaction objects for the specified user and month
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getTransactionsByUserIdAndYearMonth(UUID userId, YearMonth yearMonth) {
    return transactionRepository.findTransactionsByUserIdAndYearMonth(
        userId, yearMonth.getYear(), yearMonth.getMonthValue());
  }

  /**
   * Gets all transactions for a given payment request ID and year-month.
   *
   * @param requestId the UUID of the payment request
   * @param yearMonth the YearMonth to filter transactions
   * @return a list of Transaction objects for the specified payment request and month
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Transaction> getTransactionsByRequestIdAndYearMonth(
      UUID requestId, YearMonth yearMonth) {
    return transactionRepository.findTransactionsByRequestIdAndYearMonth(
        requestId, yearMonth.getYear(), yearMonth.getMonthValue());
  }

  /**
   * Gets all possible months that have transactions for a specific user.
   *
   * @param userId the UUID of the user
   * @return a list of YearMonth objects containing available year-month combinations for the user
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<YearMonth> getAllPossibleMonthsForUser(UUID userId) {
    List<Object[]> yearMonthCombinations =
        transactionRepository.findDistinctYearMonthCombinationsByUserId(userId);
    return yearMonthCombinations.stream()
        .map(obj -> YearMonth.of((Integer) obj[0], (Integer) obj[1]))
        .collect(Collectors.toList());
  }

  /**
   * Gets all possible months that have transactions for a specific payment request.
   *
   * @param requestId the UUID of the payment request
   * @return a list of YearMonth objects containing available year-month combinations for the
   *     payment request
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<YearMonth> getAllPossibleMonthsForRequest(UUID requestId) {
    List<Object[]> yearMonthCombinations =
        transactionRepository.findDistinctYearMonthCombinationsByRequestId(requestId);
    return yearMonthCombinations.stream()
        .map(obj -> YearMonth.of((Integer) obj[0], (Integer) obj[1]))
        .collect(Collectors.toList());
  }

  /**
   * Gets the most recent month that has transactions for a specific user.
   *
   * @param userId the UUID of the user
   * @return the most recent YearMonth with transactions for the user, or current month if none
   *     exist
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public YearMonth getMostRecentYearMonthForUser(UUID userId) {
    List<YearMonth> availableMonths = getAllPossibleMonthsForUser(userId);
    if (!availableMonths.isEmpty()) {
      return availableMonths.get(0); // First item since ordered DESC
    }
    return YearMonth.now(); // Fallback to current month
  }

  /**
   * Gets the most recent month that has transactions for a specific payment request.
   *
   * @param requestId the UUID of the payment request
   * @return the most recent YearMonth with transactions for the payment request, or current month
   *     if none exist
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public YearMonth getMostRecentYearMonthForRequest(UUID requestId) {
    List<YearMonth> availableMonths = getAllPossibleMonthsForRequest(requestId);
    if (!availableMonths.isEmpty()) {
      return availableMonths.get(0); // First item since ordered DESC
    }
    return YearMonth.now(); // Fallback to current month
  }
}
