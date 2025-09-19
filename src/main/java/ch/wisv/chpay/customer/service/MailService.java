package ch.wisv.chpay.customer.service;

import ch.wisv.chpay.core.model.transaction.Transaction;
import ch.wisv.chpay.core.repository.TransactionRepository;
import jakarta.mail.MessagingException; // Use jakarta.mail for Spring Boot 3+
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource; // For images from classpath
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

  private final JavaMailSender mailSender;
  private final TransactionRepository transactionRepository;

  @Value("${spring.mail.username}")
  private String sender;

  @Autowired
  public MailService(JavaMailSender mailSender, TransactionRepository transactionRepository) {
    this.mailSender = mailSender;
    this.transactionRepository = transactionRepository;
  }

  /**
   * An email for a successful deposit
   *
   * @param t the transaction
   * @param amount how much they deposited
   */
  public void sendDepositSuccessEmail(Transaction t, BigDecimal amount) {
    String to = t.getUser().getEmail();
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(sender);
      helper.setTo(to);
      helper.setSubject("[CHPay] Deposit Success");

      String htmlContent =
          "<html><body>"
              + "<p>Top-up with value of <strong>"
              + amount
              + "€</strong> successful!</p>"
              + "<p>Thank you for using CHPay!</p>"
              + "<p>---------------------------------------------------</p>"
              + "<img src='cid:chpayLogo'/>"
              + "</body></html>";
      helper.setText(htmlContent, true);

      ClassPathResource image = new ClassPathResource("static/images/ch-logo-small.png");
      helper.addInline("chpayLogo", image);

      mailSender.send(message);
    } catch (MailException | MessagingException e) {
      throw new MailSendException("Unable to send deposit success email", e.getCause());
    }
  }

  /**
   * Sends an email for a failed transaction
   *
   * @param t the transaction
   * @param amount the amount of money deposited
   */
  public void sendDepositFailEmail(Transaction t, BigDecimal amount) {
    String to = t.getUser().getEmail();
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(sender);
      helper.setTo(to);
      helper.setSubject("[CHPay] Deposit Error");

      String htmlContent =
          "<html><body>"
              + "<p>Top-up with value of <strong>"
              + amount
              + "€</strong> did not go through, please try again!</p>"
              + "<p>If the problem persists, please contact support.</p>"
              + "<p>---------------------------------------------------</p>"
              + "<img src='cid:chpayError'/>"
              + "</body></html>";
      helper.setText(htmlContent, true);

      ClassPathResource image = new ClassPathResource("static/images/ch-logo-small.png");
      helper.addInline("chpayError", image);

      mailSender.send(message);
    } catch (MailException | MessagingException e) {
      throw new MailSendException("Unable to send deposit failure email", e.getCause());
    }
  }

  /**
   * Sends an email with a receipt for the transaction
   *
   * @param id transaction's id
   */
  public void sendReceiptByEmail(String id) {
    Transaction t = transactionRepository.findById(UUID.fromString(id)).get();
    String to = t.getUser().getEmail();
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(sender);
      helper.setTo(to);
      helper.setSubject("[CHPay] Transaction Receipt");

      String htmlContent =
          String.format(
              """
            <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee;">
                        <h2>Transaction Receipt</h2>
                        <p><strong>Date:</strong> %s</p>
                        <p><strong>Description:</strong> %s</p>
                        <p><strong>Total:</strong> €%.2f</p>
                        <p><strong>Status:</strong> %s</p>
                        <p>---------------------------------------------------</p>
                        <img src='cid:chpayError'/>
                        <hr/>
                        <p style="font-size: 12px; color: #888;">Thank you for using CHPay.</p>
                    </div>
                </body>
            </html>
        """,
              t.getTimestamp(), t.getDescription(), t.getAmount(), t.getStatus());
      helper.setText(htmlContent, true);

      ClassPathResource image = new ClassPathResource("static/images/ch-logo-small.png");
      helper.addInline("chpayError", image);

      mailSender.send(message);
    } catch (MailException | MessagingException e) {
      throw new MailSendException("Unable to send deposit failure email", e.getCause());
    }
  }
}
