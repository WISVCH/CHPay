package chpay.frontend.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.RefundTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TransactionJSONTest {

  User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("Test User", "test@example.com", "test123", new BigDecimal("100.00"));
  }

  @Test
  void TransactionToJSONTest() {
    PaymentTransaction testTransaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment", null);
    PaymentTransaction testTransactionSpy = Mockito.spy(testTransaction);
    when(testTransactionSpy.getTimestamp()).thenReturn(LocalDateTime.now());
    TransactionJSON transactionJSON = TransactionJSON.TransactionToJSON(testTransactionSpy);
    assertEquals(testTransaction.getId(), transactionJSON.getId());
    assertEquals(testTransaction.getAmount(), transactionJSON.getAmount());
    assertEquals(testTransaction.getDescription(), transactionJSON.getDescription());
    assertEquals(testTransaction.getUser().getOpenID(), transactionJSON.getUserOpenID());
    assertEquals(testTransaction.getStatus(), transactionJSON.getStatus());
    assertEquals(null, transactionJSON.getRefundOfId());
    // assertEquals(testTransaction.getTimestamp().toLocalDate(), transactionJSON.getDate());
  }

  @Test
  void TransactionToJSONTestRefund() {
    PaymentTransaction testTransaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal("-50.00"), "Test payment", null);

    RefundTransaction testRefund =
        RefundTransaction.createRefund(testUser, new BigDecimal("50.00"), testTransaction);
    PaymentTransaction testTransactionSpy = Mockito.spy(testTransaction);
    when(testTransactionSpy.getTimestamp()).thenReturn(LocalDateTime.now());
    RefundTransaction testRefundSpy = Mockito.spy(testRefund);
    when(testRefundSpy.getTimestamp()).thenReturn(LocalDateTime.now());
    TransactionJSON transactionJSON = TransactionJSON.TransactionToJSON(testRefundSpy);
    assertEquals(testRefund.getId(), transactionJSON.getId());
    assertEquals(testRefund.getAmount(), transactionJSON.getAmount());
    assertEquals(testRefund.getDescription(), transactionJSON.getDescription());
    assertEquals(testRefund.getUser().getOpenID(), transactionJSON.getUserOpenID());
    assertEquals(testRefund.getStatus(), transactionJSON.getStatus());
    assertEquals(testRefund.getRefundOf().getId(), transactionJSON.getRefundOfId());
  }
}
