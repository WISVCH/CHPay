package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.admin.model.BalanceEntry;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.service.TransactionService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service for handling admin user-related operations. */
@Service
public class AdminUserService {

  private final TransactionService transactionService;

  @Autowired
  public AdminUserService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  /**
   * Calculates a user's balance history by processing their transactions chronologically.
   *
   * @param user the user to calculate balance history for
   * @return list of balance entries showing balance progression over time
   */
  public List<BalanceEntry> calculateUserBalanceHistory(User user) {
    List<Transaction> transactions = transactionService.getTransactionsForUser(user);
    transactions.sort((t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp()));
    return calculateBalanceOverTime(transactions);
  }

  /**
   * Processes a list of transactions chronologically to build balance history. Only includes
   * transactions that affect the balance (successful, refunded, partially refunded).
   *
   * @param transactions list of transactions sorted by timestamp
   * @return list of balance entries showing balance progression over time
   */
  public List<BalanceEntry> calculateBalanceOverTime(List<Transaction> transactions) {
    List<BalanceEntry> balanceHistory = new ArrayList<>();
    BigDecimal currentBalance = BigDecimal.ZERO;

    for (Transaction transaction : transactions) {
      if (isTransactionIncludedInBalance(transaction.getStatus())) {
        BigDecimal amount = transaction.getAmount();
        currentBalance = currentBalance.add(amount != null ? amount : BigDecimal.ZERO);

        balanceHistory.add(
            new BalanceEntry(
                transaction.getTimestamp(),
                currentBalance,
                transaction.getId().toString(),
                transaction.getDescription(),
                transaction.getType().toString()));
      }
    }

    return balanceHistory;
  }

  /**
   * Determines if a transaction should be included in balance calculation. Includes successful
   * transactions and transactions that were later refunded.
   *
   * @param status the transaction status to check
   * @return true if the transaction affects the balance
   */
  private boolean isTransactionIncludedInBalance(Transaction.TransactionStatus status) {
    return status == Transaction.TransactionStatus.SUCCESSFUL
        || status == Transaction.TransactionStatus.REFUNDED
        || status == Transaction.TransactionStatus.PARTIALLY_REFUNDED;
  }
}
