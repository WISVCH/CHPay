package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.SystemSettings;
import chpay.DatabaseHandler.transactiondb.repositories.SystemSettingsRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Lazy;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

  @Mock private SystemSettingsRepository repo;

  @Mock @Lazy private SettingService selfProxy;

  @Spy @InjectMocks private SettingService settingService;

  private SystemSettings testSettings;

  @BeforeEach
  void setUp() {
    testSettings = new SystemSettings();
    testSettings.setFrozen(false);
    testSettings.setMaxBalance(new BigDecimal("500.00"));
    testSettings.setMinTopUp(new BigDecimal("10.00"));

    lenient().when(selfProxy.isFrozen()).thenReturn(false);
    lenient().when(selfProxy.getMaxBalance()).thenReturn(new BigDecimal("500.00"));
    lenient().when(selfProxy.getMinTopUp()).thenReturn(new BigDecimal("10.00"));
  }

  @Test
  void init_settingsExist_doesNotCreateNewSettings() {
    when(repo.findById(1)).thenReturn(Optional.of(testSettings));

    settingService.init();

    verify(repo).findById(1);
    verify(repo, never()).save(any());
  }

  @Test
  void init_settingsDoNotExist_createsNewSettings() {
    when(repo.findById(1)).thenReturn(Optional.empty());
    when(repo.save(any(SystemSettings.class))).thenReturn(testSettings);

    settingService.init();

    verify(repo).findById(1);
    verify(repo).save(any(SystemSettings.class));
  }

  @Test
  void isFrozen_settingsExist_returnsCorrectValue() {
    testSettings.setFrozen(true);
    when(repo.findById(1)).thenReturn(Optional.of(testSettings));

    boolean result = settingService.isFrozen();

    assertTrue(result);
    verify(repo).findById(1);
  }

  @Test
  void isFrozen_settingsDoNotExist_returnsFalse() {
    when(repo.findById(1)).thenReturn(Optional.empty());

    boolean result = settingService.isFrozen();

    assertFalse(result);
    verify(repo).findById(1);
  }

  @Test
  void assertNotFrozen_systemNotFrozen_doesNotThrowException() {
    when(selfProxy.isFrozen()).thenReturn(false);

    assertDoesNotThrow(() -> settingService.assertNotFrozen());
    verify(selfProxy).isFrozen();
  }

  @Test
  void assertNotFrozen_systemFrozen_throwsException() {
    when(selfProxy.isFrozen()).thenReturn(true);

    assertThrows(IllegalStateException.class, () -> settingService.assertNotFrozen());
    verify(selfProxy).isFrozen();
  }

  @Test
  void setFrozen_validInput_updatesFrozenState() {
    when(repo.findById(1)).thenReturn(Optional.of(testSettings));

    settingService.setFrozen(true);

    assertTrue(testSettings.isFrozen());
    verify(repo).findById(1);
    verify(repo).save(testSettings);
  }

  @Test
  void getMaxBalance_settingsExist_returnsCorrectValue() {
    BigDecimal expectedMaxBalance = new BigDecimal("500.00");
    testSettings.setMaxBalance(expectedMaxBalance);
    when(repo.findById(1)).thenReturn(Optional.of(testSettings));

    BigDecimal result = settingService.getMaxBalance();

    assertEquals(expectedMaxBalance, result);
    verify(repo).findById(1);
  }

  @Test
  void getMaxBalance_settingsDoNotExist_returnsDefaultValue() {
    when(repo.findById(1)).thenReturn(Optional.empty());

    BigDecimal result = settingService.getMaxBalance();

    assertEquals(new BigDecimal("500.00"), result);
    verify(repo).findById(1);
  }

  @Test
  void setMaxBalance_validInput_updatesMaxBalance() {
    BigDecimal newMaxBalance = new BigDecimal("1000.00");
    when(repo.findById(1)).thenReturn(Optional.of(testSettings));

    settingService.setMaxBalance(newMaxBalance);

    assertEquals(newMaxBalance, testSettings.getMaxBalance());
    verify(repo).findById(1);
    verify(repo).save(testSettings);
  }

  @Test
  void assertBalanceWithinLimit_balanceWithinLimit_doesNotThrowException() {
    BigDecimal currentBalance = new BigDecimal("300.00");
    BigDecimal amountToAdd = new BigDecimal("100.00");
    when(selfProxy.getMaxBalance()).thenReturn(new BigDecimal("500.00"));

    assertDoesNotThrow(() -> settingService.assertBalanceWithinLimit(currentBalance, amountToAdd));
    verify(selfProxy).getMaxBalance();
  }

  @Test
  void assertBalanceWithinLimit_balanceExceedsLimit_throwsException() {
    BigDecimal currentBalance = new BigDecimal("400.00");
    BigDecimal amountToAdd = new BigDecimal("200.00");
    when(selfProxy.getMaxBalance()).thenReturn(new BigDecimal("500.00"));

    assertThrows(
        IllegalStateException.class,
        () -> settingService.assertBalanceWithinLimit(currentBalance, amountToAdd));
    verify(selfProxy).getMaxBalance();
  }

  @Test
  void assertBalanceWithinLimit_exactlyAtLimit_doesNotThrowException() {
    BigDecimal currentBalance = new BigDecimal("300.00");
    BigDecimal amountToAdd = new BigDecimal("200.00");
    when(selfProxy.getMaxBalance()).thenReturn(new BigDecimal("500.00"));

    assertDoesNotThrow(() -> settingService.assertBalanceWithinLimit(currentBalance, amountToAdd));
    verify(selfProxy).getMaxBalance();
  }

  @Test
  void getMinTopUp_settingsExist_returnsCorrectValue() {
    BigDecimal expectedMinTopUp = new BigDecimal("10.00");
    testSettings.setMinTopUp(expectedMinTopUp);
    when(repo.findById(1)).thenReturn(Optional.of(testSettings));
    assertEquals(expectedMinTopUp, settingService.getMinTopUp());
  }

  @Test
  void setMinTopUp_validInput_updatesMinTopUp() {
    BigDecimal newMinTopUp = new BigDecimal("20.00");
    when(repo.findById(1)).thenReturn(Optional.of(testSettings));

    settingService.setMinTopUp(newMinTopUp);

    assertEquals(newMinTopUp, testSettings.getMinTopUp());
    verify(repo).findById(1);
    verify(repo).save(testSettings);
  }

  @Test
  void assertTopUpThrows() {
    BigDecimal newAmount = new BigDecimal(5);
    when(selfProxy.getMinTopUp()).thenReturn(new BigDecimal(10));

    assertThrows(
        IllegalArgumentException.class, () -> settingService.assertTopUpWithinLimit(newAmount));
    verify(selfProxy).getMinTopUp();
  }

  @Test
  void assertTopUpDoesNotThrowAtLimit() {
    BigDecimal newAmount = new BigDecimal(10);
    when(selfProxy.getMinTopUp()).thenReturn(new BigDecimal(10));

    assertDoesNotThrow(() -> settingService.assertTopUpWithinLimit(newAmount));
    verify(selfProxy).getMinTopUp();
  }
}
