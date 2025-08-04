package chpay.frontend.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.TopupTransaction;
import chpay.paymentbackend.service.DepositService;
import chpay.paymentbackend.service.TransactionService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class DepositRestControllerTest {

  @Mock private DepositService depositService;
  @Mock private TransactionService transactionsService;

  @InjectMocks private DepositRestController depositRestController;

  private String testMollieId;
  private TopupTransaction testTopupTransaction;
  private UUID id = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    User u = mock(User.class);
    BigDecimal amount = BigDecimal.TEN;
    testMollieId = "tr_test_mollie_id";
    testTopupTransaction = mock(TopupTransaction.class);
    Mockito.lenient().when(testTopupTransaction.getId()).thenReturn(id);
    Mockito.lenient().when(testTopupTransaction.getMollieId()).thenReturn(testMollieId);
  }

  @Test
  void notFoundTest() {
    when(transactionsService.getTransaction(testMollieId)).thenReturn(Optional.empty());
    ResponseEntity<HttpStatus> response = depositRestController.depositStatus(testMollieId);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    verify(transactionsService).getTransaction(testMollieId);
    verify(depositService, never()).validateTransaction(id);
  }

  @Test
  void okTest() {
    when(transactionsService.getTransaction(testMollieId))
        .thenReturn(Optional.of(testTopupTransaction));

    ResponseEntity<HttpStatus> response = depositRestController.depositStatus(testMollieId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(transactionsService, times(2)).getTransaction(testMollieId);
    verify(depositService).validateTransaction(id);
  }
}
