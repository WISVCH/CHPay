package chpay.frontend.customer.service;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;

public interface BalanceHistoryService {
  /***
   * This method should intercept the grabbing the transaction so its passed as a JSON sting to the frontend
   * @param user the user by which the transactions will be fetched
   * @param transactionService balance service to pull the transactions
   * @return get the transactions as JSON array
   */
  List<String> getTransactionsByUserAsJSON(
      User user,
      TransactionService transactionService,
      PaginationInfo paginationInfo,
      String sortBy,
      String order,
      boolean onlyPayments)
      throws JsonProcessingException;

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param user the user by which the transactions will be fetched
   * @param transactionService balance service to pull the transactions
   * @param page which page to get
   * @param size the size of every page
   * @param isOnlyPayments should the transactions be of only payment type
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  public PaginationInfo getTransactionPageInfo(
      User user,
      TransactionService transactionService,
      long page,
      long size,
      boolean isOnlyPayments);
}
