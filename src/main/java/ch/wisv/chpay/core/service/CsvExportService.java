package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.model.transaction.Transaction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CsvExportService {

  /**
   * Generate CSV for transactions with header row (semicolon separated).
   *
   * <p>Columns: Id;Type;Name;Description;Amount;Status;Timestamp
   */
  public byte[] generateCsv(List<Transaction> transactions) {
    String csvData =
        transactions.stream()
            .map(
                t ->
                    t.getId().toString()
                        + ";"
                        + t.getType().toString()
                        + ";"
                        + (t.getUser() != null ? t.getUser().getName() : "")
                        + ";"
                        + sanitize(t.getDescription())
                        + ";"
                        + t.getAmount().toPlainString()
                        + ";"
                        + t.getStatus().name()
                        + ";"
                        + t.getTimestamp().toString())
            .collect(Collectors.joining("\n"));

    csvData = "Id;Type;Name;Description;Amount;Status;Timestamp\n" + csvData;
    return csvData.getBytes(StandardCharsets.UTF_8);
  }

  private static String sanitize(String value) {
    if (value == null) {
      return "";
    }
    // Avoid newlines and semicolons that would break CSV; replace with spaces
    return value.replace('\n', ' ').replace(';', ',');
  }
}
