package ch.wisv.chpay.admin.model;

import java.time.YearMonth;

public record PaymentRequestMonthlyStats(YearMonth yearMonth, long fulfilments) {

}
