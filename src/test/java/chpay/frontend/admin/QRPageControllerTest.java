package chpay.frontend.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.DatabaseHandler.transactiondb.repositories.RequestRepository;
import chpay.shared.GlobalExceptionHandler;
import chpay.shared.service.NotificationService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;

@ExtendWith(MockitoExtension.class)
class QRPageControllerTest {

  @Mock private RequestRepository requestRepository;

  @Mock private NotificationService notificationService;

  private MockMvc mockMvc;

  @InjectMocks private QRPageController controller;

  /** Initialize MockMvc with a view resolver for 'qr' view and the GlobalExceptionHandler. */
  @BeforeEach
  void setup() {
    ViewResolver resolver =
        (viewName, locale) -> {
          if ("qr".equals(viewName)) {
            return new InternalResourceView("/WEB-INF/views/qr.html");
          }
          return null;
        };

    // Create GlobalExceptionHandler with mocked NotificationService
    GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    try {
      java.lang.reflect.Field field = QRPageController.class.getDeclaredField("baseUrl");
      field.setAccessible(true);
      field.set(controller, "http://localhost:8080");
    } catch (Exception e) {
      throw new RuntimeException("Failed to set baseUrl in QRPageController", e);
    }

    // Use reflection to set the notificationService field
    try {
      java.lang.reflect.Field field =
          GlobalExceptionHandler.class.getDeclaredField("notificationService");
      field.setAccessible(true);
      field.set(exceptionHandler, notificationService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set notificationService in GlobalExceptionHandler", e);
    }

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setViewResolvers(resolver)
            .setControllerAdvice(exceptionHandler) // Add the GlobalExceptionHandler
            .build();
  }

  /**
   * Tests the good weather case - a valid payment request ID returns the qr view with correct model
   * attributes.
   */
  @Test
  void showQR_valid() throws Exception {
    // Arrange
    String id = UUID.randomUUID().toString();
    PaymentRequest pr = new PaymentRequest();
    String dummyQr = "data:image/png;base64,xxx";

    when(requestRepository.findById(UUID.fromString(id))).thenReturn(Optional.of(pr));

    try (MockedStatic<QRCodeUtil> mock = mockStatic(QRCodeUtil.class)) {
      mock.when(
              () ->
                  QRCodeUtil.generateQRCodeBase64(
                      eq("http://localhost:8080/payment/" + id), eq(250), eq(250)))
          .thenReturn(dummyQr);

      mockMvc
          .perform(get("/qr/" + id))
          .andExpect(view().name("qr"))
          .andExpect(model().attribute("qrCodeBase64", dummyQr))
          .andExpect(model().attribute("paymentRequest", pr))
          .andExpect(model().attribute("paymentRequestId", id));

      verify(requestRepository).findById(UUID.fromString(id));
      mock.verify(
          () ->
              QRCodeUtil.generateQRCodeBase64(
                  eq("http://localhost:8080/payment/" + id), eq(250), eq(250)),
          times(1));
    }
  }

  /**
   * Tests the NoSuchElementException case - a non-existent payment request ID results in a redirect
   * to the index page with an error message.
   */
  @Test
  void showQR_notFound() throws Exception {

    String id = UUID.randomUUID().toString();

    when(requestRepository.findById(UUID.fromString(id))).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/qr/" + id))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/index")); // Expect a redirect to "/index"

    verify(requestRepository).findById(UUID.fromString(id));
    verify(notificationService).addErrorMessage(any(), eq("No such payment request: " + id));
  }
}
