package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransactionExpirationServiceTest {

  @Autowired private TransactionExpirationService transactionService;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private UserRepository userRepository;

  @Test
  void expireOldTransactions() throws NoSuchFieldException, IllegalAccessException {
    User user =
        userRepository.save(new User("Test User", "<EMAIL>", "test123", new BigDecimal("100.00")));
    TopupTransaction tx =
        TopupTransaction.createTopUpTransaction(user, BigDecimal.valueOf(10), "test");
    tx.setStatus(Transaction.TransactionStatus.PENDING);

    Field timestampField = Transaction.class.getDeclaredField("timestamp");
    timestampField.setAccessible(true);
    timestampField.set(tx, LocalDateTime.now().minusMinutes(30));

    transactionRepository.save(tx);

    transactionService.updateTransactionStatuses();

    Transaction updated = transactionRepository.findById(tx.getId()).orElseThrow();
    assertEquals(Transaction.TransactionStatus.FAILED, updated.getStatus());
  }

  @Test
  void leaveNewTransactionsPending() throws NoSuchFieldException, IllegalAccessException {
    User user =
        userRepository.save(
            new User("Test User", "<secondEmail>", "test2", new BigDecimal("100.00")));
    TopupTransaction tx =
        TopupTransaction.createTopUpTransaction(user, BigDecimal.valueOf(10), "test");
    tx.setStatus(Transaction.TransactionStatus.PENDING);

    Field timestampField = Transaction.class.getDeclaredField("timestamp");
    timestampField.setAccessible(true);
    timestampField.set(tx, LocalDateTime.now().minusMinutes(15));

    transactionRepository.save(tx);

    transactionService.updateTransactionStatuses();

    Transaction updated = transactionRepository.findById(tx.getId()).orElseThrow();
    assertEquals(Transaction.TransactionStatus.PENDING, updated.getStatus());
  }
}
