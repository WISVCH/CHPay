package chpay.DatabaseHandler.transactiondb.entities;

import chpay.DatabaseHandler.Exceptions.InsufficientBalanceException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "users",
    indexes = {
      @Index(name = "idx_user_email", columnList = "email"),
      @Index(name = "idx_user_openid", columnList = "open_id"),
      @Index(name = "idx_user_rfid", columnList = "rfid")
    })
@Getter
@NoArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Setter
  @Column(nullable = false)
  private String name;

  @Setter
  @Column(nullable = false, unique = true, name = "open_id")
  private String openID;

  @Setter
  @Column(nullable = false, unique = true)
  private String email;

  @Setter
  @Column(nullable = true, unique = true)
  private String rfid;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal balance;

  @Column(nullable = false)
  @Setter
  private Boolean banned;

  public User(String name, String email, String openID) {
    this.name = name;
    this.email = email;
    this.openID = openID;
    this.balance = BigDecimal.ZERO;
    this.banned = false;
  }

  // This constructor is used for testing purposes and has unfortunately been grandfathered in.
  public User(String name, String email, String openID, BigDecimal balance) {
    this.name = name;
    this.email = email;
    this.openID = openID;
    this.balance = balance;
    this.banned = false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return id != null && id.equals(user.id);
  }

  /**
   * Adds the given amount to the user's balance.
   *
   * @param amount The amount to add to the balance.
   * @throws IllegalArgumentException If the amount is negative.
   */
  public void addBalance(BigDecimal amount) throws IllegalArgumentException {
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount cannot be negative");
    }
    this.balance = this.balance.add(amount);
  }

  /**
   * Subtracts the given amount from the user's balance.
   *
   * @param amount The amount to subtract from the balance.
   * @throws IllegalArgumentException If the amount is negative.
   * @throws InsufficientBalanceException If the user does not have enough money to make the
   *     transaction.
   */
  public void subtractBalance(BigDecimal amount)
      throws IllegalArgumentException, InsufficientBalanceException {
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount cannot be negative");
    }
    if (amount.compareTo(this.balance) > 0) {
      throw new InsufficientBalanceException("Insufficient balance");
    }
    this.balance = this.balance.subtract(amount);
  }
}
