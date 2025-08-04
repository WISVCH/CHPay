package chpay.frontend.shared;

import static org.junit.jupiter.api.Assertions.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.PaymentTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionJSONConverterTest {

  private List<Transaction> testTransactions;
  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User("Test User", "test@example.com", "password", new BigDecimal("100.00"));
    setUserId(testUser);

    testTransactions = new ArrayList<>();
    // Create test transactions
    testTransactions.add(createTestTransaction("-50.00", "Test payment 1"));
    testTransactions.add(createTestTransaction("-25.00", "Test payment 2"));
  }

  @Test
  void transactionsToJSON_ValidTransactions_ReturnsCorrectJSON() throws JsonProcessingException {
    List<String> result = TransactionJSONConverter.TransactionsToJSON(testTransactions);

    assertEquals(2, result.size());
    for (String json : result) {
      assertTrue(json.contains("\"userOpenID\":\"" + testUser.getOpenID() + "\""));
      assertTrue(json.contains("\"description\":\"Test payment"));
      assertTrue(
          json.contains("\"status\":\"" + testTransactions.get(0).getStatus().toString() + "\""));
    }
  }

  @Test
  void transactionsToJSON_EmptyList_ReturnsEmptyList() throws JsonProcessingException {
    List<String> result = TransactionJSONConverter.TransactionsToJSON(new ArrayList<>());
    assertTrue(result.isEmpty());
  }

  @Test
  void transactionsToJSON_NullList_ThrowsException() {
    assertThrows(
        RuntimeException.class,
        () -> {
          TransactionJSONConverter.TransactionsToJSON(null);
        });
  }

  private Transaction createTestTransaction(String amount, String description) {
    Transaction transaction =
        PaymentTransaction.createPaymentTransaction(
            testUser, new BigDecimal(amount), description, null);
    setTransactionFields(transaction);
    return transaction;
  }

  private void setUserId(User user) {
    try {
      java.lang.reflect.Field idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, UUID.randomUUID());
    } catch (Exception e) {
      fail("Failed to set User ID: " + e.getMessage());
    }
  }

  private void setTransactionFields(Transaction transaction) {
    try {
      java.lang.reflect.Field idField = Transaction.class.getDeclaredField("id");
      java.lang.reflect.Field timestampField = Transaction.class.getDeclaredField("timestamp");
      idField.setAccessible(true);
      timestampField.setAccessible(true);
      idField.set(transaction, UUID.randomUUID());
      timestampField.set(transaction, LocalDateTime.now());
    } catch (Exception e) {
      fail("Failed to set Transaction fields: " + e.getMessage());
    }
  }
}
