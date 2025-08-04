package chpay.paymentbackend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class Dienst2ApiResponse {
  private int count;
  private String next;
  private String previous;

  @JsonProperty("results")
  private List<UserData> results;

  @Data
  public static class UserData {
    private String firstname;
    private String preposition;
    private String surname;
    private String email;
    private String netid;
  }
}
