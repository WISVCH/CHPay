package ch.wisv.chpay.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a user's balance at a specific point in time.
 * Used for tracking balance changes over time in admin views.
 */
@Getter
@AllArgsConstructor
@ToString
public class BalanceEntry {
  
  /** The timestamp when this balance was recorded */
  private final LocalDateTime timestamp;
  
  /** The balance amount at this timestamp */
  private final BigDecimal balance;
  
  /** The transaction that caused this balance change */
  private final String transactionId;
  
  /** The transaction description */
  private final String description;
  
  /** The transaction type */
  private final String transactionType;
}
