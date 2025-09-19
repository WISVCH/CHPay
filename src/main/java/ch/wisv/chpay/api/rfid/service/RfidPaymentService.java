package ch.wisv.chpay.api.rfid.service;

import ch.wisv.chpay.core.exception.InsufficientBalanceException;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.PaymentTransaction;
import ch.wisv.chpay.core.service.RequestService;
import ch.wisv.chpay.core.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class RfidPaymentService {
  private final RequestService requestService;
  private final TransactionService txnService;

  public RfidPaymentService(RequestService requestService, TransactionService txnService) {
    this.requestService = requestService;
    this.txnService = txnService;
  }

  /**
   * Orchestrates the payment: 1) ensure user.balance ≥ request.amount 2) get-or-create pending TX
   * 3) fulfill it
   *
   * @param user to make the purchase.
   * @param requestId to be paid
   * @return User name
   */
  @Transactional
  public String payFromRequest(User user, UUID requestId) {
    // 1) Load the PaymentRequest (without locking)
    PaymentRequest pr =
        requestService
            .getRequestById(requestId)
            .orElseThrow(() -> new NoSuchElementException("No payment request " + requestId));

    // 2) Pre‐check balance
    if (user.getBalance().compareTo(pr.getAmount()) < 0) {
      throw new InsufficientBalanceException(
          "Insufficient balance: need " + pr.getAmount() + " but have " + user.getBalance());
    }

    // 3) Create or fetch the pending TX
    PaymentTransaction pending = requestService.transactionFromRequest(requestId, user);

    // 4) Fulfill it
    txnService.fullfillTransaction(pending.getId(), user);

    return user.getName();
  }
}
