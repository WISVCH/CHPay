package chpay.paymentbackend.service;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public User getUserById(String id) {
    return userRepository.findById(UUID.fromString(id)).orElse(null);
  }

  /**
   * Retrieves all users from the database, paginated based on the provided page and size
   * parameters. This method is restricted to users with the ADMIN role.
   *
   * @param page the zero-based page index to retrieve.
   * @param size the number of users to retrieve per page.
   * @return a list of users for the requested page and size.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getAllUsers(int page, int size) {
    return userRepository.findBy(PageRequest.of(page, size));
  }

  /**
   * Retrieves a paginated list of all users sorted according to the specified criteria.
   *
   * @param page the page number to retrieve, zero-based
   * @param size the number of users per page
   * @param sort the sorting criteria for the list
   * @return a list of users matching the specified pagination and sorting criteria
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getAllUsers(int page, int size, Sort sort) {
    return userRepository.findBy(PageRequest.of(page, size, sort));
  }

  /**
   * Retrieves a list of users based on the provided space-separated OpenID query.
   *
   * @param query a space-separated string of OpenIDs to search for users
   * @return a list of User objects corresponding to the given OpenIDs; returns null for any OpenID
   *     that does not match a user
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByOpenIdQuery(String query) {
    List<User> users = new ArrayList<User>();
    for (String s : query.split(" ")) {
      users.add(userRepository.findByOpenID(s).orElse(null));
    }
    return users;
  }

  /**
   * Fetches a list of users whose names contain the specified query string, ignoring case
   * sensitivity.
   *
   * @param query the string used to search for matching user names
   * @return a list of users whose names match the query
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByNameQuery(String query) {
    return userRepository.findAllByNameContainingIgnoreCase(query);
  }

  /**
   * Retrieves a list of users whose names partially match the given query, using a case-insensitive
   * search and pagination.
   *
   * @param query The search query to match user names against.
   * @param page The page number (zero-based) to retrieve.
   * @param size The size of the page (number of users per page).
   * @return A list of users matching the query within the specified page.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByNameQueryPageable(String query, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return userRepository.findAllByNameContainingIgnoreCase(query, pageable);
  }

  /**
   * Retrieves a paginated and sorted list of users whose names contain the specified query string,
   * ignoring case sensitivity.
   *
   * @param query the substring to search for within user names.
   * @param page the page number to retrieve (0-based index).
   * @param size the number of users to include in a single page.
   * @param sort the sorting criteria to apply to the results.
   * @return a list of users matching the query string within the specified page, sorted by the
   *     given criteria.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByNameQueryPageable(String query, int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return userRepository.findAllByNameContainingIgnoreCase(query, pageable);
  }

  /**
   * Retrieves a paginated list of users whose email addresses contain the specified query string,
   * ignoring case sensitivity. The results are limited to the given page and size parameters.
   *
   * @param query The substring to search for within user email addresses.
   * @param page The page number to retrieve (0-based index).
   * @param size The number of users to include in a single page.
   * @return A list of users whose email addresses match the query string, paginated by the given
   *     parameters.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByEmailQueryPageable(String query, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return userRepository.findAllByEmailContainingIgnoreCase(query, pageable);
  }

  /**
   * Retrieves a paginated and sorted list of users whose email addresses contain the specified
   * query string, ignoring case sensitivity.
   *
   * @param query the substring to search for within user email addresses.
   * @param page the page number to retrieve (0-based index).
   * @param size the number of users to include in a single page.
   * @param sort the sorting criteria to apply to the results.
   * @return a list of users matching the query string within the specified page, sorted by the
   *     given criteria.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByEmailQueryPageable(String query, int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return userRepository.findAllByEmailContainingIgnoreCase(query, pageable);
  }

  /**
   * Retrieves a paginated list of users whose balance falls within the specified range.
   *
   * @param balanceAfter the minimum balance value (inclusive) to filter the users.
   * @param balanceBefore the maximum balance value (exclusive) to filter the users.
   * @param page the page number to retrieve (zero-based).
   * @param size the number of users to include per page.
   * @return a list of users whose balance ranges between the specified values, paginated according
   *     to the provided page and size.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByBalancePageable(
      BigDecimal balanceAfter, BigDecimal balanceBefore, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return userRepository.findAllByBalanceBetween(balanceAfter, balanceBefore, pageable);
  }

  /**
   * Retrieves a paginated and sorted list of users whose balance falls within the specified range.
   *
   * @param balanceAfter the minimum balance value (inclusive) to filter the users.
   * @param balanceBefore the maximum balance value (exclusive) to filter the users.
   * @param page the page number to retrieve (zero-based).
   * @param size the number of users to include per page.
   * @param sort the sorting criteria to apply to the results.
   * @return a list of users whose balance ranges between the specified values, paginated and sorted
   *     according to the provided criteria.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getUsersByBalancePageable(
      BigDecimal balanceAfter, BigDecimal balanceBefore, int page, int size, Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    return userRepository.findAllByBalanceBetween(balanceAfter, balanceBefore, pageable);
  }

  /**
   * Counts all user entities in the system.
   *
   * @return the total number of user entities
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countAll() {
    return userRepository.count();
  }

  /**
   * Saves a user in the repository
   *
   * @param user The user
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void saveAndFlush(User user) {
    userRepository.saveAndFlush(user);
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public User getUserByIdForUpdate(UUID openID) {
    return userRepository.findByIdForUpdate(openID);
  }

  /**
   * Counts the number of users whose names contain the specified query string, ignoring case.
   *
   * @param query the query string to search for in user names
   * @return the number of users whose names match the query
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countUsersByNameQuery(String query) {
    return userRepository.countAllByNameContainingIgnoreCase(query);
  }

  /**
   * Counts the number of users whose email contains the specified query string, ignoring case.
   *
   * @param query the email query string to search for
   * @return the number of users matching the email query
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countUsersByEmailQuery(String query) {
    return userRepository.countAllByEmailContainingIgnoreCase(query);
  }

  /**
   * Counts the number of users whose balance falls between the specified range.
   *
   * @param balanceAfter the lower bound of the balance range (inclusive)
   * @param balanceBefore the upper bound of the balance range (inclusive)
   * @return the count of users whose balance is within the specified range
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public long countUsersByBalance(BigDecimal balanceAfter, BigDecimal balanceBefore) {
    return userRepository.countAllByBalanceBetween(balanceAfter, balanceBefore);
  }

  /**
   * Changes a user's RFID id.
   *
   * @param openId of the user to perform the change on
   * @param newRfid to replace the old RFID with
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void changeUserRfid(String openId, String newRfid) {
    User user =
        userRepository
            .findByOpenID(openId)
            .orElseThrow(() -> new NoSuchElementException("No user found for OpenID: " + openId));
    user.setRfid(newRfid);
    userRepository.save(user);
  }

  /**
   * Clears a given user's RFID.
   *
   * @param openId
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void clearUserRfid(String openId) {
    User user =
        userRepository
            .findByOpenID(openId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + openId));
    user.setRfid(null);
    userRepository.save(user);
  }
}
