package chpay.frontend.customer.pages;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("playwright")
public class TransactionTest {

  @Value("${spring.application.base-url}")
  private String baseUrl;

  static Playwright pw;
  static Browser browser;

  BrowserContext context;
  Page page;

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

  private List<String> getProductEventValues() {
    goToFirstPageIfNeeded();
    List<String> allValues = new ArrayList<>();
    do {
      Locator rows = page.locator(".transactions-table .table-row");
      for (int i = 0; i < rows.count(); i++) {
        Locator cell = rows.nth(i).locator(".table-pairs:nth-child(1) .table-cell:nth-child(2)");
        if (cell.count() > 0) {
          allValues.add(cell.textContent().trim());
        }
      }
    } while (goToNextPageIfExists());
    return allValues;
  }

  private List<String> getProductEventDates() {
    goToFirstPageIfNeeded();
    List<String> allValues = new ArrayList<>();
    do {
      Locator rows = page.locator(".transactions-table .table-row");
      for (int i = 0; i < rows.count(); i++) {
        Locator cell = rows.nth(i).locator(".table-pairs:nth-child(1) .table-cell:nth-child(1)");
        if (cell.count() > 0) {
          allValues.add(cell.textContent().trim());
        }
      }
    } while (goToNextPageIfExists());
    return allValues;
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
    page.navigate(baseUrl);
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @Test
  void testTopUpFilter() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");

    assertTrue(
        page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
    assertEquals("Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    assertTrue(
        page.locator("h2.transactions-title").isVisible(), "Transactions header should be visible");
    assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
    // Assert footer exists
    Locator footer = page.locator("footer.app-footer");

    // Click 'Cookie Policy' and verify modal shows
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
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
    assertTrue(page.locator("#descCell").isVisible(), "Description header cell should be visible");
    assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");

    page.locator(".transactions-table .table-row")
        .first()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    int rowCount = page.locator(".transactions-table .table-row").count();
    assertNotEquals(0, rowCount);
    page.click("button[id='topUpButt']");
    goToFirstPageIfNeeded();
    List<String> vals = getProductEventValues();
    assertFalse(vals.contains("Mollie Topup"));
  }

  @Test
  void testDescriptionFilterDesc() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");

    assertTrue(
        page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
    assertEquals("Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    assertTrue(
        page.locator("h2.transactions-title").isVisible(), "Transactions header should be visible");
    assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
    // Assert footer exists
    Locator footer = page.locator("footer.app-footer");

    // Click 'Cookie Policy' and verify modal shows
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
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
    assertTrue(page.locator("#descCell").isVisible(), "Description header cell should be visible");
    assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");

    page.locator(".transactions-table .table-row")
        .first()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    List<String> initialValues = getProductEventValues();
    assertTrue(initialValues.size() > 1);
    var descCell = page.locator("#descCell");
    descCell.click();
    page.waitForURL(url -> url.contains("&sortBy=description&order=desc"));
    assertTrue(
        descCell.textContent().contains("↓"),
        "Should display downward arrow for descending description");
    List<String> expectedSortedValues = new ArrayList<>(initialValues);
    Collections.sort(expectedSortedValues);
    Collections.reverse(expectedSortedValues);
    goToFirstPageIfNeeded();
    List<String> valuesAfterSort = getProductEventValues();
    assertEquals(expectedSortedValues, valuesAfterSort);
  }

  @Test
  void testDescriptionFilterAsc() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");

    assertTrue(
        page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
    assertEquals("Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    assertTrue(
        page.locator("h2.transactions-title").isVisible(), "Transactions header should be visible");
    assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
    // Assert footer exists
    Locator footer = page.locator("footer.app-footer");

    // Click 'Cookie Policy' and verify modal shows
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
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
    assertTrue(page.locator("#descCell").isVisible(), "Description header cell should be visible");
    assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");

    page.locator(".transactions-table .table-row")
        .first()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    List<String> initialValues = getProductEventValues();
    assertTrue(initialValues.size() > 1);
    var descCell = page.locator("#descCell");
    descCell.click();
    page.waitForURL(url -> url.contains("&sortBy=description&order=desc"));
    assertTrue(
        descCell.textContent().contains("↓"),
        "Should display downward arrow for descending description");
    descCell.click();
    page.waitForURL(url -> url.contains("&sortBy=description&order=asc"));
    assertTrue(
        descCell.textContent().contains("↑"),
        "Should display downward arrow for ascending description");
    List<String> expectedSortedValues = new ArrayList<>(initialValues);
    Collections.sort(expectedSortedValues);
    goToFirstPageIfNeeded();
    List<String> valuesAfterSort = getProductEventValues();
    assertEquals(expectedSortedValues, valuesAfterSort);
  }

  @Test
  void testDateFilterDesc() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");

    assertTrue(
        page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
    assertEquals("Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    assertTrue(
        page.locator("h2.transactions-title").isVisible(), "Transactions header should be visible");
    assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
    // Assert footer exists
    Locator footer = page.locator("footer.app-footer");

    // Click 'Cookie Policy' and verify modal shows
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
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
    assertTrue(page.locator("#descCell").isVisible(), "Description header cell should be visible");
    assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");

    page.locator(".transactions-table .table-row")
        .first()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    List<String> initialValues = getProductEventDates();
    assertTrue(initialValues.size() > 1);
    var dateCell = page.locator("#dateCell");
    dateCell.click();
    page.waitForURL(url -> url.contains("&sortBy=timestamp&order=asc"));
    assertTrue(
        dateCell.textContent().contains("↑"), "Should display upward arrow for ascending date");
    dateCell.click();
    page.waitForURL(url -> url.contains("&sortBy=timestamp&order=desc"));
    assertTrue(
        dateCell.textContent().contains("↓"), "Should display downward arrow for descending date");
    List<String> expectedSortedValues = new ArrayList<>(initialValues);
    SimpleDateFormat formatter =
        new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH); // Use Locale for month names

    expectedSortedValues.sort(
        (dateString1, dateString2) -> {
          try {
            Date date1 = formatter.parse(dateString1);
            Date date2 = formatter.parse(dateString2);
            return date2.compareTo(date1);
          } catch (ParseException e) {
            System.err.println("Error parsing date string: " + e.getMessage());
          }
          return 0;
        });
    goToFirstPageIfNeeded();
    List<String> valuesAfterSort = getProductEventDates();
    assertEquals(expectedSortedValues, valuesAfterSort);
  }

  @Test
  void testDateFilterAsc() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");

    assertTrue(
        page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
    assertEquals("Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    assertTrue(
        page.locator("h2.transactions-title").isVisible(), "Transactions header should be visible");
    assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
    // Assert footer exists
    Locator footer = page.locator("footer.app-footer");

    // Click 'Cookie Policy' and verify modal shows
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
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
    assertTrue(page.locator("#descCell").isVisible(), "Description header cell should be visible");
    assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");

    page.locator(".transactions-table .table-row")
        .first()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    List<String> initialValues = getProductEventDates();
    assertTrue(initialValues.size() > 1);
    var dateCell = page.locator("#dateCell");
    dateCell.click();
    page.waitForURL(url -> url.contains("&sortBy=timestamp&order=asc"));
    assertTrue(
        dateCell.textContent().contains("↑"), "Should display upward arrow for ascending date");
    List<String> expectedSortedValues = new ArrayList<>(initialValues);
    SimpleDateFormat formatter =
        new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH); // Use Locale for month names

    expectedSortedValues.sort(
        (dateString1, dateString2) -> {
          try {
            Date date1 = formatter.parse(dateString1);
            Date date2 = formatter.parse(dateString2);
            return date1.compareTo(date2);
          } catch (ParseException e) {
            System.err.println("Error parsing date string: " + e.getMessage());
          }
          return 0;
        });
    goToFirstPageIfNeeded();
    List<String> valuesAfterSort = getProductEventDates();
    assertEquals(expectedSortedValues, valuesAfterSort);
  }

  @Test
  void testTotalFilterDesc() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");
    assertTrue(
        page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
    assertEquals("Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    assertTrue(
        page.locator("h2.transactions-title").isVisible(), "Transactions header should be visible");
    assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
    // Assert footer exists
    Locator footer = page.locator("footer.app-footer");

    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
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
    assertTrue(page.locator("#descCell").isVisible(), "Description header cell should be visible");
    assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");
    page.locator(".transactions-table .table-row")
        .first()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    List<Double> initialValues = getProductEventTotals();
    assertTrue(initialValues.size() > 1);
    var totalCell = page.locator("#totalCell");
    totalCell.click();
    page.waitForURL(url -> url.contains("&sortBy=amount&order=desc"));
    assertTrue(
        totalCell.textContent().contains("↓"),
        "Should display downward arrow for descending total");
    List<Double> expectedSortedValues = new ArrayList<>(initialValues);
    Collections.sort(expectedSortedValues);
    Collections.reverse(expectedSortedValues);
    goToFirstPageIfNeeded();
    List<Double> valuesAfterSort = getProductEventTotals();
    assertEquals(expectedSortedValues, valuesAfterSort);
  }

  @Test
  void testTotalFilterAsc() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Transactions")).click();
    assertEquals(page.url(), baseUrl + "/transactions");
    assertTrue(
        page.waitForSelector(".sidebar-logo img").isVisible(), "Sidebar logo must be visible");
    assertEquals("Christiaan Huygens Logo", page.locator(".sidebar-logo img").getAttribute("alt"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    assertTrue(
        page.locator("h2.transactions-title").isVisible(), "Transactions header should be visible");
    assertEquals("Past Transactions", page.locator("h2.transactions-title").textContent().trim());
    // Assert footer exists
    Locator footer = page.locator("footer.app-footer");

    // Click 'Cookie Policy' and verify modal shows
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
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
    assertTrue(page.locator("#descCell").isVisible(), "Description header cell should be visible");
    assertTrue(page.locator("#totalCell").isVisible(), "Total header cell should be visible");
    page.locator(".transactions-table .table-row")
        .first()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    List<Double> initialValues = getProductEventTotals();
    assertTrue(initialValues.size() > 1);
    var totalCell = page.locator("#totalCell");
    totalCell.click();
    page.waitForURL(url -> url.contains("&sortBy=amount&order=desc"));
    assertTrue(
        totalCell.textContent().contains("↓"),
        "Should display downward arrow for descending total");
    totalCell.click();
    page.waitForURL(url -> url.contains("&sortBy=amount&order=asc"));
    assertTrue(
        totalCell.textContent().contains("↑"), "Should display upward arrow for ascending total");
    List<Double> expectedSortedValues = new ArrayList<>(initialValues);
    Collections.sort(expectedSortedValues);
    goToFirstPageIfNeeded();
    List<Double> valuesAfterSort = getProductEventTotals();
    assertEquals(expectedSortedValues, valuesAfterSort);
  }
}
