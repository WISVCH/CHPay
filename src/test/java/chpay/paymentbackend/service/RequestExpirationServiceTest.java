package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.repositories.RequestRepository;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RequestExpirationServiceTest {

  @Autowired private RequestExpirationService requestExpirationService;
  @Autowired private UserRepository userRepository;
  @Autowired private RequestRepository requestRepository;

  @Test
  void expireOldTransactions() throws NoSuchFieldException, IllegalAccessException {
    PaymentRequest pr = new PaymentRequest(new BigDecimal("50.00"), "Test request", true);

    Field timestampField = PaymentRequest.class.getDeclaredField("createdAt");
    timestampField.setAccessible(true);
    timestampField.set(pr, LocalDateTime.now().minusMonths(2));

    requestRepository.save(pr);

    requestExpirationService.expireOldRequests();

    assertDoesNotThrow(() -> requestRepository.findById(pr.getRequest_id()).get());
    PaymentRequest updated = requestRepository.findById(pr.getRequest_id()).orElseThrow();
    assertTrue(updated.isFulfilled());
  }

  @Test
  void leaveNewTransactionsPending() throws NoSuchFieldException, IllegalAccessException {
    PaymentRequest pr = new PaymentRequest(new BigDecimal("50.00"), "Test request", true);

    Field timestampField = PaymentRequest.class.getDeclaredField("createdAt");
    timestampField.setAccessible(true);
    timestampField.set(pr, LocalDateTime.now().minusDays(15));

    requestRepository.save(pr);

    requestExpirationService.expireOldRequests();

    assertDoesNotThrow(() -> requestRepository.findById(pr.getRequest_id()).get());
    PaymentRequest updated = requestRepository.findById(pr.getRequest_id()).orElseThrow();
    assertFalse(updated.isFulfilled());
  }
}
