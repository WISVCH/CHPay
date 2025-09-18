package ch.wisv.chpay.customer.controller;

import ch.wisv.chpay.core.controller.PageController;

abstract class CustomerController extends PageController {
  /** Model attr Page. */
  static final String MODEL_ATTR_PAGE = "page";
  
  /** Model attr Only Top Ups. */
  static final String MODEL_ATTR_TOPUPS_TAG = "onlyTopUps";
}
