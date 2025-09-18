package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.core.controller.PageController;

abstract class AdminController extends PageController {
  /** Model attr Url Page. */
  static final String MODEL_ATTR_USERS = "users";
  static final String MODEL_ATTR_STATUS = "status";
  static final String MODEL_ATTR_BALANCE = "balanceAvailable";
  static final String MODEL_ATTR_INCOMING = "incomingFunds";
  static final String MODEL_ATTR_OUTGOING = "outgoingFunds";
  static final String MODEL_ATTR_MAIN_STAT = "mainStat";
  static final String MODEL_ATTR_SECOND_STAT = "secondStat";
  static final String MODEL_ATTR_STATS = "stats";
  static final String MODEL_ATTR_TYPE = "type";
  static final String MODEL_ATTR_REFUND_ID = "refundId";
  static final String MODEL_ATTR_REQUEST_ID = "requestId";
  static final String MODEL_ATTR_REFUND_POSSIBLE = "refundPossible";
  static final String MODEL_ATTR_PAGE_FILTERS = "pageFilters";
  static final String MODEL_ATTR_SORTING_PARAMS = "sortingParams";
  static final String MODEL_ATTR_USER = "user";
  static final String MODEL_ATTR_ERROR_LOG = "errorLog";
}
