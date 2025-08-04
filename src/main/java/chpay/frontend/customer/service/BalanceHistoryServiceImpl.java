package chpay.frontend.customer.service;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.frontend.shared.PaginationInfo;
import chpay.frontend.shared.TransactionJSONConverter;
import chpay.paymentbackend.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BalanceHistoryServiceImpl implements BalanceHistoryService {

  /**
   * Retrieves a list of transaction types based on the input parameter.
   *
   * @param isOnlyPayments if true, only the payment-related transaction type will be included; if
   *     false, additional transaction types such as refunds and top-ups will also be included.
   * @return a list of transaction types that match the specified parameter.
   */
  private List<Transaction.TransactionType> getTransactionTypes(boolean isOnlyPayments) {
    List<Transaction.TransactionType> types = new ArrayList<>();
    types.add(Transaction.TransactionType.TOP_UP);
    if (!isOnlyPayments) {
      types.add(Transaction.TransactionType.REFUND);
      types.add(Transaction.TransactionType.PAYMENT);
      types.add(Transaction.TransactionType.EXTERNAL_PAYMENT);
    }
    return types;
  }

  /***
   * Convert transactions from Java objects to json to be used on the frontend
   * @param user the user by which the transactions will be fetched
   * @param transactionService balance service to pull the transactions
   * @param paginationInfo object holding context to page number nad page size
   * @return list of json objects of transactions
   */
  @Override
  public List<String> getTransactionsByUserAsJSON(
      User user,
      TransactionService transactionService,
      PaginationInfo paginationInfo,
      String sortBy,
      String order,
      boolean isOnlyPayments)
      throws JsonProcessingException {
    // if no transactions found return an empty array
    // otherwise sql pagination gives an error
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }
    List<Transaction.TransactionType> types = getTransactionTypes(isOnlyPayments);

    List<Transaction> transactions =
        transactionService.getTransactionsForUserPageable(
            user,
            paginationInfo.getPage() - 1,
            paginationInfo.getSize(),
            paginationInfo.getSort(sortBy, order),
            types);

    if (transactions == null || transactions.isEmpty()) {
      return new ArrayList<>();
    }

    return TransactionJSONConverter.TransactionsToJSON(transactions);
  }

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param user the user by which the transactions will be fetched
   * @param transactionService balance service to pull the transactions
   * @param page which page to get
   * @param size the size of every page
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  @Override
  public PaginationInfo getTransactionPageInfo(
      User user,
      TransactionService transactionService,
      long page,
      long size,
      boolean isOnlyPayments) {
    List<Transaction.TransactionType> types = getTransactionTypes(isOnlyPayments);
    long transactionsSize = transactionService.countTransactionsByUser(user, types);
    return PaginationInfo.buildPaginationInfo(transactionsSize, (int) page, (int) size);
  }
}
