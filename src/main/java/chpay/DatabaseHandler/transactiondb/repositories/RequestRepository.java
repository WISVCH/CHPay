package chpay.DatabaseHandler.transactiondb.repositories;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
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

  @Query(
      value =
          """
           SELECT * FROM requests
           WHERE fulfilled = FALSE
             AND created_at < :cutoff
           FOR UPDATE SKIP LOCKED
           """,
      nativeQuery = true)
  List<PaymentRequest> findOldRequestsForUpdate(@Param("cutoff") LocalDateTime cutoff);
}
