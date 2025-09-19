package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

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
    List<Object[]> yearMonthCombinations = transactionRepository.findDistinctYearMonthCombinations();
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
}
