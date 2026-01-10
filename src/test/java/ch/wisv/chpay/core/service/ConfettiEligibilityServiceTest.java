package ch.wisv.chpay.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.wisv.chpay.core.model.Confetti;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction.TransactionStatus;
import ch.wisv.chpay.core.model.transaction.Transaction.TransactionType;
import ch.wisv.chpay.core.repository.ConfettiRepository;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ConfettiEligibilityServiceTest {

  @Mock private ConfettiRepository confettiRepository;
  @Mock private TransactionRepository transactionRepository;

  private ConfettiEligibilityService confettiEligibilityService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    confettiEligibilityService =
        new ConfettiEligibilityService(confettiRepository, transactionRepository);
  }

  @Test
  void isConfettiUnlockedForUserReturnsTrueForDefaultConfetti() {
    Confetti confetti = buildConfetti(true, 999, "nope", true);

    boolean unlocked = confettiEligibilityService.isConfettiUnlockedForUser(confetti, 0, List.of());

    assertTrue(unlocked);
  }

  @Test
  void isConfettiUnlockedForUserReturnsFalseWhenBelowMinimumTransactions() {
    Confetti confetti = buildConfetti(false, 5, null, false);

    boolean unlocked = confettiEligibilityService.isConfettiUnlockedForUser(confetti, 2, List.of());

    assertFalse(unlocked);
  }

  @Test
  void isConfettiUnlockedForUserMatchesExactGroup() {
    Confetti confetti = buildConfetti(false, 0, "beheer", false);

    boolean unlocked =
        confettiEligibilityService.isConfettiUnlockedForUser(confetti, 0, List.of("beheer"));

    assertTrue(unlocked);
  }

  @Test
  void isConfettiUnlockedForUserMatchesGroupPrefixWhenEnabled() {
    Confetti confetti = buildConfetti(false, 0, "beheer", true);

    boolean unlocked =
        confettiEligibilityService.isConfettiUnlockedForUser(confetti, 0, List.of("beheer_2026"));

    assertTrue(unlocked);
  }

  @Test
  void isConfettiUnlockedForUserReturnsFalseWhenPrefixDoesNotMatch() {
    Confetti confetti = buildConfetti(false, 0, "beheer", true);

    boolean unlocked =
        confettiEligibilityService.isConfettiUnlockedForUser(confetti, 0, List.of("bestuur"));

    assertFalse(unlocked);
  }

  @Test
  void isConfettiUnlockedForUserReturnsFalseWhenGroupRequiredAndUserGroupsNull() {
    Confetti confetti = buildConfetti(false, 0, "beheer", false);

    boolean unlocked = confettiEligibilityService.isConfettiUnlockedForUser(confetti, 0, null);

    assertFalse(unlocked);
  }

  @Test
  @SuppressWarnings("unchecked")
  void countEligibleTransactionsUsesPaymentAndExternalTypes() {
    User user = buildUserWithId();
    when(transactionRepository.countByUserAndStatusInAndTypeIn(eq(user), anyList(), anyList()))
        .thenReturn(7L);

    long result = confettiEligibilityService.countEligibleTransactions(user);

    assertEquals(7L, result);

    ArgumentCaptor<List<TransactionStatus>> statusesCaptor =
        (ArgumentCaptor<List<TransactionStatus>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<TransactionType>> typesCaptor =
        (ArgumentCaptor<List<TransactionType>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);

    verify(transactionRepository)
        .countByUserAndStatusInAndTypeIn(eq(user), statusesCaptor.capture(), typesCaptor.capture());

    Set<TransactionStatus> statuses = new HashSet<>(statusesCaptor.getValue());
    Set<TransactionType> types = new HashSet<>(typesCaptor.getValue());

    assertEquals(
        Set.of(
            TransactionStatus.SUCCESSFUL,
            TransactionStatus.REFUNDED,
            TransactionStatus.PARTIALLY_REFUNDED),
        statuses);
    assertEquals(Set.of(TransactionType.PAYMENT, TransactionType.EXTERNAL_PAYMENT), types);
  }

  @Test
  void ensureUserConfettiSetsDefaultWhenLocked() {
    User user = buildUserWithId();
    Confetti locked = buildConfetti(false, 10, null, false);
    user.setConfetti(locked);

    Confetti fallback = buildConfetti(true, 0, null, false);
    when(confettiRepository.findFirstByDefaultConfettiTrue()).thenReturn(Optional.of(fallback));
    when(transactionRepository.countByUserAndStatusInAndTypeIn(eq(user), anyList(), anyList()))
        .thenReturn(0L);

    boolean changed = confettiEligibilityService.ensureUserConfetti(user);

    assertTrue(changed);
    assertEquals(fallback, user.getConfetti());
  }

  @Test
  void ensureUserConfettiReturnsFalseWhenAlreadyUnlocked() {
    User user = buildUserWithId();
    Confetti confetti = buildConfetti(false, 0, null, false);
    user.setConfetti(confetti);

    when(transactionRepository.countByUserAndStatusInAndTypeIn(eq(user), anyList(), anyList()))
        .thenReturn(1L);

    boolean changed = confettiEligibilityService.ensureUserConfetti(user);

    assertFalse(changed);
    assertEquals(confetti, user.getConfetti());
    verify(confettiRepository, never()).findFirstByDefaultConfettiTrue();
  }

  private Confetti buildConfetti(
      boolean isDefault, int minimumTransactions, String group, boolean groupStartsWith) {
    return new Confetti(
        "Test",
        List.of(),
        List.of(),
        1.0,
        minimumTransactions,
        group,
        groupStartsWith,
        false,
        isDefault);
  }

  private User buildUserWithId() {
    User user = new User("Test User", "user@example.com", "open-id");
    setUserId(user, UUID.randomUUID());
    return user;
  }

  private void setUserId(User user, UUID id) {
    try {
      Field field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Failed to set user id for tests", ex);
    }
  }
}
