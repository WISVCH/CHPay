package chpay.shared;

import java.util.Map;

public class NotificationPayload {

  public static Map<String, String> success(String message) {
    return Map.of("type", "success", "message", message);
  }

  public static Map<String, String> error(String message) {
    return Map.of("type", "error", "message", message);
  }

  public static Map<String, String> info(String message) {
    return Map.of("type", "message", "message", message);
  }

  // Optional: extend this for more structured data in the future
}
