package chpay.frontend.admin.pages;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("playwright")
public class SystemSettingsTest {

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
  void SystemSettingsPageTest() {
    // Log in as admin
    page.setViewportSize(1920, 1080);
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");

    page.navigate(baseUrl);
    assertTrue(page.url().contains("/index"));

    // Wait for sidebar to appear and click the Admin View button
    page.waitForSelector("aside.sidebar");
    var adminButton = page.locator(".sidebar-nav a[href='/admin']");
    assertTrue(adminButton.isVisible(), "Admin button should be visible");
    assertTrue(adminButton.innerText().contains("Admin View"));
    adminButton.click();

    // Wait for admin page to load
    page.waitForURL(url -> url.endsWith("/admin"));

    // Look for the System Settings button and click it
    var systemSettingsButton = page.locator(".sidebar-nav a:has-text('System Setting')");
    assertTrue(systemSettingsButton.isVisible(), "System Settings button should be visible");
    assertTrue(systemSettingsButton.innerText().contains("System Setting"));
    systemSettingsButton.click();

    // Should navigate to system settings page
    page.waitForURL(url -> url.endsWith("/admin/settings"));

    // Verify page title
    assertTrue(page.locator(".title").isVisible(), "Page title should be visible");
    assertTrue(
        page.locator(".title").innerText().contains("System Settings"),
        "Page should have the correct title");

    // Verify form elements
    var form = page.locator("form[action='/admin/settings']");
    assertTrue(form.isVisible(), "Settings form should be visible");

    // Check maximum balance field
    var maximumBalanceField = page.locator("#maximumBalance");
    assertTrue(maximumBalanceField.isVisible(), "Maximum Balance field should be visible");
    assertEquals(
        "number",
        maximumBalanceField.getAttribute("type"),
        "Maximum Balance should be a number input");
    assertEquals(
        "0.01", maximumBalanceField.getAttribute("step"), "Maximum Balance should have 0.01 step");
    assertEquals(
        "0.01",
        maximumBalanceField.getAttribute("min"),
        "Maximum Balance should have 0.01 minimum");

    // Check system freeze toggle
    var systemFreezeCheckbox = page.locator("#isFrozen");
    var systemFreezeToggle = page.locator(".toggle-switch");
    assertTrue(systemFreezeToggle.isVisible(), "System Freeze toggle should be visible");
    assertEquals(
        "checkbox",
        systemFreezeCheckbox.getAttribute("type"),
        "System Freeze should be a checkbox");

    // Check submit button
    var submitButton = page.locator(".form-actions button[type='submit']");
    assertTrue(submitButton.isVisible(), "Submit button should be visible");
    assertTrue(
        submitButton.getAttribute("class").contains("disabled"),
        "Submit button should be disabled initially");
    assertTrue(
        submitButton.isDisabled(), "Submit button should be disabled until fields are filled");
    assertEquals(
        "Save Settings >",
        submitButton.innerText().trim(),
        "Submit button should have the correct text");

    // Fill out the form
    maximumBalanceField.fill("200.00");

    // Verify submit button becomes enabled
    page.waitForSelector("button:not(.disabled)");
    assertFalse(submitButton.isDisabled(), "Submit button should be enabled after filling form");

    // Toggle system freeze checkbox and verify it works
    systemFreezeToggle.check();
    assertTrue(
        systemFreezeCheckbox.isChecked(), "Should be able to check the system freeze checkbox");

    systemFreezeToggle.uncheck();
    assertFalse(
        systemFreezeCheckbox.isChecked(), "Should be able to uncheck the system freeze checkbox");

    // Test edge cases for maximum balance input
    maximumBalanceField.fill("0.01"); // minimum value
    assertEquals("0.01", maximumBalanceField.inputValue(), "Should accept minimum value");

    maximumBalanceField.fill("999.99"); // large value
    assertEquals("999.99", maximumBalanceField.inputValue(), "Should accept large values");

    // Test System Freeze functionality
    maximumBalanceField.fill("300.00");
    systemFreezeToggle.check();
    assertTrue(systemFreezeCheckbox.isChecked(), "System freeze checkbox should be checked");

    // Submit the form
    submitButton.click();

    // Verify system freeze warning appears
    page.waitForSelector("#freeze-warning");
    var freezeWarning = page.locator("#freeze-warning");
    page.hover("#freeze-warning");
    page.waitForTimeout(500);
    assertTrue(freezeWarning.isVisible(), "System freeze warning should be visible");
    assertTrue(
        page.locator(".warning-text").innerText().contains("Warning: System is frozen"),
        "Warning should indicate system is frozen");

    // Return to user view to verify effects
    var userViewButton = page.locator(".sidebar-link:has-text('Return to user view')");
    assertTrue(userViewButton.isVisible(), "Return to user view button should be visible");
    userViewButton.click();

    // Navigate to balance page
    page.waitForURL(url -> url.endsWith("/index"));
    page.navigate(baseUrl + "/balance");

    // Verify system freeze warning appears on balance page too
    page.waitForSelector("#freeze-warning");
    freezeWarning = page.locator("#freeze-warning");
    page.hover("#freeze-warning");
    page.waitForTimeout(500);
    assertTrue(
        freezeWarning.isVisible(), "System freeze warning should be visible on balance page");
    assertTrue(
        freezeWarning.locator(".warning-text").innerText().contains("Warning: System is frozen"),
        "Warning should indicate system is frozen on balance page");

    // Verify maximum balance is displayed correctly
    var topupMax = page.locator(".topup-max");
    assertTrue(topupMax.isVisible(), "Maximum balance display should be visible");
    assertTrue(
        topupMax.innerText().contains("Max Balance: 300.00€"),
        "Maximum balance should display the value we set");

    // Go back to admin view to disable system freeze
    page.waitForSelector("aside.sidebar");
    adminButton = page.locator(".sidebar-nav a[href='/admin']");
    adminButton.click();

    // Wait for admin page to load
    page.waitForURL(url -> url.endsWith("/admin"));

    // Go to system settings
    systemSettingsButton = page.locator(".sidebar-nav a:has-text('System Setting')");
    systemSettingsButton.click();

    // Should navigate to system settings page
    page.waitForURL(url -> url.endsWith("/admin/settings"));

    // Uncheck system freeze
    systemFreezeToggle = page.locator(".toggle-switch");
    systemFreezeToggle.uncheck();
    assertFalse(
        page.locator("#isFrozen").isChecked(), "System freeze checkbox should be unchecked");

    // Submit the form again
    submitButton = page.locator(".form-actions button[type='submit']");
    submitButton.click();

    // Return to user view
    userViewButton = page.locator(".sidebar-link:has-text('Return to user view')");
    userViewButton.click();

    // Navigate to balance page
    page.waitForURL(url -> url.endsWith("/index"));
    page.navigate(baseUrl + "/balance");

    // Verify freeze warning is gone
    assertFalse(
        page.locator("#freeze-warning").isVisible(),
        "System freeze warning should no longer be visible after disabling freeze");

    // Verify maximum balance is still displayed correctly
    topupMax = page.locator(".topup-max");
    assertTrue(topupMax.isVisible(), "Maximum balance display should be visible");
    assertTrue(
        topupMax.innerText().contains("Max Balance: 300.00€"),
        "Maximum balance should still display the value we set");
  }
}
