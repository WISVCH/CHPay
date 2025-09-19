package ch.wisv.chpay.customer.service;

import be.woutschoovaerts.mollie.Client;
import be.woutschoovaerts.mollie.ClientBuilder;
import be.woutschoovaerts.mollie.data.common.AddressRequest;
import be.woutschoovaerts.mollie.data.common.Amount;
import be.woutschoovaerts.mollie.data.payment.PaymentRequest;
import be.woutschoovaerts.mollie.data.payment.PaymentResponse;
import be.woutschoovaerts.mollie.exception.MollieException;
import ch.wisv.chpay.core.aop.CheckSystemNotFrozen;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.TopupTransaction;
import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import ch.wisv.chpay.core.repository.UserRepository;
import ch.wisv.chpay.core.service.BalanceService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DepositService {

  private final BalanceService balanceService;
  private final UserRepository userRepository;
  private final Client mollieClient;

  @Value("${mollie.redirectUrl}")
  private String redirectUrl;

  @Value("${mollie.webhookUrl}")
  private String webhookUrl;

  @Value("${mollie.transactionFee}")
  private String fee;

  private final TransactionRepository transactionRepository;

  private final MailService mailService;

  private static final Logger logger = LoggerFactory.getLogger(DepositService.class);

  @Autowired
  public DepositService(
      BalanceService balanceService,
      UserRepository userRepository,
      @Value("${mollie.apiKey}") String apiKey,
      TransactionRepository transactionRepository,
      MailService mailService) {
    this.balanceService = balanceService;
    this.userRepository = userRepository;
    this.mollieClient = new ClientBuilder().withApiKey(apiKey).build();
    this.transactionRepository = transactionRepository;
    this.mailService = mailService;
  }

  public DepositService(
      BalanceService balanceService,
      UserRepository userRepository,
      Client mollie,
      TransactionRepository transactionRepository,
      MailService mailService) {
    this.balanceService = balanceService;
    this.userRepository = userRepository;
    this.mollieClient = mollie;
    this.transactionRepository = transactionRepository;
    this.mailService = mailService;
  }

  /**
   * Get the payment url
   *
   * @param transaction the transaction in question
   * @return
   */
  @CheckSystemNotFrozen
  @Transactional
  public String getMollieUrl(TopupTransaction transaction) {
    PaymentRequest pr = createPayment(transaction);
    try {
      PaymentResponse molliePayment = mollieClient.payments().createPayment(pr);
      updateTransaction(transaction, molliePayment);
      return molliePayment.getLinks().getCheckout().getHref();
    } catch (MollieException e) {
      handleMollieError(e);
      return null;
    }
  }

  /**
   * Update the status of the transaction
   *
   * @param transactionId the transaction's id
   * @return the validated transaction
   */
  @Transactional
  public TopupTransaction validateTransaction(UUID transactionId) {
    TopupTransaction transaction = transactionRepository.findByIdForUpdateTopup(transactionId);

    try {
      PaymentResponse pr = mollieClient.payments().getPayment(transaction.getMollieId());
      switch (pr.getStatus()) {
        case PENDING -> {
          transaction.setStatus(Transaction.TransactionStatus.PENDING);
        }
        case CANCELED, EXPIRED -> {
          balanceService.markTopUpAsFailed(transaction);
          mailService.sendDepositFailEmail(transaction, transaction.getAmount());
        }
        case PAID -> {
          transaction.setStatus(Transaction.TransactionStatus.SUCCESSFUL);
          balanceService.markTopUpAsPaid(transaction);
          mailService.sendDepositSuccessEmail(transaction, transaction.getAmount());
        }
      }
      return transactionRepository.saveAndFlush(transaction);
    } catch (MollieException e) {
      handleMollieError(e);
      return transaction;
    }
  }

  private void updateTransaction(TopupTransaction transaction, PaymentResponse molliePayment) {
    transaction.setMollieId(molliePayment.getId());
    transaction.setType(Transaction.TransactionType.TOP_UP);
    transaction.setStatus(Transaction.TransactionStatus.PENDING);
    transactionRepository.saveAndFlush(transaction);
  }

  /**
   * Create a payment request from which we get the url
   *
   * @param transaction the transaction
   * @return
   */
  @CheckSystemNotFrozen
  @Transactional
  public PaymentRequest createPayment(TopupTransaction transaction) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("orderId", transaction.getId().toString());

    String netId = transaction.getUser().getOpenID();
    BigDecimal amount = transaction.getAmount().add(BigDecimal.valueOf(Double.parseDouble(fee)));
    Optional<User> u = userRepository.findByOpenID(netId);
    if (u.isEmpty()) {
      throw new RuntimeException("User not found");
    }
    User u1 = u.get();
    String email = u1.getEmail();
    String redirectUrl = this.redirectUrl + transaction.getId().toString();
    String webhookUrl = this.webhookUrl;
    AddressRequest addressRequest = AddressRequest.builder().email(email).build();
    Amount toDeposit =
        Amount.builder().value(amount.setScale(2, RoundingMode.CEILING)).currency("EUR").build();
    return PaymentRequest.builder()
        .amount(toDeposit)
        .description("W.I.S.V. 'Christiaan Huygens'")
        .billingAddress(Optional.of(addressRequest))
        .redirectUrl(redirectUrl)
        .webhookUrl(Optional.of(webhookUrl))
        .metadata(metadata)
        .build();
  }

  /**
   * Handles potential problems with the payment
   *
   * @param mollieException The exception in question
   */
  private void handleMollieError(MollieException mollieException) {
    Map<String, Object> details = mollieException.getDetails();

    String title = toStringSafe(details.get("title"));
    String detail = toStringSafe(details.get("detail"));
    String field = toStringSafe(details.get("field"));

    if (!title.isEmpty() || !detail.isEmpty()) {
      logger.warn("Mollie error ({}): {} [field: {}]", title, detail, field);
    } else {
      logger.warn("Mollie error: {}", mollieException.getMessage(), mollieException);
    }
    throw new RuntimeException("Mollie error: " + detail);
  }

  private String toStringSafe(Object obj) {
    return obj instanceof String ? (String) obj : "";
  }
}
