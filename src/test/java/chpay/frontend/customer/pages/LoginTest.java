package chpay.frontend.customer.pages;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("playwright")
public class LoginTest {

  private final String baseUrl;

  static Playwright pw;
  static Browser browser;

  BrowserContext context;
  Page page;

  public LoginTest(@Value("${spring.application.base-url}") String baseUrl) {
    this.baseUrl = baseUrl;
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
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  /**
   * Basic authentication test, this is how we bypass the SSO. Use in all end to end test cases
   * where authentication is required.
   */
  @Test
  void basicAuthentication() {

    page.setViewportSize(1920, 1080);
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");

    page.navigate(baseUrl);

    assertFalse(page.url().contains("/login"));
    assertTrue(page.url().contains("/index"));

    // Make sure the cookie popup appears and clears when we dismiss it
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
    page.waitForSelector(
        ".cookie-text", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
    assertEquals(0, page.locator(".cookie-text").count());

    // Make sure the sidebar is visible and contains correct user info
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("playwright@test.com"));

    var sidebar = page.locator("aside.sidebar");
    assertTrue(sidebar.isVisible(), "Sidebar should be visible");

    var logo = page.locator(".sidebar-logo img");
    assertTrue(logo.isVisible(), "Logo should be visible in sidebar");
    assertEquals(
        "Christiaan Huygens Logo", logo.getAttribute("alt"), "Logo should have correct alt text");

    // Make sure sidebar navigation is visible and contains correct links
    var balanceLink = page.locator(".sidebar-nav a[href='/balance']");
    assertTrue(balanceLink.isVisible(), "Balance link should be visible");
    assertTrue(
        balanceLink.innerText().contains("Balance"), "Balance link should contain 'Balance' text");

    balanceLink.click();
    page.waitForURL(url -> url.contains("/balance"));
    assertTrue(page.url().contains("/balance"), "User should be redirected to balance page");

    page.navigate(baseUrl);

    var transactionsLink = page.locator(".sidebar-nav a[href='/transactions']");
    assertTrue(transactionsLink.isVisible(), "Transactions link should be visible");
    assertTrue(
        transactionsLink.innerText().contains("Transactions"),
        "Transactions link should contain 'Transactions' text");

    /* THIS PART IS BUGGED CURRENTLY, NEEDS FIXING IN TRANSACTION PAGE LOGIC BEFORE UNCOMMENTING
    transactionsLink.click();
    page.waitForURL(url -> url.contains("/transactions"));
    assertTrue(page.url().contains("/transactions"), "User should be redirected to transactions page");
    */
    page.navigate(baseUrl);

    var adminLink = page.locator(".sidebar-nav a[href='/admin']");
    assertTrue(adminLink.isVisible(), "Admin View link should be visible");
    assertTrue(
        adminLink.innerText().contains("Admin View"),
        "Admin View link should contain 'Admin View' text");
    assertTrue(
        adminLink.getAttribute("class").contains("sidebar-link-red"),
        "Admin View link should have red styling");

    adminLink.click();
    page.waitForURL(url -> url.contains("/admin"));
    assertTrue(page.url().contains("/admin"), "User should be redirected to admin page");

    page.navigate(baseUrl);

    var logoutButton = page.waitForSelector(".sidebar-logout button");
    assertTrue(logoutButton.isVisible(), "Logout button should be visible");
    assertTrue(
        logoutButton.innerText().contains("Log Out"),
        "Logout button should contain 'Log Out' text");

    var logoutForm = page.locator(".sidebar-logout form");
    assertEquals(
        "/logout", logoutForm.getAttribute("action"), "Logout form should have correct action URL");
    assertEquals("post", logoutForm.getAttribute("method"), "Logout form should use POST method");

    assertTrue(
        page.locator(".sidebar-nav .fa-wallet").isVisible(),
        "Wallet icon should be visible for Balance");
    assertTrue(
        page.locator(".sidebar-nav .fa-clock-rotate-left").isVisible(),
        "Clock icon should be visible for Transactions");
    assertTrue(
        page.locator(".sidebar-nav .fa-lock").isVisible(),
        "Lock icon should be visible for Admin View");
    assertTrue(
        page.locator(".sidebar-logout .fa-arrow-right-from-bracket").isVisible(),
        "Logout icon should be visible");
  }

  @Test
  void mobileNavigationAndHeaderWork() {
    // Login first using the test helper
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");
    page.navigate(baseUrl);

    // Set viewport to mobile size
    page.setViewportSize(440, 956); // mobile size

    // Get rid of the cookie popup and make sure it disappears when dismissed
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
    page.waitForSelector(
        ".cookie-text", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
    assertEquals(0, page.locator(".cookie-text").count());

    // Verify mobile header is visible
    var mobileHeader = page.locator(".main-header-mobile");
    assertTrue(mobileHeader.isVisible(), "Mobile header should be visible on small screens");

    // Check the mobile header logo
    var mobileLogo = page.locator(".main-header-mobile img");
    assertTrue(mobileLogo.isVisible(), "Logo should be visible in mobile header");
    assertEquals(
        "Christiaan Huygens Logo",
        mobileLogo.getAttribute("alt"),
        "Logo should have correct alt text");

    // Check the mobile header username
    var mobileLogout = page.locator(".mobile-logout-button");
    assertTrue(mobileLogout.isVisible(), "Mobile logout button should be visible");
    assertTrue(
        mobileLogout.innerText().contains("Log Out"), "Logout button should contain 'Log Out'");

    // Verify mobile navigation exists
    var mobileNav = page.locator(".mobile-nav");
    assertTrue(mobileNav.isVisible(), "Mobile navigation should be visible on small screens");

    // Verify mobile navigation has exactly 2 links (Balance and Transactions, no Admin View)
    assertEquals(
        2,
        page.locator(".mobile-nav a.nav-button").count(),
        "Mobile navigation should have exactly 2 links");

    // Verify Balance button in mobile nav
    var balanceLink = page.locator(".mobile-nav a[href='/balance']");
    assertTrue(balanceLink.isVisible(), "Balance link should be visible in mobile nav");
    assertTrue(
        balanceLink.innerText().contains("Balance"), "Balance link should contain 'Balance' text");
    assertTrue(
        balanceLink.locator("i.fa-wallet").isVisible(), "Balance link should have wallet icon");

    // Click Balance button and verify navigation
    balanceLink.click();
    page.waitForURL(url -> url.contains("/balance"));
    assertTrue(page.url().contains("/balance"), "User should be redirected to balance page");

    // Navigate back to home page
    page.navigate(baseUrl);

    // Set viewport back to mobile size (navigation might have changed the viewport)
    page.setViewportSize(440, 956);

    // Verify Transactions button in mobile nav
    var transactionsLink = page.locator(".mobile-nav a[href='/transactions']");
    assertTrue(transactionsLink.isVisible(), "Transactions link should be visible in mobile nav");
    assertTrue(
        transactionsLink.innerText().contains("Transactions"),
        "Transactions link should contain 'Transactions' text");
    assertTrue(
        transactionsLink.locator("i.fa-clock-rotate-left").isVisible(),
        "Transactions link should have clock icon");

    // Click Transactions button and verify navigation
    /* THIS PART IS BUGGED CURRENTLY, NEEDS FIXING IN TRANSACTION PAGE LOGIC BEFORE UNCOMMENTING
    transactionsLink.click();
    page.waitForURL(url -> url.contains("/transactions"));
    assertTrue(page.url().contains("/transactions"), "User should be redirected to transactions page");

    // Navigate back to home page
    page.navigate(baseUrl);

    // Set viewport back to mobile size
    page.setViewportSize(375, 667);*/

    // Verify Admin View button is NOT in mobile nav but IS in sidebar (which may be hidden)
    assertEquals(
        0,
        page.locator(".mobile-nav a[href='/admin']").count(),
        "Admin View link should not be in mobile nav");

    // Check that footer exists and contains correct text
    var footer = page.locator(".app-footer");
    assertFalse(footer.isVisible(), "Footer should not be visible on mobile");
    assertTrue(
        footer.innerText().contains("W.I.S.V. 'Christiaan Huygens' 2025"),
        "Footer should contain copyright text");

    // Check cookie policy link in footer
    var cookiePolicyLink = page.locator(".app-footer .cookie-link");
    assertFalse(cookiePolicyLink.isVisible(), "Cookie policy link should not be visible in footer");
    assertTrue(
        cookiePolicyLink.innerText().contains("Cookie Policy"),
        "Link should contain 'Cookie Policy' text");
  }

  @Test
  void logoutFunctionalityDesktop() {
    // Login first using the test helper
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");
    page.navigate(baseUrl);

    // Set viewport to desktop size to ensure we're testing desktop view
    page.setViewportSize(1920, 1080);

    // Verify we're logged in
    assertTrue(page.waitForSelector(".sidebar-user").innerText().contains("test playwright"));

    // Find and click the logout button in the sidebar
    var logoutButton = page.waitForSelector(".sidebar-logout button");
    assertTrue(logoutButton.isVisible(), "Logout button should be visible");

    // Click the logout button
    logoutButton.click();

    // Wait for navigation to complete after logout
    page.waitForURL(url -> url.contains("/logout-success"));

    // Verify we're on the logout success page
    assertTrue(
        page.url().contains("/logout-success"), "User should be redirected to logout success page");

    // Get rid of the cookie popup and make sure it disappears when dismissed
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
    page.waitForSelector(
        ".cookie-text", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
    assertEquals(0, page.locator(".cookie-text").count());

    // Verify logout page elements
    var logoutBox = page.locator(".box");
    assertTrue(logoutBox.isVisible(), "Logout box should be visible");

    var logoutTitle = page.locator(".logout-title");
    assertTrue(logoutTitle.isVisible(), "Logout title should be visible");
    assertEquals("Logged Out", logoutTitle.innerText(), "Logout title should be 'Logged Out'");

    var successMessage = page.locator(".success-message");
    assertTrue(successMessage.isVisible(), "Success message should be visible");
    assertTrue(
        successMessage.innerText().contains("You have been successfully logged out"),
        "Success message should indicate successful logout");

    var logoutText = page.locator(".logout-text");
    assertTrue(logoutText.isVisible(), "Logout text should be visible");
    assertTrue(
        logoutText.innerText().contains("Thank you for using our service"),
        "Logout text should contain thank you message");

    var returnToLoginButton = page.locator(".basic-button");
    assertTrue(returnToLoginButton.isVisible(), "Return to login button should be visible");
    assertEquals(
        "Return to Login",
        returnToLoginButton.innerText().trim(),
        "Button should have text 'Return to Login'");
    assertEquals(
        "/login", returnToLoginButton.getAttribute("href"), "Button should link to login page");

    // Verify footer is present on logout page
    var footer = page.locator(".app-footer");
    assertTrue(footer.isVisible(), "Footer should be visible on logout page");
    assertTrue(
        footer.innerText().contains("© W.I.S.V. 'Christiaan Huygens' 2025"),
        "Footer should contain copyright text");

    // Click the return to login button and verify navigation
    returnToLoginButton.click();
    page.waitForURL(url -> url.contains("/login"));
    assertTrue(page.url().contains("/login"), "User should be redirected to login page");
  }

  @Test
  void SSOTestDesktop() {
    page.setViewportSize(1920, 1080);

    // Navigate to the base URL, which should redirect to login page if not authenticated
    page.navigate(baseUrl);

    // Verify we're redirected to the login page
    assertTrue(page.url().contains("/login"), "User should be redirected to login page");

    // Get rid of the cookie popup and make sure it disappears when dismissed
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
    page.waitForSelector(
        ".cookie-text", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
    assertEquals(0, page.locator(".cookie-text").count());

    // Verify the login page title
    var loginTitle = page.locator(".login-title");
    assertTrue(loginTitle.isVisible(), "Login title should be visible");
    assertEquals(
        "Welcome to CHPay", loginTitle.innerText(), "Login title should be 'Welcome to CHPay'");

    // Verify the login container exists
    var loginContainer = page.locator(".page-container");
    assertTrue(loginContainer.isVisible(), "Login container should be visible");

    // Verify the login button with CHConnect
    var loginButton = page.locator(".basic-button");
    assertTrue(loginButton.isVisible(), "Login button should be visible");
    assertEquals(
        "Login with CHConnect",
        loginButton.innerText().trim(),
        "Button should have text 'Login with CHConnect'");
    assertEquals(
        "/oauth2/authorization/wisvchconnect",
        loginButton.getAttribute("href"),
        "Button should link to OAuth2 authorization endpoint");

    // Verify the cookie policy footer is present
    var footer = page.locator(".app-footer");
    assertTrue(footer.isVisible(), "Footer should be visible on login page");
    assertTrue(
        footer.innerText().contains("© W.I.S.V. 'Christiaan Huygens' 2025"),
        "Footer should contain copyright text");

    var cookiePolicyLink = page.locator(".app-footer .cookie-link");
    assertTrue(cookiePolicyLink.isVisible(), "Cookie policy link should be visible");
    assertTrue(
        cookiePolicyLink.innerText().contains("Cookie Policy"),
        "Link should contain 'Cookie Policy' text");

    // Test cookie policy modal
    cookiePolicyLink.click();
    var cookiePolicyModal = page.locator("#cookie-policy-modal");
    assertTrue(
        cookiePolicyModal.isVisible(), "Cookie policy modal should be visible after clicking link");

    var policyBox = page.locator(".policy-box");
    assertTrue(policyBox.isVisible(), "Policy box should be visible");
    assertTrue(policyBox.innerText().contains("Cookie Policy"), "Policy box should contain title");
    assertTrue(
        policyBox.innerText().contains("JSESSIONID"), "Policy should mention JSESSIONID cookie");
    assertTrue(
        policyBox.innerText().contains("cookie_consent"),
        "Policy should mention cookie_consent cookie");

    // Close the cookie policy modal
    page.click(".cookie-button");
    assertFalse(
        page.locator("#cookie-policy-modal").getAttribute("class").contains("show"),
        "Cookie policy modal should be hidden after clicking close");

    // Click the login button to initiate SSO login
    loginButton.click();

    // Wait for redirect to CH Connect page
    page.waitForURL(url -> url.contains("connect.ch.tudelft.nl"));

    // Verify we're on the CH Connect login page
    assertTrue(
        page.url().contains("connect.ch.tudelft.nl"),
        "User should be redirected to CH Connect SSO page");

    // Verify page title
    assertEquals("CH Connect - Log In", page.title(), "SSO page should have correct title");

    // Verify CH logo is present
    var chLogo = page.locator(".login-box img[alt='CH Logo']");
    assertTrue(chLogo.isVisible(), "CH Logo should be visible on SSO page");

    // Verify login options heading
    var loginHeading = page.locator(".login-box h4");
    assertTrue(loginHeading.isVisible(), "Login heading should be visible");
    assertEquals("Log in using", loginHeading.innerText(), "Heading should say 'Log in using'");
  }

  @Test
  void logoutFunctionalityMobile() {
    // Login first using the test helper
    page.navigate(baseUrl + "/test/login-get?username=playwright&roles=ADMIN,USER");
    page.navigate(baseUrl);

    // Set viewport to mobile size to ensure we're testing mobile view
    page.setViewportSize(440, 956);

    // Get rid of the cookie popup and make sure it disappears when dismissed
    assertTrue(page.waitForSelector(".cookie-text").innerText().contains("We use cookies"));
    page.click(".cookie-button");
    page.waitForSelector(
        ".cookie-text", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
    assertEquals(0, page.locator(".cookie-text").count());

    // Verify we're logged in by checking mobile header is visible
    var mobileHeader = page.locator(".main-header-mobile");
    assertTrue(mobileHeader.isVisible(), "Mobile header should be visible on small screens");

    // Find and verify the mobile logout button
    var mobileLogoutButton = page.locator(".mobile-logout-button");
    assertTrue(mobileLogoutButton.isVisible(), "Mobile logout button should be visible");
    assertTrue(
        mobileLogoutButton.innerText().contains("Log Out"),
        "Mobile logout button should contain 'Log Out' text");

    // Verify the mobile logout form has correct attributes
    var mobileLogoutForm = page.locator(".mobile-logout-form");
    assertTrue(mobileLogoutForm.isVisible(), "Mobile logout form should be visible");
    assertEquals(
        "/logout",
        mobileLogoutForm.getAttribute("action"),
        "Mobile logout form should have correct action URL");
    assertEquals(
        "post",
        mobileLogoutForm.getAttribute("method"),
        "Mobile logout form should use POST method");

    // Verify the logout icon is present in mobile button
    assertTrue(
        mobileLogoutButton.locator("i.fa-arrow-right-from-bracket").isVisible(),
        "Mobile logout button should have logout icon");

    // Click the mobile logout button
    mobileLogoutButton.click();

    // Wait for navigation to complete after logout
    page.waitForURL(url -> url.contains("/logout-success"));

    // Verify we're on the logout success page
    assertTrue(
        page.url().contains("/logout-success"),
        "User should be redirected to logout success page after mobile logout");

    // Verify logout page elements (same as desktop, should work on mobile too)
    var logoutBox = page.locator(".box");
    assertTrue(logoutBox.isVisible(), "Logout box should be visible on mobile");

    var logoutTitle = page.locator(".logout-title");
    assertTrue(logoutTitle.isVisible(), "Logout title should be visible on mobile");
    assertEquals("Logged Out", logoutTitle.innerText(), "Logout title should be 'Logged Out'");

    var successMessage = page.locator(".success-message");
    assertTrue(successMessage.isVisible(), "Success message should be visible on mobile");
    assertTrue(
        successMessage.innerText().contains("You have been successfully logged out"),
        "Success message should indicate successful logout");

    var logoutText = page.locator(".logout-text");
    assertTrue(logoutText.isVisible(), "Logout text should be visible on mobile");
    assertTrue(
        logoutText.innerText().contains("Thank you for using our service"),
        "Logout text should contain thank you message");

    var returnToLoginButton = page.locator(".basic-button");
    assertTrue(
        returnToLoginButton.isVisible(), "Return to login button should be visible on mobile");
    assertEquals(
        "Return to Login",
        returnToLoginButton.innerText().trim(),
        "Button should have text 'Return to Login'");
    assertEquals(
        "/login", returnToLoginButton.getAttribute("href"), "Button should link to login page");

    // Click the return to login button and verify navigation
    returnToLoginButton.click();
    page.waitForURL(url -> url.contains("/login"));
    assertTrue(page.url().contains("/login"), "User should be redirected to login page");
  }
}
