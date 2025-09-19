package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
