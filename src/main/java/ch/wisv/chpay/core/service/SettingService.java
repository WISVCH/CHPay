package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.model.SystemSettings;
import ch.wisv.chpay.core.repository.SystemSettingsRepository;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SettingService {

  private final SystemSettingsRepository repo;
  private final SettingService selfProxy;
  private final BigDecimal defaultMinTopUp;

  // To anyone reading this, I have committed far worse code than this here, but this was the
  // beginning of my descent into madness.
  @Autowired
  public SettingService(
      @Lazy SettingService selfProxy,
      SystemSettingsRepository repo,
      @Value("${chpay.settings.minTopUp}") BigDecimal defaultMinTopUp) {
    this.selfProxy = selfProxy;
    this.repo = repo;
    this.defaultMinTopUp = defaultMinTopUp;
  }

  private static final int SETTINGS_ID = 1;

  @PostConstruct
  public void init() {
    repo.findById(SETTINGS_ID)
        .orElseGet(
            () -> {
              SystemSettings settings = new SystemSettings();
              settings.setFrozen(false);
              settings.setMaxBalance(new BigDecimal("120"));
              settings.setMinTopUp(defaultMinTopUp);
              return repo.save(settings);
            });
  }

  /**
   * Checks whether the system is in a frozen state. The frozen state indicates that certain
   * system-wide operations might be restricted.
   *
   * @return true if the system is frozen, false otherwise.
   */
  @Cacheable("systemFrozen")
  public boolean isFrozen() {
    return repo.findById(SETTINGS_ID).map(SystemSettings::isFrozen).orElse(false);
  }

  /**
   * Ensures that the system is not in a frozen state. If the system is frozen, an
   * IllegalStateException is thrown. The frozen state indicates that certain system-wide operations
   * might be restricted.
   *
   * @throws IllegalStateException if the system is currently in a frozen state.
   */
  public void assertNotFrozen() {
    if (selfProxy.isFrozen()) {
      throw new IllegalStateException("System is currently frozen");
    }
  }

  /**
   * Sets the system's frozen state. The frozen state determines whether certain system-wide
   * operations are restricted.
   *
   * @param frozen a boolean indicating the desired frozen state of the system; true to freeze the
   *     system, and false to unfreeze it.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @CacheEvict(value = "systemFrozen", allEntries = true)
  public void setFrozen(boolean frozen) {
    SystemSettings settings = repo.findById(SETTINGS_ID).orElseThrow();
    settings.setFrozen(frozen);
    repo.save(settings);
  }

  /**
   * Gets the current allowed maximum balance. Caches the maximum balance.
   *
   * @return the maximum value.
   */
  @Cacheable(value = "maxBalance")
  public BigDecimal getMaxBalance() {
    return repo.findById(SETTINGS_ID)
        .map(SystemSettings::getMaxBalance)
        .orElse(new BigDecimal("500.00"));
  }

  /**
   * Sets the maximum allowed balance in user wallets system-wide.
   *
   * @param maxBalance the new maximum value.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @CacheEvict(value = "maxBalance", allEntries = true)
  public void setMaxBalance(BigDecimal maxBalance) {
    maxBalance = maxBalance.setScale(2, RoundingMode.DOWN);
    if (maxBalance.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Maximum balance must be positive");
    }
    SystemSettings settings = repo.findById(SETTINGS_ID).orElseThrow();
    settings.setMaxBalance(maxBalance);
    repo.save(settings);
  }

  /**
   * Validates that adding a specified amount to the current balance does not exceed the maximum
   * allowed balance limit in the system. Throws an exception if the new balance would surpass the
   * system's maximum balance.
   *
   * @param currentBalance The current balance before adding the new amount.
   * @param amountToAdd The amount to add to the current balance.
   * @throws IllegalStateException if the new balance exceeds the maximum allowed limit.
   */
  public void assertBalanceWithinLimit(BigDecimal currentBalance, BigDecimal amountToAdd) {
    BigDecimal newBalance = currentBalance.add(amountToAdd);
    BigDecimal max = selfProxy.getMaxBalance();

    if (newBalance.compareTo(max) > 0) {
      throw new IllegalStateException("Balance would exceed the system max of " + max);
    }
  }

  /**
   * Retrieves the minimum top-up value from the system settings. If the value is not found in the
   * repository, a default value of 2.00 is returned.
   *
   * @return the minimum top-up value as a BigDecimal
   */
  @Cacheable(value = "minTopUp")
  public BigDecimal getMinTopUp() {
    return repo.findById(SETTINGS_ID)
        .map(SystemSettings::getMinTopUp)
        .orElse(new BigDecimal("2.00"));
  }

  /**
   * Sets the minimum top-up amount for the system settings. The provided value is rounded down to
   * two decimal places. If the value is zero or negative, an {@code IllegalArgumentException} is
   * thrown.
   *
   * @param minTopUp the minimum top-up amount to be set in the system settings. It must be a
   *     positive {@code BigDecimal}.
   * @throws IllegalArgumentException if the provided {@code minTopUp} value is less than or equal
   *     to zero.
   */
  @CacheEvict(value = "minTopUp", allEntries = true)
  public void setMinTopUp(BigDecimal minTopUp) {
    minTopUp = minTopUp.setScale(2, RoundingMode.DOWN);
    if (minTopUp.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("MinTopUp must be positive");
    }
    SystemSettings settings = repo.findById(SETTINGS_ID).orElseThrow();
    settings.setMinTopUp(minTopUp);
    repo.save(settings);
  }

  /**
   * Validates that the provided top-up amount is not less than the minimum top-up limit.
   *
   * @param topUp the top-up amount to be validated
   * @throws IllegalArgumentException if the top-up amount is less than the minimum top-up limit
   */
  public void assertTopUpWithinLimit(BigDecimal topUp) {
    BigDecimal minTopUp = selfProxy.getMinTopUp();
    if (topUp.compareTo(minTopUp) < 0) {
      throw new IllegalArgumentException("MinTopUp must be greater than " + minTopUp);
    }
  }
}
