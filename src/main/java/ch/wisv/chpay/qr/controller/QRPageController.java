package ch.wisv.chpay.qr.controller;

import ch.wisv.chpay.core.controller.PageController;
import ch.wisv.chpay.core.model.PaymentRequest;
import ch.wisv.chpay.core.repository.RequestRepository;
import ch.wisv.chpay.qr.util.QRCodeUtil;
import com.google.zxing.WriterException;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/qr")
public class QRPageController extends PageController {

  @Autowired RequestRepository requestRepository;

  /** Inject the `spring.application.base-url` from application.yml (or from $BASE_URL). */
  @Value("${spring.application.baseurl}")
  private String baseUrl;

  /**
   * Constructor for QRPageController.
   *
   * @param requestRepository
   */
  public QRPageController(RequestRepository requestRepository) {
    this.requestRepository = requestRepository;
  }

  /**
   * Generates a QR code for a given payment request ID and prepares the model with relevant data to
   * display the QR code along with payment request details on the "qr" page.
   *
   * @param paymentRequestId the unique identifier of the payment request
   * @param model the model object to supply attributes for rendering the view
   * @return the name of the Thymeleaf template to render, "qr"
   * @throws IOException if an error occurs while generating the QR code
   * @throws WriterException if an error occurs in encoding the QR code
   */
  @GetMapping("/{paymentRequestId}")
  public String showQR(@PathVariable String paymentRequestId, Model model)
      throws IOException, WriterException {
    String paymentURL = baseUrl + "/payment/request/" + paymentRequestId;
    String qrCodeBase64 = QRCodeUtil.generateQRCodeBase64(paymentURL, 250, 250);

    PaymentRequest pr =
        requestRepository
            .findById(UUID.fromString(paymentRequestId))
            .orElseThrow(
                () -> new NoSuchElementException("No such payment request: " + paymentRequestId));

    model.addAttribute(MODEL_ATTR_QR_CODE_BASE64, qrCodeBase64);
    model.addAttribute(MODEL_ATTR_PAYMENT_REQUEST, pr);
    model.addAttribute(MODEL_ATTR_PAYMENT_REQUEST_ID, paymentRequestId);
    return "qr";
  }
}
