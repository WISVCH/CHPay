package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.model.Confetti;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction.TransactionStatus;
import ch.wisv.chpay.core.model.transaction.Transaction.TransactionType;
import ch.wisv.chpay.core.repository.ConfettiRepository;
import ch.wisv.chpay.core.repository.TransactionRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfettiEligibilityService {

  private final ConfettiRepository confettiRepository;
  private final TransactionRepository transactionRepository;

  private static final List<TransactionStatus> ELIGIBLE_STATUSES =
      List.of(
          TransactionStatus.SUCCESSFUL,
          TransactionStatus.REFUNDED,
          TransactionStatus.PARTIALLY_REFUNDED);
  private static final List<TransactionType> ELIGIBLE_TYPES =
      List.of(TransactionType.PAYMENT, TransactionType.EXTERNAL_PAYMENT);

  @Autowired
  public ConfettiEligibilityService(
      ConfettiRepository confettiRepository, TransactionRepository transactionRepository) {
    this.confettiRepository = confettiRepository;
    this.transactionRepository = transactionRepository;
  }

  public boolean isConfettiUnlockedForUser(
      Confetti confetti, long eligibleTransactionCount, Collection<String> userGroups) {
    if (confetti == null) {
      return false;
    }

    if (confetti.isDefaultConfetti()) {
      return true;
    }

    if (eligibleTransactionCount < confetti.getMinimumTransactions()) {
      return false;
    }

    String requiredGroup = confetti.getGroup();
    if (requiredGroup == null || requiredGroup.isBlank()) {
      return true;
    }

    if (userGroups == null) {
      return false;
    }

    if (confetti.isGroupStartsWith()) {
      for (String group : userGroups) {
        if (group != null && group.startsWith(requiredGroup)) {
          return true;
        }
      }
      return false;
    }

    return userGroups.contains(requiredGroup);
  }

  @Transactional(readOnly = true)
  public long countEligibleTransactions(User user) {
    if (user == null || user.getId() == null) {
      return 0;
    }
    return transactionRepository.countByUserAndStatusInAndTypeIn(
        user, ELIGIBLE_STATUSES, ELIGIBLE_TYPES);
  }

  @Transactional
  public boolean ensureUserConfetti(User user) {
    if (user == null) {
      return false;
    }

    long eligibleTransactions = countEligibleTransactions(user);
    Confetti current = user.getConfetti();
    if (!isConfettiUnlockedForUser(current, eligibleTransactions, user.getGroups())) {
      Optional<Confetti> fallback = getFallbackDefaultConfetti();
      fallback.ifPresent(user::setConfetti);
      return fallback.isPresent();
    }
    return false;
  }

  @Transactional(readOnly = true)
  public Optional<Confetti> getFallbackDefaultConfetti() {
    Optional<Confetti> defaultConfetti = confettiRepository.findFirstByDefaultConfettiTrue();
    if (defaultConfetti.isPresent()) {
      return defaultConfetti;
    }
    List<Confetti> all = confettiRepository.findAll();
    return all.stream().min(Comparator.comparing(Confetti::getCreatedAt));
  }
}
