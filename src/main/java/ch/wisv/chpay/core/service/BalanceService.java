package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.aop.CheckSystemNotFrozen;
import ch.wisv.chpay.core.exception.IllegalRefundException;
import ch.wisv.chpay.core.exception.InsufficientBalanceException;
import ch.wisv.chpay.core.exception.UserNotFoundException;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService {

  private final UserRepository userRepository;
  private final SettingService settingService;

  @Autowired
  public BalanceService(UserRepository userRepository, SettingService settingService) {
    this.userRepository = userRepository;
    this.settingService = settingService;
  }

  @CheckSystemNotFrozen
  @Transactional
  protected User pay(User user, BigDecimal amount)
      throws InsufficientBalanceException, UserNotFoundException, NoSuchElementException {
    User lockedFrom = userRepository.findByIdForUpdate(user.getId());

    if (lockedFrom == null) {
      throw new UserNotFoundException("User not found");
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Payment amount must be positive");
    }

    if (lockedFrom.getBalance().compareTo(amount) < 0) {
      throw new InsufficientBalanceException("Insufficient balance for payment");
    }

    debit(lockedFrom, amount);
    return lockedFrom;
  }

  @CheckSystemNotFrozen
  @Transactional
  public User topup(User user, java.math.BigDecimal amount) {
    User lockedFrom = userRepository.findByIdForUpdate(user.getId());
    if (lockedFrom == null) {
      throw new UserNotFoundException("User not found");
    }
    credit(lockedFrom, amount);
    return lockedFrom;
  }

  @CheckSystemNotFrozen
  @Transactional
  protected User refund(User user, BigDecimal amount)
      throws IllegalStateException, IllegalArgumentException, IllegalRefundException {
    User lockedFrom = userRepository.findByIdForUpdate(user.getId());
    if (lockedFrom == null) {
      throw new UserNotFoundException("User not found");
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalRefundException("Refund amount must be positive");
    }
    credit(lockedFrom, amount);
    return lockedFrom;
  }

  /**
   * Internal method to add money to a user's balance. Checks if the balance is within the limit.
   *
   * @param user The user to add the money to.
   * @param amount The amount to add to the balance. Must be a positive number.
   */
  private void credit(User user, BigDecimal amount)
      throws IllegalStateException, IllegalArgumentException {
    settingService.assertBalanceWithinLimit(user.getBalance(), amount);

    user.addBalance(amount);

    userRepository.saveAndFlush(user);
  }

  /**
   * Internal method to subtract money from a user's balance.
   *
   * @param user The user to subtract the money from.
   * @param amount The amount to subtract from the balance. Must be a positive number.
   */
  private void debit(User user, BigDecimal amount)
      throws IllegalArgumentException, InsufficientBalanceException {
    user.subtractBalance(amount);
    userRepository.saveAndFlush(user);
  }
}
