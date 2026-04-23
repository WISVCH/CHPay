package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.controller.PageController;
import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import org.springframework.security.access.AccessDeniedException;

abstract class CustomerController extends PageController {

  protected void assertCurrentUserOwnsTransaction(User currentUser, Transaction transaction) {
    if (currentUser == null
        || transaction.getUser() == null
        || !transaction.getUser().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("You are not allowed to access this transaction");
    }
  }
}
