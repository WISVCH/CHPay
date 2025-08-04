package chpay.frontend.admin.pages;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("playwright")
public class TerminalPageTest {

  @Value("${spring.application.base-url}")
  private String baseUrl;

  static Playwright pw;
  static Browser browser;

  BrowserContext context;
  Page page;

  @BeforeAll
  static void launchBrowser() {
    pw = Playwright.create();
    browser = pw.firefox().launch();
  }

  @AfterAll
  static void closeBrowser() {
    if (browser != null) browser.close();
    if (pw != null) pw.close();
  }

  @BeforeEach
  void createContextAndPage() {
    context = browser.newContext();
    page = context.newPage();
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void mainTerminalPageTest() {
    // Log in as admin
    page.setViewportSize(1920, 1080);
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");

    page.navigate(baseUrl);
    assertTrue(page.url().contains("/index"));

    // Wait for sidebar to appear and look for the "Terminal" button
    page.waitForSelector("aside.sidebar");
    var adminButton = page.locator(".sidebar-nav a[href='/admin']");
    assertTrue(adminButton.isVisible(), "Admin button should be visible");
    assertTrue(adminButton.innerText().contains("Admin View"));
    // Find the terminal link (fails if not present)
    adminButton.click();

    // look for the "Terminal" button
    var terminalButton = page.locator(".sidebar-nav a[href='/admin/createPaymentRequest']");
    page.waitForURL(url -> url.endsWith("/admin"));
    assertTrue(terminalButton.isVisible(), "Terminal button should be visible");
    assertTrue(terminalButton.innerText().contains("Terminal"));
    // Open the terminal page
    terminalButton.click();

    // Should navigate to createPaymentRequest page
    page.waitForURL(url -> url.endsWith("/admin/createPaymentRequest"));

    // Verify page title
    assertTrue(page.locator(".title").isVisible(), "Page title should be visible");
    assertTrue(
        page.locator(".title").innerText().contains("Create Payment Request"),
        "Page should have the correct title");

    // Verify form elements
    var form = page.locator("form[action='/admin/createPaymentRequest']");
    assertTrue(form.isVisible(), "Payment request form should be visible");

    // Check form fields
    var descriptionField = page.locator("#description");
    assertTrue(descriptionField.isVisible(), "Description field should be visible");
    assertEquals(
        "text", descriptionField.getAttribute("type"), "Description should be a text input");
    assertTrue(descriptionField.getAttribute("required") != null, "Description should be required");

    var amountField = page.locator("#amount");
    assertTrue(amountField.isVisible(), "Amount field should be visible");
    assertEquals("number", amountField.getAttribute("type"), "Amount should be a number input");
    assertEquals("0.01", amountField.getAttribute("step"), "Amount should have 0.01 step");
    assertEquals("0.01", amountField.getAttribute("min"), "Amount should have 0.01 minimum");
    assertTrue(amountField.getAttribute("required") != null, "Amount should be required");

    // The checkbox is the actual logical checkbox, hidden behind the toggle which is just the
    // visual representation
    var multiUseCheckbox = page.locator("#multiUse");
    var multiUseToggle = page.locator(".toggle-switch");
    assertTrue(multiUseToggle.isVisible(), "Multi-use toggle should be visible");
    assertEquals(
        "checkbox", multiUseCheckbox.getAttribute("type"), "Multi-use should be a checkbox");
    assertFalse(multiUseCheckbox.isChecked(), "Multi-use checkbox should be unchecked by default");

    var submitButton = page.locator("div.form-actions button.btn-geel[type='submit']");
    assertTrue(submitButton.isVisible(), "Submit button should be visible");
    assertTrue(
        submitButton.getAttribute("class").contains("disabled"),
        "Submit button should be disabled initially");
    assertTrue(
        submitButton.isDisabled(), "Submit button should be disabled until fields are filled");
    assertEquals(
        "Go to QR", submitButton.innerText().trim(), "Submit button should have the correct text");

    // Test form validation - initially fields are highlighted in red due to validation
    assertTrue(
        descriptionField.getAttribute("style").contains("border-color: red"),
        "Description field should have red border when empty");
    assertTrue(
        amountField.getAttribute("style").contains("border-color: red"),
        "Amount field should have red border when empty");

    // Fill out the form
    descriptionField.fill("Test payment request");
    amountField.fill("10.50");

    // Verify submit button becomes enabled
    page.waitForSelector("button.btn-geel:not(.disabled)");
    assertFalse(submitButton.isDisabled(), "Submit button should be enabled after filling form");

    // Toggle multi-use checkbox and verify it works
    multiUseToggle.check();
    assertTrue(multiUseCheckbox.isChecked(), "Should be able to check the multi-use checkbox");

    multiUseToggle.uncheck();
    assertFalse(multiUseCheckbox.isChecked(), "Should be able to uncheck the multi-use checkbox");

    // Check the checkbox for final test
    multiUseToggle.check();

    // Don't actually submit in this test, but verify form is ready to submit
    assertTrue(submitButton.isEnabled(), "Submit button should be enabled with valid form data");

    submitButton.click();

    // Wait for redirection to QR page
    page.waitForURL(url -> url.contains("/qr/"));
    assertTrue(page.url().contains("/qr/"), "Should be redirected to QR code page");

    // Verify QR page elements
    var qrContainer = page.locator(".qr-content");
    assertTrue(qrContainer.isVisible(), "QR content should be visible");

    var backButton = page.locator(".back-button");
    assertTrue(backButton.isVisible(), "Back button should be visible");
    assertEquals(
        "< Terminal", backButton.innerText().trim(), "Back button should have correct text");
    assertEquals(
        "/admin/createPaymentRequest",
        backButton.getAttribute("href"),
        "Back button should link to terminal page");

    var qrImage = page.locator(".qr-image img");
    assertTrue(qrImage.isVisible(), "QR code image should be visible");
    assertEquals("QR Code", qrImage.getAttribute("alt"), "QR image should have correct alt text");

    // Verify payment information
    var paymentInfo = page.locator(".payment-info");
    assertTrue(paymentInfo.isVisible(), "Payment info section should be visible");

    var paymentAmount = page.locator(".payment-amount");
    assertTrue(paymentAmount.isVisible(), "Payment amount should be visible");
    assertEquals(
        "EUR 10.50",
        paymentAmount.innerText().trim(),
        "Payment amount should match what we entered");

    var paymentDescription = page.locator(".payment-description");
    assertTrue(paymentDescription.isVisible(), "Payment description should be visible");
    assertEquals(
        "Test payment request",
        paymentDescription.innerText().trim(),
        "Payment description should match what we entered");

    // Check that payment date is in the correct format (don't check exact date as it will vary)
    var paymentDate = page.locator(".payment-date");
    assertTrue(paymentDate.isVisible(), "Payment date should be visible");

    // Check that payment ID is displayed
    var paymentId = page.locator(".payment-id");
    assertTrue(paymentId.isVisible(), "Payment ID should be visible");
    assertTrue(paymentId.innerText().contains("ID: "), "Payment ID should be labeled correctly");

    // Verify QR code is a clickable link
    var qrLink = page.locator(".qr-image a");
    assertTrue(qrLink.isVisible(), "QR code should be a clickable link");
    assertTrue(
        qrLink.getAttribute("href").contains("/payment/"),
        "QR link should point to payment endpoint");

    // Click the QR code link to simulate payment
    qrLink.click();

    // Wait for redirection to payment page
    page.waitForURL(url -> url.contains("/payment/"));
    assertTrue(
        page.url().contains("/payment/"), "Clicking QR code should redirect to payment page");
  }
}
