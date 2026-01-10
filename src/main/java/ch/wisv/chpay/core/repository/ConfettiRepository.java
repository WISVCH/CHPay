package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.Confetti;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConfettiRepository extends JpaRepository<Confetti, UUID> {
  long countByDefaultConfettiTrue();

  List<Confetti> findAllByDefaultConfettiTrue();

  Optional<Confetti> findFirstByDefaultConfettiTrue();

  @Query(
      """
      SELECT c.id as confettiId, COUNT(u) as userCount
      FROM Confetti c
      LEFT JOIN User u ON u.confetti = c
      GROUP BY c.id
      """)
  List<ConfettiUsageCount> getUsageCounts();
}
