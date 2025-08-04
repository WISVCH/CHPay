package chpay.frontend.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import chpay.DatabaseHandler.transactiondb.entities.PaymentRequest;
import chpay.paymentbackend.service.RequestService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NewPaymentRequestPageControllerTest {

  private MockMvc mockMvc;

  @Mock private RequestService requestService;

  @InjectMocks private NewPaymentRequestPageController controller;

  @Captor private ArgumentCaptor<BigDecimal> amountCaptor;

  @Captor private ArgumentCaptor<String> descriptionCaptor;

  @Captor private ArgumentCaptor<Boolean> multiUseCaptor;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  /**
   * Tests the delivery of the correct status, urLPage attribute, and view name.
   *
   * @throws Exception
   */
  @Test
  void testShowNewPaymentRequestPage() throws Exception {
    mockMvc
        .perform(get("/admin/createPaymentRequest"))
        .andExpect(status().isOk())
        .andExpect(view().name("create-payment-request"))
        .andExpect(model().attribute("urlPage", "createPaymentRequest"));
  }

  /**
   * Tests good weather behavior with multi-use.
   *
   * @throws Exception
   */
  @Test
  void withMultiUseTrue() throws Exception {

    UUID fixedId = UUID.fromString("00000000-0000-0000-0000-00000000abcd");
    PaymentRequest fakeRequest = org.mockito.Mockito.mock(PaymentRequest.class);
    when(fakeRequest.getRequest_id()).thenReturn(fixedId);

    when(requestService.createRequest(
            eq(new BigDecimal("42.50")), eq("Test Description"), eq(true)))
        .thenReturn(fakeRequest);

    mockMvc
        .perform(
            post("/admin/createPaymentRequest")
                .param("description", "Test Description")
                .param("amount", "42.50")
                .param("multiUse", "true"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/qr/" + fixedId.toString()));

    verify(requestService)
        .createRequest(
            amountCaptor.capture(), descriptionCaptor.capture(), multiUseCaptor.capture());

    BigDecimal capturedAmount = amountCaptor.getValue();
    String capturedDescription = descriptionCaptor.getValue();
    Boolean capturedMultiUse = multiUseCaptor.getValue();

    assert capturedAmount.compareTo(new BigDecimal("42.50")) == 0;
    assert capturedDescription.equals("Test Description");
    assert capturedMultiUse.equals(true);
  }

  /**
   * Tests good weather behavior without multi-use.
   *
   * @throws Exception
   */
  @Test
  void defaultMultiUseFalse() throws Exception {
    UUID fixedId2 = UUID.fromString("00000000-0000-0000-0000-00000000beef");

    PaymentRequest fakeRequest = org.mockito.Mockito.mock(PaymentRequest.class);
    when(fakeRequest.getRequest_id()).thenReturn(fixedId2);

    when(requestService.createRequest(eq(new BigDecimal("10.00")), eq("Another Test"), eq(false)))
        .thenReturn(fakeRequest);

    mockMvc
        .perform(
            post("/admin/createPaymentRequest")
                .param("description", "Another Test")
                .param("amount", "10.00"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/qr/" + fixedId2.toString()));

    verify(requestService)
        .createRequest(
            amountCaptor.capture(), descriptionCaptor.capture(), multiUseCaptor.capture());

    BigDecimal capturedAmount = amountCaptor.getValue();
    String capturedDescription = descriptionCaptor.getValue();
    Boolean capturedMultiUse = multiUseCaptor.getValue();

    assert capturedAmount.compareTo(new BigDecimal("10.00")) == 0;
    assert capturedDescription.equals("Another Test");
    assert capturedMultiUse.equals(false);
  }

  /**
   * Tests bad weather scenario where description is missing.
   *
   * @throws Exception
   */
  @Test
  void missingDescriptionshouldReturnBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/admin/createPaymentRequest")
                // .param("description", "…") is omitted
                .param("amount", "25.00")
            // multiUse defaults to false if omitted
            )
        .andExpect(status().isBadRequest());
  }

  /**
   * Tests bad weather scenario where where amount is missing.
   *
   * @throws Exception
   */
  @Test
  void missingAmountshouldReturnBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/admin/createPaymentRequest").param("description", "No amount provided")
            // .param("amount", "…") is omitted
            )
        .andExpect(status().isBadRequest());
  }

  /**
   * Tests bad weather scenario where amount is invalid.
   *
   * @throws Exception
   */
  @Test
  void invalidAmountFormatshouldReturnBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/admin/createPaymentRequest")
                .param("description", "Bad amount")
                .param("amount", "notANumber")
                .param("multiUse", "true"))
        .andExpect(status().isBadRequest());
  }

  /**
   * Tests bad weather scenario where multi-use is invalid.
   *
   * @throws Exception
   */
  @Test
  void invalidMultiUseFormatshouldReturnBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/admin/createPaymentRequest")
                .param("description", "Bad multiUse")
                .param("amount", "15.00")
                .param("multiUse", "banana"))
        .andExpect(status().isBadRequest());
  }
}
