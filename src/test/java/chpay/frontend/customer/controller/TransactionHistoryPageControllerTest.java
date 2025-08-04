package chpay.frontend.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.frontend.customer.service.BalanceHistoryService;
import chpay.frontend.shared.PaginationInfo;
import chpay.paymentbackend.service.MailService;
import chpay.paymentbackend.service.TransactionService;
import chpay.shared.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
public class TransactionHistoryPageControllerTest {

  private static final String MODEL_ATTR_LINKS = "links";
  private static final String MODEL_ATTR_PAGE = "page";
  private static final String MODEL_ATTR_SORTBY = "sortBy";
  private static final String MODEL_ATTR_ORDER = "order";
  private static final String MODEL_ATTR_TOPUPS_TAG = "onlyTopUps";
  private static final String MODEL_ATTR_TRANSACTIONS = "transactions";
  private static final String MODEL_ATTR_INFO = "info";

  @Mock private BalanceHistoryService balanceHistoryService;
  @Mock private TransactionService transactionService;
  @Mock private NotificationService notificationService;
  @Mock private MailService mailService;
  @Mock private Model model;
  @Mock private RedirectAttributes redirectAttributes;

  @InjectMocks private TransactionHistoryPageController controller;

  private User currentUser;
  private PaginationInfo paginationInfo;

  @BeforeEach
  void setUp() {
    currentUser = mock(User.class);
    Mockito.lenient().when(currentUser.getId()).thenReturn(UUID.randomUUID());
    Mockito.lenient().when(currentUser.getName()).thenReturn("testuser");

    paginationInfo = new PaginationInfo(1, 10, true, false, 2);

    Mockito.lenient().when(model.getAttribute("currentUser")).thenReturn(currentUser);
    Mockito.lenient()
        .when(
            balanceHistoryService.getTransactionPageInfo(
                any(User.class), eq(transactionService), anyLong(), anyLong(), anyBoolean()))
        .thenReturn(paginationInfo);
    try {
      Mockito.lenient()
          .when(
              balanceHistoryService.getTransactionsByUserAsJSON(
                  any(User.class),
                  eq(transactionService),
                  any(PaginationInfo.class),
                  anyString(),
                  anyString(),
                  anyBoolean()))
          .thenReturn(new ArrayList<>());
    } catch (JsonProcessingException e) {
    }
  }

  @Test
  void transactionViewTest() throws JsonProcessingException {
    String result =
        controller.getPage(model, "1", "timestamp", "desc", "false", redirectAttributes);

    assertEquals("transactions", result);
    verify(balanceHistoryService)
        .getTransactionPageInfo(currentUser, transactionService, 1, 4, false);
    verify(balanceHistoryService)
        .getTransactionsByUserAsJSON(
            currentUser, transactionService, paginationInfo, "timestamp", "desc", false);
    verify(model).addAttribute(MODEL_ATTR_LINKS, null);
    verify(model).addAttribute(MODEL_ATTR_PAGE, 1);
    verify(model).addAttribute(MODEL_ATTR_SORTBY, "timestamp");
    verify(model).addAttribute(MODEL_ATTR_ORDER, "desc");
    verify(model).addAttribute(MODEL_ATTR_TOPUPS_TAG, "false");
    verify(model).addAttribute(eq(MODEL_ATTR_TRANSACTIONS), any());
    verify(model).addAttribute(MODEL_ATTR_INFO, paginationInfo);
  }

  @Test
  void invalidParamTest() throws JsonProcessingException {
    String result =
        controller.getPage(model, "abc", "timestamp", "desc", "false", redirectAttributes);

    assertEquals("transactions", result);
    verify(balanceHistoryService)
        .getTransactionPageInfo(currentUser, transactionService, 1, 4, false);
  }

  @Test
  void invalidTimestampTest() throws JsonProcessingException {
    String result =
        controller.getPage(model, "1", "invalidSort", "desc", "false", redirectAttributes);

    assertEquals("transactions", result);
    verify(balanceHistoryService)
        .getTransactionsByUserAsJSON(
            currentUser, transactionService, paginationInfo, "timestamp", "desc", false);
  }

  @Test
  void invalidOrderTest() throws JsonProcessingException {
    String result =
        controller.getPage(model, "1", "timestamp", "invalidOrder", "false", redirectAttributes);

    assertEquals("transactions", result);
    verify(balanceHistoryService)
        .getTransactionsByUserAsJSON(
            currentUser, transactionService, paginationInfo, "timestamp", "desc", false);
  }

  @Test
  void onlyTopUpTest() throws JsonProcessingException {
    String result = controller.getPage(model, "1", "timestamp", "desc", "true", redirectAttributes);

    assertEquals("transactions", result);
    verify(balanceHistoryService)
        .getTransactionPageInfo(currentUser, transactionService, 1, 4, true);
    verify(balanceHistoryService)
        .getTransactionsByUserAsJSON(
            currentUser, transactionService, paginationInfo, "timestamp", "desc", true);
    verify(model).addAttribute(MODEL_ATTR_TOPUPS_TAG, "true");
  }

  @Test
  void emaiLTest() {
    String transactionId = "tx123";
    ResponseEntity<HttpStatus> response = controller.emailReceipt(transactionId);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(mailService).sendReceiptByEmail(transactionId);
  }

  @Test
  void emailErrorTest() throws Exception {
    String transactionId = "tx456";
    doThrow(new RuntimeException("Email sending failed"))
        .when(mailService)
        .sendReceiptByEmail(transactionId);

    ResponseEntity<HttpStatus> response = controller.emailReceipt(transactionId);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    verify(mailService).sendReceiptByEmail(transactionId);
  }
}
