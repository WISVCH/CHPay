package chpay.frontend.shared;

import chpay.DatabaseHandler.transactiondb.entities.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public class UserJSONConverter {
  /***
   * Convert users from Java objects to json to be used on the frontend
   * @param users transactions to be converted
   * @return list of json objects of transactions
   */
  public static List<String> UsersToJSON(List<User> users) throws JsonProcessingException {
    List<String> userStrings = new ArrayList<>();
    if (users == null) {
      return userStrings;
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      for (User user : users) {
        if (user != null) {
          userStrings.add(mapper.writeValueAsString(user));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return userStrings;
  }
}
