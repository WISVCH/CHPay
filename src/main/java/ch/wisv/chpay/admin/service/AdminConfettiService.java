package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.core.model.Confetti;
import ch.wisv.chpay.core.model.ConfettiShape;
import ch.wisv.chpay.core.repository.ConfettiRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminConfettiService {

  private final ConfettiRepository confettiRepository;

  @Autowired
  public AdminConfettiService(ConfettiRepository confettiRepository) {
    this.confettiRepository = confettiRepository;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<Confetti> getAll() {
    return confettiRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Optional<Confetti> getById(UUID id) {
    return confettiRepository.findById(id);
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public Confetti create(
      String name,
      List<ConfettiShape> shapes,
      List<String> colors,
      double scalar,
      int minimumTransactions,
      String group,
      boolean hidden,
      boolean isDefault) {
    boolean shouldBeDefault = isDefault || confettiRepository.countByDefaultConfettiTrue() == 0;
    Confetti confetti =
        new Confetti(
            name, shapes, colors, scalar, minimumTransactions, group, hidden, shouldBeDefault);
    Confetti saved = confettiRepository.save(confetti);
    enforceSingleDefault(saved.getId());
    return saved;
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public Confetti update(
      Confetti confetti,
      String name,
      List<ConfettiShape> shapes,
      List<String> colors,
      double scalar,
      int minimumTransactions,
      String group,
      boolean hidden,
      boolean isDefault) {
    if (!isDefault
        && confetti.isDefaultConfetti()
        && confettiRepository.countByDefaultConfettiTrue() == 1) {
      throw new IllegalStateException("At least one confetti must be set as default.");
    }
    confetti.updateDefinition(
        name, shapes, colors, scalar, minimumTransactions, group, hidden, isDefault);
    Confetti saved = confettiRepository.save(confetti);
    enforceSingleDefault(saved.getId());
    return saved;
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(Confetti confetti) {
    if (confetti.isDefaultConfetti()) {
      throw new IllegalStateException("Default confetti cannot be deleted.");
    }
    confettiRepository.delete(confetti);
  }

  private void enforceSingleDefault(UUID preferredId) {
    List<Confetti> defaults = confettiRepository.findAllByDefaultConfettiTrue();
    if (defaults.isEmpty()) {
      Confetti fallback =
          preferredId != null
              ? confettiRepository.findById(preferredId).orElse(null)
              : null;
      if (fallback == null) {
        fallback =
            confettiRepository.findAll().stream()
                .min(Comparator.comparing(Confetti::getCreatedAt))
                .orElse(null);
      }
      if (fallback != null) {
        fallback.setDefaultConfetti(true);
        confettiRepository.save(fallback);
      }
      return;
    }

    Confetti keep =
        defaults.stream()
            .filter(confetti -> confetti.getId().equals(preferredId))
            .findFirst()
            .orElseGet(
                () ->
                    defaults.stream()
                        .min(Comparator.comparing(Confetti::getCreatedAt))
                        .orElse(defaults.get(0)));
    for (Confetti confetti : defaults) {
      if (!confetti.getId().equals(keep.getId()) && confetti.isDefaultConfetti()) {
        confetti.setDefaultConfetti(false);
        confettiRepository.save(confetti);
      }
    }
  }
}
