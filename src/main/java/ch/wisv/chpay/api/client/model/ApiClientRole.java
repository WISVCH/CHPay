package ch.wisv.chpay.api.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ApiClientRole {
  EXTERNAL_PAYMENT("external_payment"),
  LEDSTRIP("ledstrip");

  private final String scope;

  ApiClientRole(String scope) {
    this.scope = scope;
  }

  @JsonValue
  public String scope() {
    return scope;
  }

  public String authority() {
    return "SCOPE_" + scope;
  }

  @JsonCreator
  public static ApiClientRole fromValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Api client role cannot be blank");
    }
    return Arrays.stream(values())
        .filter(role -> role.scope.equalsIgnoreCase(value) || role.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown api client role: " + value));
  }
}
