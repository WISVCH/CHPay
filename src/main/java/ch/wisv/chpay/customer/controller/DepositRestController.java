package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.model.transaction.TopupTransaction;
import ch.wisv.chpay.core.service.DepositService;
import ch.wisv.chpay.core.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepositRestController {
  private final DepositService depositService;
  private final TransactionService transactionsService;

  public DepositRestController(
      DepositService depositService, TransactionService transactionsService) {
    this.depositService = depositService;
    this.transactionsService = transactionsService;
  }

  /**
   * This is where the mollie webhook goes
   *
   * @param mollieId the id of the transaction
   * @return a http status, this isn't relevant for the user, mollie gets it
   */
  @PostMapping("/balance/status")
  public ResponseEntity<HttpStatus> depositStatus(@RequestParam(name = "id") String mollieId) {
    if (transactionsService.getTransaction(mollieId).isEmpty())
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    TopupTransaction t = transactionsService.getTransaction(mollieId).get();
    depositService.validateTransaction(t.getId());
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
