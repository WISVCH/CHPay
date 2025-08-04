package chpay.frontend.shared;

import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.List;

public class TransactionJSONConverter {
  /***
   * Convert transactions from Java objects to json to be used on the frontend
   * @param transactions transactions to be converted
   * @return list of json objects of transactions
   */
  public static List<String> TransactionsToJSON(List<Transaction> transactions)
      throws JsonProcessingException {
    List<String> transactionStrings = new ArrayList<>();
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      for (Transaction transaction : transactions) {
        transactionStrings.add(
            mapper.writeValueAsString(TransactionJSON.TransactionToJSON(transaction)));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return transactionStrings;
  }
}
