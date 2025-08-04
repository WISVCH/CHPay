package chpay.paymentbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.entities.transactions.Transaction;
import chpay.DatabaseHandler.transactiondb.repositories.TransactionRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

  @Mock private JavaMailSender mailSender;

  @InjectMocks private MailService mailService;
  @Mock private TransactionRepository transactionRepository;
  private User testUser;
  private Transaction testTransaction;
  private BigDecimal testAmount;
  private String senderEmail = "test@chpay.com";

  @BeforeEach
  void setUp() {
    Mockito.lenient().when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

    ReflectionTestUtils.setField(mailService, "sender", senderEmail);
    testUser = new User();
    testUser.setEmail("recipient@example.com");
    testUser.setName("John");
    testTransaction = Mockito.mock(Transaction.class);
    Mockito.lenient().when(testTransaction.getUser()).thenReturn(testUser);
    UUID transactionId = UUID.fromString("7be0986d-7ee4-4b3d-96f3-c85010f866d5");
    Mockito.lenient().when(testTransaction.getId()).thenReturn(transactionId);
    testAmount = new BigDecimal("100.00");
    Mockito.lenient()
        .when(transactionRepository.findById(transactionId))
        .thenReturn(Optional.of(testTransaction));
  }

  @Test
  void sendDepositSuccessEmail_sendsCorrectHtmlEmailWithImage() throws Exception {
    MimeMessage mockMimeMessage = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);

    mailService.sendDepositSuccessEmail(testTransaction, testAmount);

    verify(mailSender, times(1)).send(mockMimeMessage);

    verify(mockMimeMessage).setFrom(any(InternetAddress.class));
    verify(mockMimeMessage).setSubject(eq("[CHPay] Deposit Success"), eq("UTF-8"));

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockMimeMessage).setSubject(subjectCaptor.capture(), eq("UTF-8"));
    assertEquals("[CHPay] Deposit Success", subjectCaptor.getValue());
  }

  @Test
  void sendDepositSuccessEmail_throwsMailSendException_onMailException() {
    doThrow(new org.springframework.mail.MailParseException("Simulated mail error"))
        .when(mailSender)
        .send(any(MimeMessage.class));
    when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

    MailException thrown =
        assertThrows(
            MailException.class,
            () -> {
              mailService.sendDepositSuccessEmail(testTransaction, testAmount);
            });
    assertTrue(thrown.getMessage().contains("Unable to send deposit success email"));
  }

  @Test
  void sendDepositFailEmail_sendsCorrectHtmlEmailWithImage() throws Exception {
    MimeMessage mockMimeMessage = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);

    mailService.sendDepositFailEmail(testTransaction, testAmount);

    verify(mailSender, times(1)).send(mockMimeMessage);

    verify(mockMimeMessage).setFrom(any(InternetAddress.class));
    verify(mockMimeMessage).setSubject(eq("[CHPay] Deposit Error"), eq("UTF-8"));

    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockMimeMessage).setSubject(subjectCaptor.capture(), eq("UTF-8"));
    assertEquals("[CHPay] Deposit Error", subjectCaptor.getValue());
  }

  @Test
  void sendDepositFailEmail_throwsMailSendException_onMailException() {
    doThrow(new org.springframework.mail.MailParseException("Simulated mail error"))
        .when(mailSender)
        .send(any(MimeMessage.class));
    when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

    MailException thrown =
        assertThrows(
            MailException.class,
            () -> {
              mailService.sendDepositFailEmail(testTransaction, testAmount);
            });
    assertTrue(thrown.getMessage().contains("Unable to send deposit failure email"));
  }

  @Test
  void sendReceiptByEmail_throwsExceptionWhenSendingFails() {
    String transactionId = testTransaction.getId().toString();
    doThrow(new MailSendException("Simulated failure"))
        .when(mailSender)
        .send(any(MimeMessage.class));

    assertThrows(
        MailSendException.class,
        () -> {
          mailService.sendReceiptByEmail(transactionId);
        });
  }

  @Test
  void sendReceiptByEmail_throwsExceptionWhenTransactionNotFound() {
    String badId = UUID.randomUUID().toString();
    when(transactionRepository.findById(UUID.fromString(badId))).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () -> {
          mailService.sendReceiptByEmail(badId);
        });
  }
}
