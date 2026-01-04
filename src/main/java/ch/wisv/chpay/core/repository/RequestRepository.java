package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.PaymentRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestRepository extends JpaRepository<PaymentRequest, UUID> {

  Optional<PaymentRequest> findById(UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM PaymentRequest r WHERE r.request_id = :id")
  PaymentRequest findByIdForUpdate(@Param("id") UUID id);
}
