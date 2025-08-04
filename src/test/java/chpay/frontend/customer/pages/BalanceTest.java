package chpay.frontend.customer.pages;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("playwright")
public class BalanceTest {

  @Value("${spring.application.base-url}")
  private String baseUrl;

  static Playwright pw;
  static Browser browser;

  BrowserContext context;
  Page page;

  @BeforeAll
  static void launchBrowser() {
    pw = Playwright.create();
    browser = pw.chromium().launch();
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
    page.navigate(baseUrl);
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  private void goToFirstPageIfNeeded() {
    Locator firstPageButton = page.locator("button[id='prevButt']");
    while (firstPageButton.count() > 0 && !firstPageButton.isDisabled()) {
      firstPageButton.click();
      page.waitForLoadState(LoadState.NETWORKIDLE);
    }
  }

  private boolean goToNextPageIfExists() {
    Locator nextButton = page.locator("button[id='nextButt']");
    if (nextButton.count() > 0 && !nextButton.isDisabled()) {
      nextButton.click();
      page.waitForLoadState(LoadState.NETWORKIDLE);
      return true;
    }
    return false;
  }

  private List<Double> getProductEventTotals() {
    goToFirstPageIfNeeded();
    List<Double> allValues = new ArrayList<>();
    do {
      Locator rows = page.locator(".transactions-table .table-row");
      for (int i = 0; i < rows.count(); i++) {
        Locator cell = rows.nth(i).locator(".table-pairs:nth-child(2) .table-cell:nth-child(1)");
        if (cell.count() > 0) {
          allValues.add(Double.parseDouble(cell.textContent().trim()));
        }
      }
    } while (goToNextPageIfExists());
    return allValues;
  }

  @Test
  void testBalance5() {

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

    Locator maxBalanceLabel = container.locator(".topup-max");
    assertTrue(
        maxBalanceLabel.textContent().contains("Max Balance"),
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
    assertTrue(
        page.waitForSelector("span[class='transaction-fees']")
            .innerText()
            .contains("Transaction fees of"));

    String initialBalanceText = page.textContent("p.balance-label span").trim();
    double initialBalance = Double.parseDouble(initialBalanceText.replace("€", "").trim());
    double expectedBalance = initialBalance + 5;
    page.click("button:has-text(\"€5\")");
    if (!page.locator("#payButton").isEnabled()) {
      String maxBalanceText = page.textContent("span.topup-max").split(" ")[2];
      double maxBalance = Double.parseDouble(maxBalanceText.replace("€", "").trim());
      assertTrue(maxBalance - expectedBalance < 0);
    } else {
      page.click("#payButton");
      page.waitForURL(Pattern.compile("https://(www\\.)?mollie\\.com.*"));
      assertTrue(page.url().contains("https://www.mollie.com"));
      assertEquals(
          "Note: this is a testmode payment.",
          page.waitForSelector(".alert--warning").textContent().trim());
      Locator disclaimerLink = page.locator("a.disclaimer__link");

      String href = disclaimerLink.getAttribute("href");
      assertEquals("https://www.mollie.com/", href, "Link 'href' attribute is incorrect.");

      String linkText = disclaimerLink.textContent().trim();
      assertEquals("Payment secured and provided by", linkText, "Link text content is incorrect.");
      assertEquals(
          "Test profile", page.waitForSelector(".header__merchant-name").textContent().trim());
      assertEquals(
          "W.I.S.V. 'Christiaan Huygens'",
          page.waitForSelector(".header__info-text").textContent().trim());
      assertEquals("€5.32", page.waitForSelector(".header__amount").textContent().trim());

      page.click("button[value='ideal']");
      assertTrue(page.url().contains("checkout"));
      page.click("button[value='ideal_ABNANL2A']");
      assertTrue(page.url().contains("method=ideal&token"));
      page.click("input[type='radio'][name='final_state'][value='paid']");
      page.click("button[name='submit']");
      assertTrue(page.url().contains("payment/complete"));
      assertTrue(
          page.waitForSelector("h1").innerText().contains("Payment pending")
              || page.waitForSelector("h1").innerText().contains("Payment successful"));
      var home = page.waitForSelector("a.btn.btn-large.btn-indigo[href='/']");
      assertTrue(page.waitForSelector("h1").innerText().contains("Payment successful"));
      home.click();
      page.waitForURL("**/index*");
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Balance")).click();
      String newBalanceText = page.textContent("p.balance-label span").trim();
      double actualNewBalance = Double.parseDouble(newBalanceText.replace("€", "").trim());
      assertEquals(String.format("%.2f", expectedBalance), String.format("%.2f", actualNewBalance));

      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
      assertEquals(page.url(), baseUrl + "/transactions");
      assertTrue(
          page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
      assertEquals(
          "Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

      assertTrue(
          page.locator("h2.transactions-title").isVisible(),
          "Transactions header should be visible");
      assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
      // Assert footer exists
      // Click 'Cookie Policy' and verify modal shows
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cookie Policy")).click();
      Locator modal = page.locator("#cookie-policy-modal");
      assertTrue(modal.isVisible(), "Cookie Policy modal must be visible after clicking");

      // Close the modal
      page.locator("#cookie-policy-modal .cookie-button").click();
      assertFalse((Boolean) modal.evaluate("el => el.classList.contains('show')"));
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

      assertTrue(page.locator("#dateCell").isVisible(), "Date header cell should be visible");
      assertTrue(
          page.locator("#descCell").isVisible(), "Description header cell should be visible");
      assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");

      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      int rowCount = page.locator(".transactions-table .table-row").count();
      assertNotEquals(0, rowCount);
      page.click("button[id='topUpButt']");
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      List<Double> topups = getProductEventTotals();
      Double toTest = 5.0;
      assertTrue(topups.contains(toTest));
    }
  }

  @Test
  void testBalance10() {
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

    Locator maxBalanceLabel = container.locator(".topup-max");
    assertTrue(
        maxBalanceLabel.textContent().contains("Max Balance"),
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
    assertTrue(
        page.waitForSelector("span[class='transaction-fees']")
            .innerText()
            .contains("Transaction fees of"));
    String initialBalanceText = page.textContent("p.balance-label span").trim();
    double initialBalance = Double.parseDouble(initialBalanceText.replace("€", "").trim());
    double expectedBalance = initialBalance + 10;

    page.click("button:has-text(\"€10\")");
    if (!page.locator("#payButton").isEnabled()) {
      String maxBalanceText = page.textContent("span.topup-max").split(" ")[2];
      double maxBalance = Double.parseDouble(maxBalanceText.replace("€", "").trim());
      assertTrue(maxBalance - expectedBalance < 0);
    } else {
      page.click("#payButton");
      page.waitForURL(Pattern.compile("https://(www\\.)?mollie\\.com.*"));
      assertTrue(page.url().contains("https://www.mollie.com"));
      assertEquals(
          "Note: this is a testmode payment.",
          page.waitForSelector(".alert--warning").textContent().trim());
      Locator disclaimerLink = page.locator("a.disclaimer__link");

      String href = disclaimerLink.getAttribute("href");
      assertEquals("https://www.mollie.com/", href, "Link 'href' attribute is incorrect.");

      String linkText = disclaimerLink.textContent().trim();
      assertEquals("Payment secured and provided by", linkText, "Link text content is incorrect.");
      assertEquals(
          "Test profile", page.waitForSelector(".header__merchant-name").textContent().trim());
      assertEquals(
          "W.I.S.V. 'Christiaan Huygens'",
          page.waitForSelector(".header__info-text").textContent().trim());
      assertEquals("€10.32", page.waitForSelector(".header__amount").textContent().trim());

      page.click("button[value='ideal']");
      assertTrue(page.url().contains("checkout"));
      page.click("button[value='ideal_ABNANL2A']");
      assertTrue(page.url().contains("method=ideal&token"));
      page.click("input[type='radio'][name='final_state'][value='paid']");
      page.click("button[name='submit']");
      assertTrue(page.url().contains("payment/complete"));
      assertTrue(
          page.waitForSelector("h1").innerText().contains("Payment pending")
              || page.waitForSelector("h1").innerText().contains("Payment successful"));
      var home = page.waitForSelector("a.btn.btn-large.btn-indigo[href='/']");
      assertTrue(page.waitForSelector("h1").innerText().contains("Payment successful"));
      home.click();
      page.waitForURL("**/index*");
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Balance")).click();
      String newBalanceText = page.textContent("p.balance-label span").trim();
      double actualNewBalance = Double.parseDouble(newBalanceText.replace("€", "").trim());
      assertEquals(String.format("%.2f", expectedBalance), String.format("%.2f", actualNewBalance));

      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
      assertEquals(page.url(), baseUrl + "/transactions");
      assertTrue(
          page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
      assertEquals(
          "Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

      assertTrue(
          page.locator("h2.transactions-title").isVisible(),
          "Transactions header should be visible");
      assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
      // Assert footer exists
      // Click 'Cookie Policy' and verify modal shows
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cookie Policy")).click();
      Locator modal = page.locator("#cookie-policy-modal");
      assertTrue(modal.isVisible(), "Cookie Policy modal must be visible after clicking");

      // Close the modal
      page.locator("#cookie-policy-modal .cookie-button").click();
      assertFalse((Boolean) modal.evaluate("el => el.classList.contains('show')"));
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

      assertTrue(page.locator("#dateCell").isVisible(), "Date header cell should be visible");
      assertTrue(
          page.locator("#descCell").isVisible(), "Description header cell should be visible");
      assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      int rowCount = page.locator(".transactions-table .table-row").count();
      assertNotEquals(0, rowCount);
      page.click("button[id='topUpButt']");
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      List<Double> topups = getProductEventTotals();
      Double toTest = 10.0;
      assertTrue(topups.contains(toTest));
    }
  }

  @Test
  void testBalance20() {
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

    Locator maxBalanceLabel = container.locator(".topup-max");
    assertTrue(
        maxBalanceLabel.textContent().contains("Max Balance"),
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
    assertTrue(
        page.waitForSelector("span[class='transaction-fees']")
            .innerText()
            .contains("Transaction fees of"));
    String initialBalanceText = page.textContent("p.balance-label span").trim();
    double initialBalance = Double.parseDouble(initialBalanceText.replace("€", "").trim());
    double expectedBalance = initialBalance + 20;

    page.click("button:has-text(\"€20\")");
    if (!page.locator("#payButton").isEnabled()) {
      String maxBalanceText = page.textContent("span.topup-max").split(" ")[2];
      double maxBalance = Double.parseDouble(maxBalanceText.replace("€", "").trim());
      assertTrue(maxBalance - expectedBalance < 0);
    } else {
      page.click("#payButton");
      page.waitForURL(Pattern.compile("https://(www\\.)?mollie\\.com.*"));
      assertTrue(page.url().contains("https://www.mollie.com"));
      assertEquals(
          "Note: this is a testmode payment.",
          page.waitForSelector(".alert--warning").textContent().trim());
      Locator disclaimerLink = page.locator("a.disclaimer__link");

      String href = disclaimerLink.getAttribute("href");
      assertEquals("https://www.mollie.com/", href, "Link 'href' attribute is incorrect.");

      String linkText = disclaimerLink.textContent().trim();
      assertEquals("Payment secured and provided by", linkText, "Link text content is incorrect.");
      assertEquals(
          "Test profile", page.waitForSelector(".header__merchant-name").textContent().trim());
      assertEquals(
          "W.I.S.V. 'Christiaan Huygens'",
          page.waitForSelector(".header__info-text").textContent().trim());
      assertEquals("€20.32", page.waitForSelector(".header__amount").textContent().trim());

      page.click("button[value='ideal']");
      assertTrue(page.url().contains("checkout"));
      page.click("button[value='ideal_ABNANL2A']");
      assertTrue(page.url().contains("method=ideal&token"));
      page.click("input[type='radio'][name='final_state'][value='paid']");
      page.click("button[name='submit']");
      assertTrue(page.url().contains("payment/complete"));
      assertTrue(
          page.waitForSelector("h1").innerText().contains("Payment pending")
              || page.waitForSelector("h1").innerText().contains("Payment successful"));
      var home = page.waitForSelector("a.btn.btn-large.btn-indigo[href='/']");
      assertTrue(page.waitForSelector("h1").innerText().contains("Payment successful"));
      home.click();
      page.waitForURL("**/index*");
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Balance")).click();
      String newBalanceText = page.textContent("p.balance-label span").trim();
      double actualNewBalance = Double.parseDouble(newBalanceText.replace("€", "").trim());
      assertEquals(String.format("%.2f", expectedBalance), String.format("%.2f", actualNewBalance));

      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
      assertEquals(page.url(), baseUrl + "/transactions");
      assertTrue(
          page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
      assertEquals(
          "Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

      assertTrue(
          page.locator("h2.transactions-title").isVisible(),
          "Transactions header should be visible");
      assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
      // Assert footer exists
      // Click 'Cookie Policy' and verify modal shows
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cookie Policy")).click();
      Locator modal = page.locator("#cookie-policy-modal");
      assertTrue(modal.isVisible(), "Cookie Policy modal must be visible after clicking");

      // Close the modal
      page.locator("#cookie-policy-modal .cookie-button").click();
      assertFalse((Boolean) modal.evaluate("el => el.classList.contains('show')"));
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

      assertTrue(page.locator("#dateCell").isVisible(), "Date header cell should be visible");
      assertTrue(
          page.locator("#descCell").isVisible(), "Description header cell should be visible");
      assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      int rowCount = page.locator(".transactions-table .table-row").count();
      assertNotEquals(0, rowCount);
      page.click("button[id='topUpButt']");
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      List<Double> topups = getProductEventTotals();
      Double toTest = 20.0;
      assertTrue(topups.contains(toTest));
    }
  }

  @Test
  void testBalanceCustom() {
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

    Locator maxBalanceLabel = container.locator(".topup-max");
    assertTrue(
        maxBalanceLabel.textContent().contains("Max Balance"),
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
    assertTrue(
        page.waitForSelector("span[class='transaction-fees']")
            .innerText()
            .contains("Transaction fees of"));
    String initialBalanceText = page.textContent("p.balance-label span").trim();
    double initialBalance = Double.parseDouble(initialBalanceText.replace("€", "").trim());
    double expectedBalance = initialBalance + 14.99;

    page.fill("#customAmount", "14.99");
    if (!page.locator("#payButton").isEnabled()) {
      String maxBalanceText = page.textContent("span.topup-max").split(" ")[2];
      double maxBalance = Double.parseDouble(maxBalanceText.replace("€", "").trim());
      assertTrue(maxBalance - expectedBalance < 0);
    } else {
      page.click("#payButton");
      page.waitForURL(Pattern.compile("https://(www\\.)?mollie\\.com.*"));
      assertTrue(page.url().contains("https://www.mollie.com"));
      assertEquals(
          "Note: this is a testmode payment.",
          page.waitForSelector(".alert--warning").textContent().trim());
      Locator disclaimerLink = page.locator("a.disclaimer__link");

      String href = disclaimerLink.getAttribute("href");
      assertEquals("https://www.mollie.com/", href, "Link 'href' attribute is incorrect.");

      String linkText = disclaimerLink.textContent().trim();
      assertEquals("Payment secured and provided by", linkText, "Link text content is incorrect.");

      assertEquals(
          "Test profile", page.waitForSelector(".header__merchant-name").textContent().trim());
      assertEquals(
          "W.I.S.V. 'Christiaan Huygens'",
          page.waitForSelector(".header__info-text").textContent().trim());
      assertEquals("€15.31", page.waitForSelector(".header__amount").textContent().trim());

      page.click("button[value='ideal']");
      assertTrue(page.url().contains("checkout"));
      page.click("button[value='ideal_ABNANL2A']");
      assertTrue(page.url().contains("method=ideal&token"));
      page.click("input[type='radio'][name='final_state'][value='paid']");
      page.click("button[name='submit']");

      assertTrue(
          page.waitForSelector("h1").innerText().contains("Payment pending")
              || page.waitForSelector("h1").innerText().contains("Payment successful"));
      var home = page.waitForSelector("a.btn.btn-large.btn-indigo[href='/']");
      assertTrue(page.waitForSelector("h1").innerText().contains("Payment successful"));
      home.click();
      page.waitForURL("**/index*");
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Balance")).click();
      String newBalanceText = page.textContent("p.balance-label span").trim();
      double actualNewBalance = Double.parseDouble(newBalanceText.replace("€", "").trim());
      assertEquals(String.format("%.2f", expectedBalance), String.format("%.2f", actualNewBalance));

      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
      assertEquals(page.url(), baseUrl + "/transactions");
      assertTrue(
          page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
      assertEquals(
          "Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
      assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

      assertTrue(
          page.locator("h2.transactions-title").isVisible(),
          "Transactions header should be visible");
      assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
      // Assert footer exists
      // Click 'Cookie Policy' and verify modal shows
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Cookie Policy")).click();
      Locator modal = page.locator("#cookie-policy-modal");
      assertTrue(modal.isVisible(), "Cookie Policy modal must be visible after clicking");

      // Close the modal
      page.locator("#cookie-policy-modal .cookie-button").click();
      assertFalse((Boolean) modal.evaluate("el => el.classList.contains('show')"));
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

      assertTrue(page.locator("#dateCell").isVisible(), "Date header cell should be visible");
      assertTrue(
          page.locator("#descCell").isVisible(), "Description header cell should be visible");
      assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      int rowCount = page.locator(".transactions-table .table-row").count();
      assertNotEquals(0, rowCount);
      page.click("button[id='topUpButt']");
      page.locator(".transactions-table .table-row")
          .first()
          .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
      List<Double> topups = getProductEventTotals();
      Double toTest = 14.99;
      assertTrue(topups.contains(toTest));
    }
  }

  @Test
  void testBalanceTooMuch() {
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

    Locator maxBalanceLabel = container.locator(".topup-max");
    assertTrue(
        maxBalanceLabel.textContent().contains("Max Balance"),
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
    assertTrue(
        page.waitForSelector("span[class='transaction-fees']")
            .innerText()
            .contains("Transaction fees of"));
    page.fill("#customAmount", "3999.99");
    assertFalse(page.locator("#payButton").isEnabled());
  }

  @Test
  void testBalanceInvalid() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Balance")).click();
    assertEquals(page.url(), baseUrl + "/balance");
    assertThrows(
        com.microsoft.playwright.PlaywrightException.class,
        () -> {
          page.fill("#customAmount", "askfj#(@$");
        });
  }

  @Test
  void testBalanceFail() {
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

    Locator maxBalanceLabel = container.locator(".topup-max");
    assertTrue(
        maxBalanceLabel.textContent().contains("Max Balance"),
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
    assertTrue(
        page.waitForSelector("span[class='transaction-fees']")
            .innerText()
            .contains("Transaction fees of"));
    String initialBalanceText = page.textContent("p.balance-label span").trim();
    double initialBalance = Double.parseDouble(initialBalanceText.replace("€", "").trim());

    page.click("button:has-text(\"€20\")");
    if (!page.locator("#payButton").isEnabled()) {
      String maxBalanceText = page.textContent("span.topup-max").split(" ")[2];
      double maxBalance = Double.parseDouble(maxBalanceText.replace("€", "").trim());
      assertTrue(maxBalance - (initialBalance + 20) < 0);
    } else {
      page.click("#payButton");
      page.waitForURL(Pattern.compile("https://(www\\.)?mollie\\.com.*"));
      assertTrue(page.url().contains("https://www.mollie.com"));

      assertEquals(
          "Test profile", page.waitForSelector(".header__merchant-name").textContent().trim());
      assertEquals(
          "W.I.S.V. 'Christiaan Huygens'",
          page.waitForSelector(".header__info-text").textContent().trim());
      assertEquals("€20.32", page.waitForSelector(".header__amount").textContent().trim());

      page.click("button[class='button--link']");
      assertTrue(page.url().contains("payment/complete"));
      assertTrue(
          page.waitForSelector("h1").innerText().contains("Payment pending")
              || page.waitForSelector("h1").innerText().contains("Payment failed"));
      var home = page.waitForSelector("a.btn.btn-large.btn-indigo[href='/']");
      assertEquals("Payment failed :(", page.waitForSelector("h1").innerText());
      home.click();
      page.waitForURL("**/index*");
      page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Balance")).click();
      String newBalanceText = page.textContent("p.balance-label span").trim();
      double actualNewBalance = Double.parseDouble(newBalanceText.replace("€", "").trim());
      assertEquals(String.format("%.2f", initialBalance), String.format("%.2f", actualNewBalance));
    }
  }

  @Test
  void testBalanceNeg() {
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

    Locator maxBalanceLabel = container.locator(".topup-max");
    assertTrue(
        maxBalanceLabel.textContent().contains("Max Balance"),
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
    assertTrue(
        page.waitForSelector("span[class='transaction-fees']")
            .innerText()
            .contains("Transaction fees of"));
    page.fill("#customAmount", "-5.00");
    assertFalse(page.locator("#payButton").isEnabled());
  }
}
