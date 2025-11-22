package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.repository.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
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
   * @return a list of users for the requested page and size.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> getAllUsers() {
    return userRepository.findAll();
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
