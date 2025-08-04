package chpay.frontend.admin.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class AdminUserPageTest {

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
  void UserListPageTest() {

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

    // Look for the Users button
    var usersButton = page.locator(".sidebar-nav a:has-text('Users')");
    assertTrue(usersButton.isVisible(), "Users button should be visible");
    assertTrue(usersButton.innerText().contains("Users"));

    usersButton.click();

    // Wait for users page to load
    page.waitForURL(url -> url.contains("/admin/users"));

    // Verify page title
    var pageTitle = page.locator(".transactions-title");
    assertTrue(pageTitle.isVisible(), "Page title should be visible");
    assertEquals("Users", pageTitle.innerText().trim(), "Page should have the correct title");

    // Verify filter dropdown is present and has correct options
    var filterDropdown = page.locator("#fieldFilter");
    assertTrue(filterDropdown.isVisible(), "Filter dropdown should be visible");

    var balanceOption = page.locator("#fieldFilter option[value='balance']");
    var emailOption = page.locator("#fieldFilter option[value='email']");
    var nameOption = page.locator("#fieldFilter option[value='name']");

    assertEquals(1, balanceOption.count(), "Balance filter option should exist");
    assertEquals(1, emailOption.count(), "Email filter option should exist");
    assertEquals(1, nameOption.count(), "Name filter option should exist");

    // Test Balance filter (default)
    assertEquals(
        "balance", filterDropdown.inputValue(), "Balance filter should be selected by default");

    var balanceFilterSection = page.locator("#fieldBalance");
    assertTrue(
        balanceFilterSection.isVisible(), "Balance filter section should be visible by default");

    var startBalanceField = page.locator("#startBalance");
    var endBalanceField = page.locator("#endBalance");
    assertTrue(startBalanceField.isVisible(), "Start balance field should be visible");
    assertTrue(endBalanceField.isVisible(), "End balance field should be visible");
    assertEquals(
        "number", startBalanceField.getAttribute("type"), "Start balance should be number input");
    assertEquals(
        "number", endBalanceField.getAttribute("type"), "End balance should be number input");

    // Test switching to Email filter
    filterDropdown.selectOption("email");

    var emailFilterSection = page.locator("#fieldEmail");
    assertTrue(
        emailFilterSection.isVisible(), "Email filter section should be visible when selected");
    assertFalse(balanceFilterSection.isVisible(), "Balance filter section should be hidden");

    var emailSearchField = page.locator("#usersEmailSearch");
    assertTrue(emailSearchField.isVisible(), "Email search field should be visible");
    assertEquals(
        "User Email",
        emailSearchField.getAttribute("placeholder"),
        "Email field should have correct placeholder");

    // Test switching to Name filter
    filterDropdown.selectOption("name");

    var nameFilterSection = page.locator("#fieldName");
    assertTrue(
        nameFilterSection.isVisible(), "Name filter section should be visible when selected");
    assertFalse(emailFilterSection.isVisible(), "Email filter section should be hidden");

    var nameSearchField = page.locator("#usersNameSearch");
    assertTrue(nameSearchField.isVisible(), "Name search field should be visible");
    assertEquals(
        "User Name",
        nameSearchField.getAttribute("placeholder"),
        "Name field should have correct placeholder");

    // Search for "playwright" user by name
    nameSearchField.fill("playwright");

    var searchButton = page.locator("#searchButt");
    assertTrue(searchButton.isVisible(), "Search button should be visible");
    assertEquals(
        "Search", searchButton.innerText().trim(), "Search button should have correct text");

    searchButton.click();

    // Wait for search results to load and verify we can find the playwright user
    page.waitForTimeout(1000); // Allow time for search to complete

    // Verify table structure
    var tableHeader = page.locator(".table-header");
    assertTrue(tableHeader.isVisible(), "Table header should be visible");

    var headerCells = page.locator(".table-header .table-cell");
    assertEquals(5, headerCells.count(), "Should have 5 header columns");
    assertEquals("OpenID", headerCells.nth(0).innerText().trim());
    assertEquals("Email", headerCells.nth(1).innerText().trim());
    assertEquals("Name", headerCells.nth(2).innerText().trim());
    assertEquals("Balance", headerCells.nth(3).innerText().trim());

    // Look for the playwright user in the table
    var tableRows = page.locator(".table-row");
    assertTrue(tableRows.count() > 0, "Should have at least one user row");

    // Find the playwright user row by checking if any row contains "playwright" text
    var playwrightUserFound = false;
    com.microsoft.playwright.Locator playwrightViewButton = null;
    for (int i = 0; i < tableRows.count(); i++) {
      var row = tableRows.nth(i);
      var rowText = row.innerText().toLowerCase();
      if (rowText.contains("playwright")) {
        playwrightUserFound = true;

        // Verify the row has the expected structure
        var userCells = row.locator(".table-cell");
        assertTrue(userCells.count() >= 4, "User row should have at least 4 cells");

        // Verify View button is present
        playwrightViewButton = row.locator(".receipt-button");
        assertTrue(
            playwrightViewButton.isVisible(), "View button should be visible for playwright user");
        assertEquals(
            "View", playwrightViewButton.innerText().trim(), "Button should have correct text");
        break;
      }
    }

    assertTrue(playwrightUserFound, "Should find playwright user in the search results");

    // Test the View button functionality
    assertNotNull(playwrightViewButton, "Playwright view button should have been found");
    playwrightViewButton.click();

    // Wait for navigation to user detail page
    page.waitForURL(url -> url.contains("/admin/users/"));
    assertTrue(page.url().contains("/admin/users/"), "Should navigate to user detail page");

    // Verify User Info section
    var userInfoCard = page.locator(".card-user").first();
    assertTrue(userInfoCard.isVisible(), "User info card should be visible");

    var userInfoHeader = page.locator(".card-header-user h1").first();
    assertTrue(userInfoHeader.isVisible(), "User info header should be visible");
    assertEquals("User Info", userInfoHeader.innerText().trim(), "Should have correct header text");

    // Verify user information fields
    var userInfoSections = page.locator(".user-info");
    assertTrue(userInfoSections.count() >= 4, "Should have at least 4 user info sections");

    // Check ID field - find the section that contains "ID:" in its description
    var idFound = false;
    for (int i = 0; i < userInfoSections.count(); i++) {
      var section = userInfoSections.nth(i);
      var descText = section.locator(".user-info-desc p").innerText();
      if (descText.contains("ID:")) {
        idFound = true;
        var idValue = section.locator("p").last().innerText();
        assertTrue(idValue.length() > 0, "ID should have a value");
        assertTrue(idValue.matches("^[0-9a-fA-F-]+$"), "ID should be in UUID format");
        break;
      }
    }
    assertTrue(idFound, "Should find ID field");

    // Check Name field
    var nameFound = false;
    for (int i = 0; i < userInfoSections.count(); i++) {
      var section = userInfoSections.nth(i);
      var descText = section.locator(".user-info-desc p").innerText();
      if (descText.contains("Name:")) {
        nameFound = true;
        var nameValue = section.locator("p").last().innerText();
        assertTrue(
            nameValue.toLowerCase().contains("playwright"), "Name should contain 'playwright'");
        break;
      }
    }
    assertTrue(nameFound, "Should find Name field");

    // Check OpenID field
    var openIdFound = false;
    for (int i = 0; i < userInfoSections.count(); i++) {
      var section = userInfoSections.nth(i);
      var descText = section.locator(".user-info-desc p").innerText();
      if (descText.contains("OpenID:")) {
        openIdFound = true;
        var openIdValue = section.locator("p").last().innerText();
        assertEquals("playwright", openIdValue, "OpenID should be 'playwright'");
        break;
      }
    }
    assertTrue(openIdFound, "Should find OpenID field");

    // Check Email field
    var emailFound = false;
    for (int i = 0; i < userInfoSections.count(); i++) {
      var section = userInfoSections.nth(i);
      var descText = section.locator(".user-info-desc p").innerText();
      if (descText.contains("Email:")) {
        emailFound = true;
        var emailValue = section.locator("p").last().innerText();
        assertTrue(emailValue.contains("@"), "Email should contain @ symbol");
        assertTrue(
            emailValue.contains("playwright") || emailValue.contains("test"),
            "Email should be related to playwright user");
        break;
      }
    }
    assertTrue(emailFound, "Should find Email field");

    // Verify Financial Overview section
    var financialCard = page.locator(".card-user").last();
    assertTrue(financialCard.isVisible(), "Financial overview card should be visible");

    var financialHeader = page.locator(".card-header-user h1").last();
    assertTrue(financialHeader.isVisible(), "Financial overview header should be visible");
    assertEquals(
        "Financial Overview",
        financialHeader.innerText().trim(),
        "Should have correct financial header text");

    // Check balance display
    var balanceSection = page.locator(".user-info-balance");
    assertTrue(balanceSection.isVisible(), "Balance section should be visible");

    var balanceLabel = balanceSection.locator("p").first();
    assertEquals("Balance:", balanceLabel.innerText().trim(), "Should have balance label");

    var balanceValue = balanceSection.locator("p").last();
    assertTrue(balanceValue.isVisible(), "Balance value should be visible");
    assertTrue(balanceValue.innerText().contains("EUR"), "Balance should be in EUR");
    assertTrue(
        balanceValue.innerText().matches(".*\\d+\\.\\d{2}EUR.*"),
        "Balance should be in correct format");

    // Verify user action buttons
    var userActions = page.locator(".user-actions");
    assertTrue(userActions.isVisible(), "User actions section should be visible");

    var actionButtons = userActions.locator(".user-action-button");
    assertEquals(4, actionButtons.count(), "Should have 3 action buttons");

    // Check View Transactions button
    var viewTransactionsButton = actionButtons.first();
    assertTrue(viewTransactionsButton.isVisible(), "View Transactions button should be visible");
    assertEquals(
        "View Transactions",
        viewTransactionsButton.innerText().trim(),
        "First button should be View Transactions");

    // Check Statistic User button
    var changeRFIDButton = actionButtons.all().get(1);
    assertTrue(changeRFIDButton.isVisible(), "Change RFID button should be visible");
    assertEquals(
        "Change RFID", changeRFIDButton.innerText().trim(), "Second button should be change RFID");

    // Check Statistic User button
    var statUserButton = actionButtons.all().get(2);
    assertTrue(statUserButton.isVisible(), "Stat User button should be visible");
    assertEquals(
        "View Statistics",
        statUserButton.innerText().trim(),
        "Third button should be Statistics User");

    // Check Ban User button
    var banUserButton = actionButtons.last();
    assertTrue(banUserButton.isVisible(), "Ban User button should be visible");
    assertEquals("Ban User", banUserButton.innerText().trim(), "Third button should be Ban User");

    // Test View Transactions button functionality
    viewTransactionsButton.click();

    // Wait for navigation to transactions page with user filter
    page.waitForURL(
        url -> url.contains("/admin/transactions") && url.contains("userOpenIdQuery=playwright"));
    assertTrue(page.url().contains("/admin/transactions"), "Should navigate to transactions page");
    assertTrue(
        page.url().contains("userOpenIdQuery=playwright"),
        "Should filter transactions for playwright user");

    // Verify we're on the transactions page and it's filtered for the playwright user
    var transactionsTitle = page.locator(".transactions-title");
    assertTrue(transactionsTitle.isVisible(), "Transactions page title should be visible");
    assertTrue(
        transactionsTitle.innerText().contains("Transactions"), "Should be on transactions page");

    page.navigate(baseUrl + "/admin/users");

    // Test pagination controls
    var prevButton = page.locator("#prevButt");
    var nextButton = page.locator("#nextButt");

    assertTrue(prevButton.isVisible(), "Previous button should be visible");
    assertTrue(nextButton.isVisible(), "Next button should be visible");
    assertEquals("←", prevButton.innerText().trim(), "Previous button should have correct text");
    assertEquals("→", nextButton.innerText().trim(), "Next button should have correct text");

    // Test resetting filter back to balance
    filterDropdown.selectOption("balance");
    assertTrue(balanceFilterSection.isVisible(), "Should be able to switch back to balance filter");
    assertFalse(nameFilterSection.isVisible(), "Name filter section should be hidden again");

    // Test balance filter with values
    startBalanceField.fill("50");
    endBalanceField.fill("150");
    searchButton.click();

    // Allow time for search to complete
    page.waitForTimeout(1000);

    // Verify that balance filter search was executed (page should still be functional)
    assertTrue(searchButton.isVisible(), "Page should remain functional after balance search");
  }
}
