package ch.wisv.chpay.core.service;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.model.transaction.Transaction;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OfxExportService {

  private static final DateTimeFormatter OFX_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT).withZone(ZoneId.systemDefault());

  /**
   * Generate OFX content (OFX 1.x SGML) for a user's transactions.
   *
   * @param user owner of the transactions
   * @param transactions transactions to include
   * @return bytes of the OFX file (UTF-8)
   */
  public byte[] generateOfx(User user, List<Transaction> transactions) {
    StringBuilder sb = new StringBuilder();

    // SGML header (OFX 1.02) using UNICODE/UTF-8 to match produced bytes
    sb.append("OFXHEADER:100\n");
    sb.append("DATA:OFXSGML\n");
    sb.append("VERSION:102\n");
    sb.append("SECURITY:NONE\n");
    sb.append("ENCODING:UNICODE\n");
    sb.append("CHARSET:UTF-8\n");
    sb.append("COMPRESSION:NONE\n");
    sb.append("OLDFILEUID:NONE\n");
    sb.append("NEWFILEUID:").append(UUID.randomUUID()).append('\n');

    // Body
    sb.append("<OFX>\n");
    sb.append("  <SIGNONMSGSRSV1>\n");
    sb.append("    <SONRS>\n");
    sb.append("      <STATUS>\n");
    sb.append("        <CODE>0\n");
    sb.append("        <SEVERITY>INFO\n");
    sb.append("      </STATUS>\n");
    sb.append("      <DTSERVER>")
        .append(OFX_DATE_TIME.format(java.time.Instant.now()))
        .append('\n');
    sb.append("      <LANGUAGE>ENG\n");
    sb.append("      <FI>\n");
    sb.append("        <ORG>W.I.S.V. 'Christiaan Huygens'\n");
    sb.append("        <FID>CHPAY\n");
    sb.append("      </FI>\n");
    sb.append("    </SONRS>\n");
    sb.append("  </SIGNONMSGSRSV1>\n");

    // Use BANKMSGSRSV1/STMTRS as a generic statement container
    sb.append("  <BANKMSGSRSV1>\n");
    sb.append("    <STMTTRNRS>\n");
    sb.append("      <TRNUID>").append(UUID.randomUUID()).append('\n');
    sb.append("      <STATUS>\n");
    sb.append("        <CODE>0\n");
    sb.append("        <SEVERITY>INFO\n");
    sb.append("      </STATUS>\n");
    sb.append("      <STMTRS>\n");
    sb.append("        <CURDEF>EUR\n");
    sb.append("        <BANKACCTFROM>\n");
    sb.append("          <BANKID>CHPAY\n");
    sb.append("          <BRANCHID>CH\n");
    sb.append("          <ACCTID>")
        .append(user.getId() != null ? user.getId() : UUID.randomUUID())
        .append('\n');
    sb.append("          <ACCTTYPE>CHECKING\n");
    sb.append("        </BANKACCTFROM>\n");

    sb.append("        <BANKTRANLIST>\n");
    // Add DTSTART/DTEND based on min/max timestamps
    java.time.Instant minTs = null;
    java.time.Instant maxTs = null;
    for (Transaction tx : transactions) {
      java.time.Instant ts = tx.getTimestamp().atZone(ZoneId.systemDefault()).toInstant();
      if (minTs == null || ts.isBefore(minTs)) {
        minTs = ts;
      }
      if (maxTs == null || ts.isAfter(maxTs)) {
        maxTs = ts;
      }
    }
    if (minTs != null) {
      sb.append("          <DTSTART>").append(OFX_DATE_TIME.format(minTs)).append('\n');
    }
    if (maxTs != null) {
      sb.append("          <DTEND>").append(OFX_DATE_TIME.format(maxTs)).append('\n');
    }
    for (Transaction tx : transactions) {
      appendTransaction(sb, tx);
    }
    sb.append("        </BANKTRANLIST>\n");

    // Ledger balance is not tracked here; emit zero to keep format valid
    sb.append("        <LEDGERBAL>\n");
    sb.append("          <BALAMT>0.00\n");
    sb.append("          <DTASOF>")
        .append(OFX_DATE_TIME.format(java.time.Instant.now()))
        .append('\n');
    sb.append("        </LEDGERBAL>\n");

    sb.append("      </STMTRS>\n");
    sb.append("    </STMTTRNRS>\n");
    sb.append("  </BANKMSGSRSV1>\n");
    sb.append("</OFX>\n");

    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void appendTransaction(StringBuilder sb, Transaction tx) {
    sb.append("          <STMTTRN>\n");
    sb.append("            <TRNTYPE>").append(mapType(tx)).append('\n');
    sb.append("            <DTPOSTED>")
        .append(OFX_DATE_TIME.format(tx.getTimestamp().atZone(ZoneId.systemDefault())))
        .append('\n');
    sb.append("            <TRNAMT>").append(formatAmount(tx.getAmount())).append('\n');
    sb.append("            <FITID>").append(tx.getId()).append('\n');
    // Payee name: use Mollie for top-ups, W.I.S.V. for other transactions
    String payeeName =
        tx.getType() == Transaction.TransactionType.TOP_UP
            ? "Mollie"
            : "W.I.S.V. 'Christiaan Huygens'";
    sb.append("            <NAME>").append(payeeName).append('\n');
    // Use MEMO for the transaction description
    String memo = sanitize(tx.getDescription());
    if (memo != null && !memo.isEmpty()) {
      sb.append("            <MEMO>").append(memo).append('\n');
    }
    sb.append("          </STMTTRN>\n");
  }

  private static String mapType(Transaction tx) {
    // Map using sign and type, keeping TRNTYPE simple and import-friendly
    if (tx.getAmount() == null) {
      return "OTHER";
    }
    return tx.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? "CREDIT" : "DEBIT";
  }

  private static String formatAmount(BigDecimal amount) {
    if (amount == null) {
      return "0.00";
    }
    return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private static String sanitize(String value) {
    if (value == null) {
      return "";
    }
    // OFX SGML is permissive, but avoid newlines and angle brackets
    return value.replace('\n', ' ').replace('<', '(').replace('>', ')');
  }
}
