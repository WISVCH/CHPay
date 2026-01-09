package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.Confetti;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfettiRepository extends JpaRepository<Confetti, UUID> {
  long countByDefaultConfettiTrue();

  List<Confetti> findAllByDefaultConfettiTrue();

  Optional<Confetti> findFirstByDefaultConfettiTrue();
}
