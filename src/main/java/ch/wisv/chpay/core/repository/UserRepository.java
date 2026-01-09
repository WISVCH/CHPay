package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.User;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByOpenID(String openId);

  Optional<User> findById(UUID id);

  @Query("SELECT SUM(u.balance) FROM User u")
  BigDecimal getBalanceNow();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.openID = :openID")
  Optional<User> findAndLockByOpenID(String openID);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  User findByIdForUpdate(@Param("id") UUID id);

  Optional<User> findByRfid(String rfid);

  long countByConfetti(ch.wisv.chpay.core.model.Confetti confetti);

  @Modifying
  @Query("UPDATE User u SET u.confetti = :defaultConfetti WHERE u.confetti = :confetti")
  int reassignConfetti(
      @Param("confetti") ch.wisv.chpay.core.model.Confetti confetti,
      @Param("defaultConfetti") ch.wisv.chpay.core.model.Confetti defaultConfetti);
}
