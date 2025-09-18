package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.admin.util.TransactionJSONConverter;
import ch.wisv.chpay.core.dto.PaginationInfo;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminTransactionService {

  /**
   * Retrieves a list of transaction types based on the provided input string. If the input string
   * matches one of the predefined transaction types ("top_up", "refund", "payment"), it returns a
   * list with the corresponding transaction type. Otherwise, returns a list of all available
   * transaction types.
   *
   * @param typeString the input string to determine the transaction type(s). Can be "top_up",
   *     "refund", "payment", or others.
   * @return a list of transaction types derived from the given input string, or all types if the
   *     input is "all" or unrecognized.
   */
  private List<Transaction.TransactionType> getTransactionTypes(String typeString) {
    if (typeString.equals("top_up")
        || typeString.equals("refund")
        || typeString.equals("payment")
        || typeString.equals("external_payment")) {
      return List.of(Transaction.TransactionType.valueOf(typeString.toUpperCase()));
    }
    // if its all or the string doesnt match return all types
    return List.of(
        Transaction.TransactionType.TOP_UP,
        Transaction.TransactionType.REFUND,
        Transaction.TransactionType.PAYMENT,
        Transaction.TransactionType.EXTERNAL_PAYMENT);
  }

  /**
   * Retrieves a list of transaction statuses based on the given status string. If the status string
   * matches a valid transaction status (successful, refunded, pending, failed, or
   * partially_refunded), it returns the corresponding status. Otherwise, it returns a list of all
   * possible transaction statuses.
   *
   * @param statusString the status string used to filter transaction statuses. Valid values are
   *     "successful", "refunded", "pending", "failed", and "partially_refunded". Any other value
   *     will result in returning all statuses.
   * @return a list containing the corresponding transaction status if the input matches a valid
   *     status; otherwise, a list of all available transaction statuses.
   */
  private List<Transaction.TransactionStatus> getTransactionStatus(String statusString) {
    if (statusString.equals("successful")
        || statusString.equals("refunded")
        || statusString.equals("pending")
        || statusString.equals("failed")
        || statusString.equals("partially_refunded")) {
      return List.of(Transaction.TransactionStatus.valueOf(statusString.toUpperCase()));
    }
    // if its all or the string doesnt match return all statuses
    return List.of(
        Transaction.TransactionStatus.SUCCESSFUL,
        Transaction.TransactionStatus.REFUNDED,
        Transaction.TransactionStatus.PENDING,
        Transaction.TransactionStatus.FAILED,
        Transaction.TransactionStatus.PARTIALLY_REFUNDED);
  }

  /***
   * Convert transactions from Java objects to json to be used on the frontend
   * @param transactionsService balance service to pull the transactions
   * @param paginationInfo object holding context to page number nad page size
   * @return list of JSON objects of transactions
   */
  public List<String> getTransactionsAllJSON(
      TransactionService transactionsService,
      PaginationInfo paginationInfo,
      String sortBy,
      String order)
      throws JsonProcessingException {
    // return an empty array if no rows are present
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }

    List<Transaction> transactions =
        transactionsService.getAllTransactionsPageable(
            (int) paginationInfo.getPage() - 1,
            (int) paginationInfo.getSize(),
            paginationInfo.getSort(sortBy, order));

    if (transactions == null || transactions.isEmpty()) {
      return new ArrayList<>();
    }
    return TransactionJSONConverter.TransactionsToJSON(transactions);
  }

  /***
   * Convert transactions from Java objects to json to be used on the frontend filtered by date
   * @param transactionsService balance service to pull the transactions
   * @param paginationInfo object holding context to page number nad page size
   * @param startDate start date for filter search in format yyyy-MM-dd
   * @param endDate end date for filter search in format yyyy-MM-dd
   * @return list of json objects of transactions
   */
  public List<String> getTransactionsByDateJSON(
      TransactionService transactionsService,
      PaginationInfo paginationInfo,
      String startDate,
      String endDate,
      String sortBy,
      String order)
      throws JsonProcessingException, DateTimeParseException {
    // return empty array if no rows are present
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }
    // parse the start and end dates
    LocalDateTime start =
        LocalDateTime.of(
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MIN);
    LocalDateTime end =
        LocalDateTime.of(
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MAX);

    List<Transaction> transactions =
        transactionsService.getAllTransactionsByDatePageable(
            start,
            end,
            (int) paginationInfo.getPage(),
            (int) paginationInfo.getSize(),
            paginationInfo.getSort(sortBy, order));

    if (transactions == null || transactions.isEmpty()) {
      return new ArrayList<>();
    }
    return TransactionJSONConverter.TransactionsToJSON(transactions);
  }

  /***
   * Convert transactions from Java objects to json to be used on the frontend filtered by amount
   * @param transactionsService balance service to pull the transactions
   * @param paginationInfo object holding context to page number nad page size
   * @param amountUnder upper bound of amount
   * @param amountOver lower bound of amount
   * @return list of json objects of transactions
   */
  public List<String> getTransactionsByAmountJSON(
      TransactionService transactionsService,
      PaginationInfo paginationInfo,
      String amountUnder,
      String amountOver,
      String sortBy,
      String order)
      throws JsonProcessingException, NumberFormatException {
    // return an empty array if no rows are present
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }
    // parse the amount bounds
    BigDecimal amountUnderDecimal = new BigDecimal(amountUnder);
    BigDecimal amountOverDecimal = new BigDecimal(amountOver);
    List<Transaction> transactions =
        transactionsService.getAllTransactionsByAmountPageable(
            amountUnderDecimal,
            amountOverDecimal,
            (int) paginationInfo.getPage() - 1,
            (int) paginationInfo.getSize(),
            paginationInfo.getSort(sortBy, order));

    if (transactions == null || transactions.isEmpty()) {
      return new ArrayList<>();
    }
    return TransactionJSONConverter.TransactionsToJSON(transactions);
  }

  /***
   * Convert transactions from Java objects to json to be used on the frontend filtered by users
   * @param transactionsService balance service to pull the transactions
   * @param paginationInfo object holding context to page number nad page size
   * @param users list of users
   * @return list of json objects of transactions
   */
  public List<String> getTransactionsByUsersJSON(
      TransactionService transactionsService,
      PaginationInfo paginationInfo,
      List<User> users,
      String sortBy,
      String order)
      throws JsonProcessingException, NumberFormatException {
    // return empty array if no rows are present
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }

    List<Transaction> transactions =
        transactionsService.getAllTransactionsByUserListPageable(
            users,
            (int) paginationInfo.getPage() - 1,
            (int) paginationInfo.getSize(),
            paginationInfo.getSort(sortBy, order));

    if (transactions == null || transactions.isEmpty()) {
      return new ArrayList<>();
    }
    return TransactionJSONConverter.TransactionsToJSON(transactions);
  }

  /***
   * Convert transactions from Java objects to json to be used on the frontend filtered by description
   * @param transactionsService balance service to pull the transactions
   * @param paginationInfo object holding context to page number nad page size
   * @param description description query
   * @return list of json objects of transactions
   */
  public List<String> getTransactionsByDescriptionJSON(
      TransactionService transactionsService,
      PaginationInfo paginationInfo,
      String description,
      String sortBy,
      String order)
      throws JsonProcessingException, NumberFormatException {
    // return an empty array if no rows are present
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }

    List<Transaction> transactions =
        transactionsService.getAllTransactionsByDescriptionPageable(
            description,
            (int) paginationInfo.getPage() - 1,
            (int) paginationInfo.getSize(),
            paginationInfo.getSort(sortBy, order));

    if (transactions == null || transactions.isEmpty()) {
      return new ArrayList<>();
    }
    return TransactionJSONConverter.TransactionsToJSON(transactions);
  }

  /**
   * Retrieves and returns a JSON representation of transactions filtered by type and status, based
   * on the given pagination information. If no transactions match the criteria, an empty list is
   * returned.
   *
   * @param transactionsService the service to fetch transactions from
   * @param paginationInfo the pagination details such as current page and size
   * @param type the type of transactions to filter
   * @param status the status of transactions to filter
   * @return a list of JSON strings representing the filtered transactions
   * @throws JsonProcessingException if there is an error processing JSON conversion
   * @throws NumberFormatException if pagination info contains invalid number formats
   */
  public List<String> getTransactionsByTypeAndStatusJSON(
      TransactionService transactionsService,
      PaginationInfo paginationInfo,
      String type,
      String status,
      String sortBy,
      String order)
      throws JsonProcessingException, NumberFormatException {
    // return an empty array if no rows are present
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }

    List<Transaction> transactions =
        transactionsService.getTransactionsForTypeAndStatusPageable(
            getTransactionTypes(type),
            getTransactionStatus(status),
            (int) paginationInfo.getPage() - 1,
            (int) paginationInfo.getSize(),
            paginationInfo.getSort(sortBy, order));

    if (transactions == null || transactions.isEmpty()) {
      return new ArrayList<>();
    }
    return TransactionJSONConverter.TransactionsToJSON(transactions);
  }

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param transactionsService balance service to pull the transactions
   * @param page which page to get
   * @param size the size of every page
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  public PaginationInfo getTransactionPageInfo(
      TransactionService transactionsService, int page, int size) {
    long transactionsSize = transactionsService.countTransactionsAll();
    return PaginationInfo.buildPaginationInfo(transactionsSize, page, size);
  }

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param transactionsService balance service to pull the transactions
   * @param page which page to get
   * @param size the size of every page
   * @param startDate start of range
   * @param endDate end of range
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  public PaginationInfo getTransactionByDatePageInfo(
      TransactionService transactionsService, int page, int size, String startDate, String endDate)
      throws DateTimeParseException {
    LocalDateTime start =
        LocalDateTime.of(
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MIN);
    LocalDateTime end =
        LocalDateTime.of(
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.MAX);
    long transactionsSize = transactionsService.countTransactionsByDate(start, end);
    return PaginationInfo.buildPaginationInfo(transactionsSize, page, size);
  }

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param transactionsService balance service to pull the transactions
   * @param page which page to get
   * @param size the size of every page
   * @param amountUnder end of range
   * @param amountOver start of range
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  public PaginationInfo getTransactionByAmountPageInfo(
      TransactionService transactionsService,
      int page,
      int size,
      String amountUnder,
      String amountOver)
      throws NumberFormatException {
    BigDecimal amountUnderDecimal = new BigDecimal(amountUnder);
    BigDecimal amountOverDecimal = new BigDecimal(amountOver);
    long transactionsSize =
        transactionsService.countTransactionsByAmount(amountUnderDecimal, amountOverDecimal);
    return PaginationInfo.buildPaginationInfo(transactionsSize, page, size);
  }

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param transactionsService balance service to pull the transactions
   * @param page which page to get
   * @param size the size of every page
   * @param users users by whom to fetch transactions
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  public PaginationInfo getTransactionByUsersInPageInfo(
      TransactionService transactionsService, int page, int size, List<User> users) {
    long transactionsSize = transactionsService.countTransactionsByUsersIn(users);
    return PaginationInfo.buildPaginationInfo(transactionsSize, page, size);
  }

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param transactionsService balance service to pull the transactions
   * @param page which page to get
   * @param size the size of every page
   * @param description query of descriptions
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  public PaginationInfo getTransactionByDescriptionPageInfo(
      TransactionService transactionsService, int page, int size, String description) {
    long transactionsSize = transactionsService.countTransactionsByDescription(description);
    return PaginationInfo.buildPaginationInfo(transactionsSize, page, size);
  }

  /**
   * Retrieves pagination information for transactions filtered by type and status.
   *
   * @param transactionsService the service instance used to fetch transaction data
   * @param page the current page number for pagination
   * @param size the number of records per page
   * @param type the type of transactions to filter
   * @param status the status of transactions to filter
   * @return a PaginationInfo object containing details about the pagination state
   */
  public PaginationInfo getTransactionByTypeAndStatusPageInfo(
      TransactionService transactionsService, int page, int size, String type, String status) {
    long transactionsSize =
        transactionsService.countTransactionsByTypeAndStatus(
            getTransactionTypes(type), getTransactionStatus(status));
    return PaginationInfo.buildPaginationInfo(transactionsSize, page, size);
  }
}
