package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.admin.util.UserJSONConverter;
import ch.wisv.chpay.core.dto.PaginationInfo;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminUserService {
  /**
   * Retrieves a list of users in JSON format, based on the given pagination and sorting parameters.
   * If the pagination details indicate zero total transactions or if no users are found, an empty
   * list is returned.
   *
   * @param userService the service used to manage and fetch user information
   * @param paginationInfo the object containing pagination details such as page size, total
   *     transactions, and sorting preferences
   * @param sortBy the field to sort the users by (e.g., name, email); can be null if sorting is not
   *     specified
   * @param order the order of sorting (e.g., "asc" for ascending, "desc" for descending); can be
   *     null if sorting is not specified
   * @return a list of users in JSON format; if no users are found, an empty list is returned
   * @throws JsonProcessingException if there is an error converting the users into JSON format
   */
  public List<String> getUsersAllJSON(
      UserService userService, PaginationInfo paginationInfo, String sortBy, String order)
      throws JsonProcessingException {
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }
    List<User> users = new ArrayList<>();
    if (sortBy == null || order == null) {
      // query users without any sorting
      users =
          userService.getAllUsers(
              (int) paginationInfo.getPage() - 1, (int) paginationInfo.getSize());
    } else {
      users =
          userService.getAllUsers(
              (int) paginationInfo.getPage() - 1,
              (int) paginationInfo.getSize(),
              paginationInfo.getSort(sortBy, order));
    }

    if (users == null || users.isEmpty()) {
      return new ArrayList<>();
    }
    return UserJSONConverter.UsersToJSON(users);
  }

  /**
   * Retrieves a paginated and optionally sorted list of users within a specified balance range and
   * converts it to a JSON representation.
   *
   * @param userService the service used to fetch user data
   * @param paginationInfo an object containing pagination parameters (e.g., page number, size)
   * @param balanceAfter the lower bound of the user's balance, represented as a string
   * @param balanceBefore the upper bound of the user's balance, represented as a string
   * @param sortBy the field by which to sort the results (optional)
   * @param order the order of sorting, either "asc" for ascending or "desc" for descending
   *     (optional)
   * @return a list of JSON strings representing the users within the specified balance range
   * @throws JsonProcessingException if an error occurs during JSON processing of the user objects
   * @throws NumberFormatException if the balanceAfter or balanceBefore parameters cannot be
   *     converted to BigDecimal
   */
  public List<String> getUsersByBalanceJSON(
      UserService userService,
      PaginationInfo paginationInfo,
      String balanceAfter,
      String balanceBefore,
      String sortBy,
      String order)
      throws JsonProcessingException, NumberFormatException {
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }
    BigDecimal balanceAfterDecimal = new BigDecimal(balanceAfter);
    BigDecimal balanceBeforeDecimal = new BigDecimal(balanceBefore);

    List<User> users = new ArrayList<>();
    if (sortBy == null || order == null) {
      // query users without any sorting
      users =
          userService.getUsersByBalancePageable(
              balanceAfterDecimal,
              balanceBeforeDecimal,
              (int) paginationInfo.getPage() - 1,
              (int) paginationInfo.getSize());
    } else {
      users =
          userService.getUsersByBalancePageable(
              balanceAfterDecimal,
              balanceBeforeDecimal,
              (int) paginationInfo.getPage() - 1,
              (int) paginationInfo.getSize(),
              paginationInfo.getSort(sortBy, order));
    }

    if (users == null || users.isEmpty()) {
      return new ArrayList<>();
    }
    return UserJSONConverter.UsersToJSON(users);
  }

  /**
   * Retrieves a list of users filtered by their names in JSON format. The method supports
   * pagination and optional sorting based on the provided criteria.
   *
   * @param userService the service used to interact with user data
   * @param paginationInfo the pagination information including page number, size, and total
   *     transactions
   * @param query the search query used to filter users by their names
   * @param sortBy the attribute to sort the results by (optional, can be null)
   * @param order the sorting order, which can be "asc" or "desc" (optional, can be null)
   * @return a list of users in JSON format; if no users match the criteria or no total transactions
   *     are present, returns an empty list
   * @throws JsonProcessingException if an error occurs during the conversion of user objects to
   *     JSON
   */
  public List<String> getUsersByNameJSON(
      UserService userService,
      PaginationInfo paginationInfo,
      String query,
      String sortBy,
      String order)
      throws JsonProcessingException {
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }

    List<User> users = new ArrayList<>();
    if (sortBy == null || order == null) {
      // query users without any sorting
      users =
          userService.getUsersByNameQueryPageable(
              query, (int) paginationInfo.getPage() - 1, (int) paginationInfo.getSize());
    } else {
      users =
          userService.getUsersByNameQueryPageable(
              query,
              (int) paginationInfo.getPage() - 1,
              (int) paginationInfo.getSize(),
              paginationInfo.getSort(sortBy, order));
    }

    if (users == null || users.isEmpty()) {
      return new ArrayList<>();
    }
    return UserJSONConverter.UsersToJSON(users);
  }

  /**
   * Retrieves a paginated list of users filtered by a given email query and converts the result
   * into a JSON representation.
   *
   * @param userService the service used to fetch user information based on the email query
   * @param paginationInfo an object containing pagination details such as page number, size, and
   *     sorting information
   * @param query the email search query used to filter users
   * @param sortBy the field by which to sort the results, can be null for no sorting
   * @param order the sorting order (e.g., "asc" or "desc"), can be null for no sorting
   * @return a list of user details in JSON format as strings or an empty list if no users are found
   * @throws JsonProcessingException if an error occurs during JSON conversion
   */
  public List<String> getUsersByEmailJSON(
      UserService userService,
      PaginationInfo paginationInfo,
      String query,
      String sortBy,
      String order)
      throws JsonProcessingException {
    if (paginationInfo.getTotalTransactions() == 0) {
      return new ArrayList<>();
    }
    List<User> users = new ArrayList<>();

    if (sortBy == null || order == null) {
      // query users without any sorting
      users =
          userService.getUsersByEmailQueryPageable(
              query, (int) paginationInfo.getPage() - 1, (int) paginationInfo.getSize());
    } else {
      users =
          userService.getUsersByEmailQueryPageable(
              query,
              (int) paginationInfo.getPage() - 1,
              (int) paginationInfo.getSize(),
              paginationInfo.getSort(sortBy, order));
    }

    if (users == null || users.isEmpty()) {
      return new ArrayList<>();
    }
    return UserJSONConverter.UsersToJSON(users);
  }

  /**
   * Retrieves pagination information for all users based on the total number of users and the
   * provided pagination parameters.
   *
   * @param userService the service used to fetch user information and count the total number of
   *     users
   * @param page the current page number to retrieve
   * @param size the number of records per page
   * @return a PaginationInfo object containing information about the current page, total records,
   *     and navigation details (e.g., whether there is a next or previous page)
   * @throws NumberFormatException if the provided pagination parameters are invalid
   */
  public PaginationInfo getUserAllPageInfo(UserService userService, int page, int size)
      throws NumberFormatException {
    long usersSize = userService.countAll();
    return PaginationInfo.buildPaginationInfo(usersSize, page, size);
  }

  /**
   * Retrieves pagination information for users within a specified balance range.
   *
   * @param userService the service used to interact with user data
   * @param page the page number for pagination
   * @param size the number of records per page
   * @param balanceAfter the lower bound of the balance range, represented as a string
   * @param balanceBefore the upper bound of the balance range, represented as a string
   * @return an object of type PaginationInfo containing details about the pagination state
   * @throws NumberFormatException if the balanceAfter or balanceBefore parameters cannot be
   *     converted to BigDecimal
   */
  public PaginationInfo getUserByBalancePageInfo(
      UserService userService, int page, int size, String balanceAfter, String balanceBefore)
      throws NumberFormatException {
    BigDecimal balanceAfterDecimal = new BigDecimal(balanceAfter);
    BigDecimal balanceBeforeDecimal = new BigDecimal(balanceBefore);
    long usersSize = userService.countUsersByBalance(balanceAfterDecimal, balanceBeforeDecimal);
    return PaginationInfo.buildPaginationInfo(usersSize, page, size);
  }

  /**
   * Retrieves pagination information for users based on their names and a search query.
   *
   * @param userService the service used to fetch user information
   * @param page the current page number to retrieve
   * @param size the number of records per page
   * @param query the search query used to filter users by their names
   * @return a PaginationInfo object containing details about the pagination state such as total
   *     records, current page, and page size
   * @throws NumberFormatException if there is an error in parsing numeric values related to
   *     pagination
   */
  public PaginationInfo getUserByNamePageInfo(
      UserService userService, int page, int size, String query) throws NumberFormatException {
    long usersSize = userService.countUsersByNameQuery(query);
    return PaginationInfo.buildPaginationInfo(usersSize, page, size);
  }

  /**
   * Retrieves pagination information for users filtered by their email query.
   *
   * @param userService the service used to perform user-related operations
   * @param page the current page number to retrieve
   * @param size the number of users to include per page
   * @param query the email query used to filter users
   * @return an object of type PaginationInfo containing details about the current page, size, and
   *     total number of transactions
   * @throws NumberFormatException if there is an error in handling numerical values while
   *     calculating pagination
   */
  public PaginationInfo getUserByEmailPageInfo(
      UserService userService, int page, int size, String query) throws NumberFormatException {
    long usersSize = userService.countUsersByEmailQuery(query);
    return PaginationInfo.buildPaginationInfo(usersSize, page, size);
  }
}
