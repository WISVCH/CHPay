package chpay.frontend.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestApiControllerTest {

  @Mock private TransactionRepository transactionRepository;

  @InjectMocks private RequestApiController controller;

  private final UUID requestId = UUID.randomUUID();
  private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  /** Tests good weather behavior with two transactions for the same payment request. */
  @Test
  void returnsTwoTransactionsCorrectly() {

    // Transaction #1
    Transaction tx1 = Mockito.mock(Transaction.class);
    when(tx1.getId()).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    User user1 = Mockito.mock(User.class);
    when(user1.getName()).thenReturn("Alice Doe");
    when(tx1.getUser()).thenReturn(user1);
    when(tx1.getAmount()).thenReturn(new BigDecimal("-17.50"));
    LocalDateTime ts1 = LocalDateTime.of(2025, 6, 4, 12, 7, 15);
    when(tx1.getTimestamp()).thenReturn(ts1);

    // Transaction #2
    Transaction tx2 = Mockito.mock(Transaction.class);
    when(tx2.getId()).thenReturn(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    User user2 = Mockito.mock(User.class);
    when(user2.getName()).thenReturn("Bob Smith");
    when(tx2.getUser()).thenReturn(user2);
    when(tx2.getAmount()).thenReturn(new BigDecimal("-5.00"));
    LocalDateTime ts2 = LocalDateTime.of(2025, 6, 4, 12, 10, 0);
    when(tx2.getTimestamp()).thenReturn(ts2);

    // Stub the repository to return [tx1, tx2]
    when(transactionRepository.findAllSuccessfulForRequest(
            ArgumentMatchers.eq(requestId),
            ArgumentMatchers.eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(List.of(tx1, tx2));

    List<Map<String, String>> result = controller.getSuccessfulPayments(requestId);

    assertThat(result).hasSize(2);

    Map<String, String> map1 = result.get(0);
    assertThat(map1.get("transactionId")).isEqualTo("11111111-1111-1111-1111-111111111111");
    assertThat(map1.get("payerName")).isEqualTo("Alice Doe");
    assertThat(map1.get("amount")).isEqualTo("17.50");
    assertThat(map1.get("timestamp")).isEqualTo("2025-06-04T12:07:15");

    Map<String, String> map2 = result.get(1);
    assertThat(map2.get("transactionId")).isEqualTo("22222222-2222-2222-2222-222222222222");
    assertThat(map2.get("payerName")).isEqualTo("Bob Smith");
    assertThat(map2.get("amount")).isEqualTo("5.00");
    assertThat(map2.get("timestamp")).isEqualTo("2025-06-04T12:10:00");
  }

  /** Tests that the controller returns an empty json when there are no transactions matching. */
  @Test
  void returnsEmptyList() {
    // Stub repository to return an empty list for this requestId
    when(transactionRepository.findAllSuccessfulForRequest(
            ArgumentMatchers.eq(requestId),
            ArgumentMatchers.eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(List.of());

    List<Map<String, String>> result = controller.getSuccessfulPayments(requestId);

    assertThat(result).isEmpty();
  }

  /** Tests that the API provides only the matching transactions' information. */
  @Test
  void paymentsForOtherRequestAreIgnored() {
    // Prepare two request IDs:
    UUID req1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    UUID req2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    // Transaction for request #1
    Transaction txForReq1 = Mockito.mock(Transaction.class);
    when(txForReq1.getId()).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    User userA = Mockito.mock(User.class);
    when(userA.getName()).thenReturn("Alice A");
    when(txForReq1.getUser()).thenReturn(userA);
    when(txForReq1.getAmount()).thenReturn(new BigDecimal("-10.00"));
    LocalDateTime timeA = LocalDateTime.of(2025, 6, 4, 14, 0, 0);
    when(txForReq1.getTimestamp()).thenReturn(timeA);

    // Transaction for request #2
    Transaction txForReq2 = Mockito.mock(Transaction.class);
    when(txForReq2.getId()).thenReturn(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    User userB = Mockito.mock(User.class);
    when(userB.getName()).thenReturn("Bob B");
    when(txForReq2.getUser()).thenReturn(userB);
    when(txForReq2.getAmount()).thenReturn(new BigDecimal("-5.00"));
    LocalDateTime timeB = LocalDateTime.of(2025, 6, 4, 15, 0, 0);
    when(txForReq2.getTimestamp()).thenReturn(timeB);

    // Stub repository:
    when(transactionRepository.findAllSuccessfulForRequest(
            ArgumentMatchers.eq(req1),
            ArgumentMatchers.eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(List.of(txForReq1));
    when(transactionRepository.findAllSuccessfulForRequest(
            ArgumentMatchers.eq(req2),
            ArgumentMatchers.eq(Transaction.TransactionStatus.SUCCESSFUL)))
        .thenReturn(List.of(txForReq2));

    // for request 1
    List<Map<String, String>> resultForReq1 = controller.getSuccessfulPayments(req1);

    assertThat(resultForReq1).hasSize(1);
    Map<String, String> map1 = resultForReq1.get(0);
    assertThat(map1.get("transactionId")).isEqualTo("11111111-1111-1111-1111-111111111111");
    assertThat(map1.get("payerName")).isEqualTo("Alice A");
    assertThat(map1.get("amount")).isEqualTo("10.00");
    assertThat(map1.get("timestamp")).isEqualTo("2025-06-04T14:00:00");

    List<Map<String, String>> resultForReq2 = controller.getSuccessfulPayments(req2);

    // for request 2
    assertThat(resultForReq2).hasSize(1);
    Map<String, String> map2 = resultForReq2.get(0);
    assertThat(map2.get("transactionId")).isEqualTo("22222222-2222-2222-2222-222222222222");
    assertThat(map2.get("payerName")).isEqualTo("Bob B");
    assertThat(map2.get("amount")).isEqualTo("5.00");
    assertThat(map2.get("timestamp")).isEqualTo("2025-06-04T15:00:00");
  }
}
