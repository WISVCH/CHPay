package ch.wisv.chpay.api.external_payment.model;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Data
public class CHPaymentRequest {
  private BigDecimal amount;
  private String description;
  private String consumerName;
  private String consumerEmail;
  private String redirectURL;
  private String webhookURL;
  private String fallbackURL;
  private Map<String, Object> metadata;

  public CHPaymentRequest(
      BigDecimal amount,
      String description,
      String consumerName,
      String consumerEmail,
      String redirectURL,
      String webhookURL,
      String fallbackURL,
      Map<String, Object> metadata) {
    this.amount = amount;
    this.description = description;
    this.consumerName = consumerName;
    this.consumerEmail = consumerEmail;
    this.redirectURL = redirectURL;
    this.webhookURL = webhookURL;
    this.fallbackURL = fallbackURL;
    this.metadata = metadata;
  }
}
