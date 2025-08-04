package chpay.frontend.customer.pages;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("playwright")
public class IndexTest {

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
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");
    page.navigate(baseUrl + "/index");
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void clickBalanceLink() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Balance")).click();
    assertEquals(page.url(), baseUrl + "/balance");
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
    page.waitForSelector(
        ".cookie-text", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
    assertEquals(0, page.locator(".cookie-text").count());
    Locator container = page.locator(".balance-container");
    Locator balanceLabel = container.locator(".balance-label");
    assertTrue(
        balanceLabel.textContent().contains("Balance:"), "Balance label missing or incorrect");

    Locator topupLabel = container.locator(".topup-label");
    assertTrue(topupLabel.textContent().contains("Top-up Balance"), "Top-up label missing");

    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    var sidebar = page.locator("aside.sidebar");
    assertTrue(sidebar.isVisible(), "Sidebar should be visible");

    var logo = page.locator(".sidebar-logo img");
    assertTrue(logo.isVisible(), "Logo should be visible in sidebar");
    assertEquals(
        "Christiaan Huygens Logo", logo.getAttribute("alt"), "Logo should have correct alt text");

    Locator maxBalance = container.locator(".topup-max");
    assertTrue(
        maxBalance.textContent().contains("Max Balance"),
        "Max balance display missing or incorrect");

    assertEquals(
        "post",
        container.locator("form#topup-form").getAttribute("method"),
        "Top-up form method should be POST");

    Locator topupButtons = container.locator(".btn-indigo");
    assertEquals(3, topupButtons.count(), "There should be exactly 3 preset top-up buttons");

    Locator customInput = container.locator("#customAmount");
    assertTrue(customInput.isVisible(), "Custom amount input is not visible");

    Locator payButton = container.locator("#payButton");
    assertTrue(payButton.isVisible(), "Pay button is not visible");

    var footer = page.locator(".app-footer");
    assertTrue(footer.isVisible(), "Footer should be visible on balance page");
    assertTrue(
        footer.innerText().contains("Copyright © W.I.S.V. 'Christiaan Huygens' 2025 "),
        "Footer should contain copyright text");

    var cookiePolicyLink = page.locator(".app-footer .cookie-link");
    assertTrue(cookiePolicyLink.isVisible(), "Cookie policy link should be visible");
    assertTrue(
        cookiePolicyLink.innerText().contains("Cookie Policy"),
        "Link should contain 'Cookie Policy' text");
  }

  @Test
  void clickAdminLink() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Admin View")).click();
    assertEquals(page.url(), baseUrl + "/admin");

    Locator container = page.locator(".admin-container");
    assertTrue(
        container.locator("h1").textContent().contains("Admin Dashboard"),
        "Admin Dashboard header missing");

    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    var sidebar = page.locator("aside.sidebar");
    assertTrue(sidebar.isVisible(), "Sidebar should be visible");

    var logo = page.locator(".sidebar-logo img");
    assertTrue(logo.isVisible(), "Logo should be visible in sidebar");
    assertEquals(
        "Christiaan Huygens Logo", logo.getAttribute("alt"), "Logo should have correct alt text");
  }

  @Test
  void clickTransactionsLink() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
    page.waitForSelector(
        ".cookie-text", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
    assertEquals(0, page.locator(".cookie-text").count());
    Locator container = page.locator(".transactions-container");

    assertTrue(
        container.locator(".transactions-title").textContent().contains("Past Transactions"),
        "Transactions title missing");

    Locator filterButtons = container.locator(".transactions-filter button");
    assertEquals(2, filterButtons.count(), "Expected 2 filter buttons (All and Top-Ups)");

    Locator tableHeader = container.locator(".table-header .table-cell");
    assertEquals(5, tableHeader.count(), "Expected 5 table header cells");

    assertTrue(
        container.locator("#dateCell").textContent().contains("Date"),
        "Date column header missing");
    assertTrue(
        container.locator("#descCell").textContent().contains("Product/Event"),
        "Description column header missing");
    assertTrue(
        container.locator("#totalCell").textContent().contains("Total"),
        "Total column header missing");

    Locator navButtons = container.locator(".transaction-nav-button");
    assertEquals(2, navButtons.count(), "Expected navigation buttons (Prev and Next)");

    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    var sidebar = page.locator("aside.sidebar");
    assertTrue(sidebar.isVisible(), "Sidebar should be visible");

    var logo = page.locator(".sidebar-logo img");
    assertTrue(logo.isVisible(), "Logo should be visible in sidebar");
    assertEquals(
        "Christiaan Huygens Logo", logo.getAttribute("alt"), "Logo should have correct alt text");

    Locator prevButton = container.locator("#prevButt");
    Locator nextButton = container.locator("#nextButt");
    assertTrue(prevButton.textContent().contains("←Prev"), "Previous button label incorrect");
    assertTrue(nextButton.textContent().contains("Next→"), "Next button label incorrect");

    var footer = page.locator(".app-footer");
    assertTrue(footer.isVisible(), "Footer should be visible on transaction page");
    assertTrue(
        footer.innerText().contains("Copyright © W.I.S.V. 'Christiaan Huygens' 2025 "),
        "Footer should contain copyright text");

    var cookiePolicyLink = page.locator(".app-footer .cookie-link");
    assertTrue(cookiePolicyLink.isVisible(), "Cookie policy link should be visible");
    assertTrue(
        cookiePolicyLink.innerText().contains("Cookie Policy"),
        "Link should contain 'Cookie Policy' text");
  }
}
