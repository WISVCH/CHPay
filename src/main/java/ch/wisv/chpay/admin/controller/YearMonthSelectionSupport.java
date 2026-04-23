package ch.wisv.chpay.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import java.util.function.Supplier;

final class YearMonthSelectionSupport {

  private YearMonthSelectionSupport() {}

  static YearMonth resolveYearMonthOrRedirect(
      String yearMonth,
      HttpServletRequest request,
      Supplier<YearMonth> getMostRecentYearMonth,
      Function<YearMonth, String> buildRedirectUrl) {
    if (yearMonth == null || yearMonth.trim().isEmpty()) {
      throw new RedirectException(buildRedirectUrlWithPreservedParams(request, getMostRecentYearMonth, buildRedirectUrl));
    }

    try {
      return YearMonth.parse(yearMonth);
    } catch (DateTimeParseException e) {
      throw new RedirectException(buildRedirectUrlWithPreservedParams(request, getMostRecentYearMonth, buildRedirectUrl));
    }
  }

  private static String buildRedirectUrlWithPreservedParams(
      HttpServletRequest request,
      Supplier<YearMonth> getMostRecentYearMonth,
      Function<YearMonth, String> buildRedirectUrl) {
    YearMonth selectedYearMonth = getMostRecentYearMonth.get();
    String queryString = request.getQueryString();
    String preservedParams = "";
    if (queryString != null && !queryString.isEmpty()) {
      preservedParams = queryString.replaceAll("(&?)yearMonth=[^&]*(&?)", "").replaceAll("^&|&$", "");
    }
    return buildRedirectUrl.apply(selectedYearMonth)
        + (preservedParams.isEmpty() ? "" : "&" + preservedParams);
  }

  static class RedirectException extends RuntimeException {
    private final String redirectUrl;

    RedirectException(String redirectUrl) {
      super("Redirect to: " + redirectUrl);
      this.redirectUrl = redirectUrl;
    }

    String getRedirectUrl() {
      return redirectUrl;
    }
  }
}
