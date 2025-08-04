package chpay.frontend.shared;

import chpay.DatabaseHandler.transactiondb.entities.transactions.RefundTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.Getter;

@Getter
public class TransactionJSON {
  private UUID id;

  private String userOpenID;

  private BigDecimal amount;

  private String description;

  private Transaction.TransactionStatus status;

  private String date;

  private UUID refundOfId;

  public static TransactionJSON TransactionToJSON(Transaction transaction) {
    TransactionJSON transactionJSON = new TransactionJSON();
    transactionJSON.id = transaction.getId();

    transactionJSON.userOpenID = transaction.getUser().getOpenID();

    transactionJSON.amount = transaction.getAmount();
    transactionJSON.description = transaction.getDescription();
    transactionJSON.status = transaction.getStatus();
    transactionJSON.date =
        transaction
            .getTimestamp()
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd LLLL yyyy"));

    if (transaction instanceof RefundTransaction) {
      RefundTransaction refundTransaction = (RefundTransaction) transaction;
      if (refundTransaction.getRefundOf() == null) {
        transactionJSON.refundOfId = null;
      } else {
        transactionJSON.refundOfId = refundTransaction.getRefundOf().getId();
      }
    } else {
      transactionJSON.refundOfId = null;
    }
    return transactionJSON;
  }
}
