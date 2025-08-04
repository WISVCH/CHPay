package chpay.frontend.admin.pages;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("playwright")
public class AdminStatisticsPageTest {

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
    // Login as admin
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void testStatisticsPageLoads() {
    page.navigate(baseUrl + "/admin/stats/balance");

    // Check that the page title is correct
    assertEquals("CHpay - Admin Panel", page.title());

    // Verify the main content area exists
    Locator statContent = page.locator(".stat-content");
    assertTrue(statContent.isVisible(), "Statistics content should be visible");
  }

  @Test
  void testStatisticsPageHeader() {
    page.navigate(baseUrl + "/admin/stats/balance");

    // Wait for page to load
    page.waitForSelector(".stat-header");

    // Verify the header section
    Locator statHeader = page.locator(".stat-header");
    assertTrue(statHeader.isVisible(), "Statistics header should be visible");

    // Check the title
    Locator title = page.locator("h2.transactions-title");
    assertTrue(title.isVisible(), "Page title should be visible");
    assertEquals("Balance", title.textContent().trim(), "Default title should be Balance");

    // Check the header actions
    Locator headerActions = page.locator(".stat-header-action");
    assertTrue(headerActions.isVisible(), "Header actions should be visible");
  }

  @Test
  void testStatisticsFilterDropdown() {
    page.navigate(baseUrl + "/admin/stats/balance");

    // Wait for the dropdown to be available
    page.waitForSelector("#fieldStat");

    // Check that the statistics filter dropdown exists
    Locator statFilter = page.locator("#fieldStat");
    assertTrue(statFilter.isVisible(), "Statistics filter dropdown should be visible");

    // Verify filter options exist using count() method
    Locator balanceOption = page.locator("#fieldStat option[value='balance']");
    Locator incomingOption = page.locator("#fieldStat option[value='incoming-funds']");
    Locator outgoingOption = page.locator("#fieldStat option[value='outcoming-funds']");

    assertEquals(1, balanceOption.count(), "Balance filter option should exist");
    assertEquals(1, incomingOption.count(), "Incoming funds filter option should exist");
    assertEquals(1, outgoingOption.count(), "Outgoing funds filter option should exist");

    // Test that balance is selected by default
    assertEquals("balance", statFilter.inputValue(), "Balance should be selected by default");
  }

  @Test
  void testStatisticsFilterSelection() {
    page.navigate(baseUrl + "/admin/stats/balance");

    page.waitForSelector("#fieldStat");

    Locator statFilter = page.locator("#fieldStat");

    // Test selecting different filter options and verify URL changes
    statFilter.selectOption("incoming-funds");
    assertEquals(
        "incoming-funds", statFilter.inputValue(), "Should be able to select incoming funds");

    page.waitForURL(url -> url.contains("/admin/stats/incoming-funds"));
    assertTrue(
        page.url().contains("/admin/stats/incoming-funds"),
        "Should navigate to incoming-funds page");

    // Test outgoing funds
    statFilter.selectOption("outcoming-funds");
    assertEquals(
        "outcoming-funds", statFilter.inputValue(), "Should be able to select outgoing funds");

    page.waitForURL(url -> url.contains("/admin/stats/outcoming-funds"));
    assertTrue(
        page.url().contains("/admin/stats/outcoming-funds"),
        "Should navigate to outgoing-funds page");

    // Test back to balance
    statFilter.selectOption("balance");
    assertEquals("balance", statFilter.inputValue(), "Should be able to select balance");

    page.waitForURL(url -> url.contains("/admin/stats/balance"));
    assertTrue(page.url().contains("/admin/stats/balance"), "Should navigate to balance page");
  }

  @Test
  void testIncomingFundsStatisticsPage() {
    page.navigate(baseUrl + "/admin/stats/incoming-funds");

    // Wait for page to load
    page.waitForSelector(".stat-header");

    // Check the title changes for incoming funds
    Locator title = page.locator("h2.transactions-title");
    assertTrue(title.isVisible(), "Page title should be visible");
    // Title should reflect the selected statistic type

    // Verify the filter is set correctly
    page.waitForSelector("#fieldStat");
    Locator statFilter = page.locator("#fieldStat");
    assertEquals("incoming-funds", statFilter.inputValue(), "Incoming funds should be selected");

    // Verify main content area exists
    Locator statContent = page.locator(".stat-content");
    assertTrue(statContent.isVisible(), "Statistics content should be visible");
  }

  @Test
  void testOutgoingFundsStatisticsPage() {
    page.navigate(baseUrl + "/admin/stats/outcoming-funds");

    // Wait for page to load
    page.waitForSelector(".stat-header");

    // Verify the filter is set correctly
    page.waitForSelector("#fieldStat");
    Locator statFilter = page.locator("#fieldStat");
    assertEquals("outcoming-funds", statFilter.inputValue(), "Outgoing funds should be selected");

    // Verify main content area exists
    Locator statContent = page.locator(".stat-content");
    assertTrue(statContent.isVisible(), "Statistics content should be visible");
  }

  @Test
  void testStatisticsInfoSection() {
    page.navigate(baseUrl + "/admin/stats/balance");

    // Wait for statistics info to load
    page.waitForSelector(".stat-info");

    // Verify statistics info section
    Locator statInfo = page.locator(".stat-info");
    assertTrue(statInfo.isVisible(), "Statistics info section should be visible");

    // Check statistics info text section
    Locator statInfoText = page.locator(".stat-info-text");
    assertTrue(statInfoText.isVisible(), "Statistics info text should be visible");

    // Verify that statistics values are displayed (don't check exact values since data is mocked)
    Locator mainAmount = statInfoText.locator("p").first();
    assertTrue(mainAmount.isVisible(), "Main amount should be visible");
    assertTrue(mainAmount.textContent().contains("EUR"), "Amount should contain EUR currency");

    Locator averageAmount = statInfoText.locator("p").last();
    assertTrue(averageAmount.isVisible(), "Average amount should be visible");
    assertTrue(
        averageAmount.textContent().contains("Average"), "Should display average information");
  }

  @Test
  void testTimePeriodSelector() {
    page.navigate(baseUrl + "/admin/stats/balance");

    page.waitForSelector("#fieldTime");

    // Check time period selector
    Locator timeSelector = page.locator("#fieldTime");
    assertTrue(timeSelector.isVisible(), "Time period selector should be visible");

    // Verify time period options exist
    Locator oneWeek = page.locator("#fieldTime option[value='7']");
    Locator oneMonth = page.locator("#fieldTime option[value='30']");
    Locator threeMonths = page.locator("#fieldTime option[value='90']");
    Locator sixMonths = page.locator("#fieldTime option[value='180']");
    Locator nineMonths = page.locator("#fieldTime option[value='210']");
    Locator oneYear = page.locator("#fieldTime option[value='365']");
    Locator threeYears = page.locator("#fieldTime option[value='1096']");

    assertEquals(1, oneWeek.count(), "1 Week option should exist");
    assertEquals(1, oneMonth.count(), "1 Month option should exist");
    assertEquals(1, threeMonths.count(), "3 Month option should exist");
    assertEquals(1, sixMonths.count(), "6 Month option should exist");
    assertEquals(1, nineMonths.count(), "9 Month option should exist");
    assertEquals(1, oneYear.count(), "1 Year option should exist");
    assertEquals(1, threeYears.count(), "3 Years option should exist");

    // Test selecting different time periods
    timeSelector.selectOption("30");
    assertEquals("30", timeSelector.inputValue(), "Should be able to select 1 month");

    timeSelector.selectOption("365");
    assertEquals("365", timeSelector.inputValue(), "Should be able to select 1 year");
  }

  @Test
  void testDownloadCSVButton() {
    page.navigate(baseUrl + "/admin/stats/balance");

    page.waitForSelector("#downloadLink");

    // Check CSV download button
    Locator downloadButton = page.locator("#downloadLink");
    assertTrue(downloadButton.isVisible(), "Download CSV button should be visible");
    assertEquals(
        "Download CSV", downloadButton.textContent().trim(), "Button should have correct text");

    // Verify it has download attribute
    assertTrue(
        downloadButton.getAttribute("download") != null, "Button should have download attribute");
    assertTrue(
        downloadButton.getAttribute("download").contains(".csv"), "Download should be CSV file");

    // Verify it has href attribute
    assertTrue(downloadButton.getAttribute("href") != null, "Button should have href attribute");
  }

  @Test
  void testChartCanvas() {
    page.navigate(baseUrl + "/admin/stats/balance");

    // Wait for chart to load
    page.waitForSelector("#canvasWrap");

    // Check that chart canvas exists
    Locator canvasWrap = page.locator("#canvasWrap");
    assertTrue(canvasWrap.isVisible(), "Chart canvas wrapper should be visible");
    assertTrue(
        canvasWrap.getAttribute("class").contains("stat-canvas"), "Should have stat-canvas class");

    // Check that canvas element exists
    Locator canvas = page.locator("#balance");
    assertTrue(canvas.isVisible(), "Chart canvas should be visible");
    assertEquals(
        "canvas", canvas.evaluate("el => el.tagName.toLowerCase()"), "Should be a canvas element");
  }

  @Test
  void testSidebarNavigationToStatistics() {
    page.navigate(baseUrl + "/admin");

    // Wait for sidebar to load
    page.waitForSelector("aside.sidebar");

    // Look for the Statistics button in sidebar
    Locator statisticsButton = page.locator(".sidebar-nav a:has-text('Statistics')");
    assertTrue(statisticsButton.isVisible(), "Statistics button should be visible in sidebar");
    statisticsButton.click();
    page.waitForURL(url -> url.contains("/admin/stats/balance"));

    // Button is dimmed when we're on the page
    assertTrue(
        statisticsButton.getAttribute("style").contains("opacity: 60%"),
        "Statistics button should be disabled/dimmed");
    assertEquals(
        "#",
        statisticsButton.getAttribute("href"),
        "Statistics button should have placeholder href");
  }

  @Test
  void testPageResponsiveness() {
    page.navigate(baseUrl + "/admin/stats/balance");

    // Test desktop view
    page.setViewportSize(1920, 1080);
    page.waitForSelector(".stat-content");
    assertTrue(
        page.locator(".stat-content").isVisible(), "Statistics should be visible on desktop");

    // Test tablet view
    page.setViewportSize(768, 1024);
    assertTrue(page.locator(".stat-content").isVisible(), "Statistics should be visible on tablet");

    // Test mobile view
    page.setViewportSize(375, 667);
    assertTrue(page.locator(".stat-content").isVisible(), "Statistics should be visible on mobile");
  }

  @Test
  void testStatisticsPageScripts() {
    page.navigate(baseUrl + "/admin/stats/balance");

    // Wait for page to load
    page.waitForSelector(".stat-content");

    // Verify that Chart.js is loaded (check if Chart object exists)
    Boolean chartJsLoaded = (Boolean) page.evaluate("() => typeof Chart !== 'undefined'");
    assertTrue(chartJsLoaded, "Chart.js should be loaded");

    // Verify that statistics data is available
    Boolean statsDataExists = (Boolean) page.evaluate("() => typeof stats !== 'undefined'");
    assertTrue(statsDataExists, "Statistics data should be available");
  }

  @Test
  void testStatisticsFilterNavigation() {
    // Start at balance page
    page.navigate(baseUrl + "/admin/stats/balance");
    page.waitForSelector("#fieldStat");

    assertEquals(
        "balance", page.locator("#fieldStat").inputValue(), "Should start on balance page");

    // Navigate to incoming funds
    page.locator("#fieldStat").selectOption("incoming-funds");
    page.waitForURL(url -> url.contains("/admin/stats/incoming-funds"));

    // Verify we're on the right page and filter is correct
    assertEquals(
        "incoming-funds",
        page.locator("#fieldStat").inputValue(),
        "Should be on incoming funds page");

    // Navigate to outgoing funds
    page.locator("#fieldStat").selectOption("outcoming-funds");
    page.waitForURL(url -> url.contains("/admin/stats/outcoming-funds"));

    // Verify we're on the right page and filter is correct
    assertEquals(
        "outcoming-funds",
        page.locator("#fieldStat").inputValue(),
        "Should be on outgoing funds page");

    // Navigate back to balance
    page.locator("#fieldStat").selectOption("balance");
    page.waitForURL(url -> url.contains("/admin/stats/balance"));

    // Verify we're back on balance page
    assertEquals(
        "balance", page.locator("#fieldStat").inputValue(), "Should be back on balance page");
  }
}
