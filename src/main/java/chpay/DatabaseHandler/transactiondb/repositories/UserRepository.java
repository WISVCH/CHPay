package chpay.DatabaseHandler.transactiondb.repositories;

import chpay.DatabaseHandler.transactiondb.entities.User;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByOpenID(String openId);

  Optional<User> findById(UUID id);

  List<User> findBy(Pageable pageable);

  List<User> findAllByNameContainingIgnoreCase(String name);

  @Query("SELECT SUM(u.balance) FROM User u")
  BigDecimal getBalanceNow();

  List<User> findAllByNameContainingIgnoreCase(String name, Pageable pageable);

  List<User> findAllByBalanceBetween(
      BigDecimal balanceAfter, BigDecimal balanceBefore, Pageable pageable);

  List<User> findAllByEmailContainingIgnoreCase(String email, Pageable pageable);

  long countAllByBalanceBetween(BigDecimal balanceAfter, BigDecimal balanceBefore);

  long countAllByNameContainingIgnoreCase(String name);

  long countAllByEmailContainingIgnoreCase(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.openID = :openID")
  Optional<User> findAndLockByOpenID(String openID);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  User findByIdForUpdate(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.openID = :open_id")
  User findByOpenIDForUpdate(@Param("open_id") String open_id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.email = :email")
  Optional<User> findAndLockByEmail(String email);

  Optional<User> findByRfid(String rfid);

  Optional<User> findByEmail(String email);
}
