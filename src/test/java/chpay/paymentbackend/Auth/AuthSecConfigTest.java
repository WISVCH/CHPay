package chpay.paymentbackend.Auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for AuthSecConfig. We load only the security filter chain (no controllers). Public URLs
 * should not redirect; protected URLs should 302 to /login.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthSecConfigTest {

  @Autowired private MockMvc mockMvc;

  // satisfy the autowired CustomOIDCUserService
  @MockitoBean private CustomOIDCUserService customOidcUserService;

  @Nested
  @DisplayName("Anonymous access")
  class Anonymous {

    @Test
    @DisplayName("GET /  is permitted (with redirect to /login)")
    void rootPermitted() throws Exception {
      mockMvc
          .perform(get("/"))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", containsString("/login")));
    }

    /**
     * @Test @DisplayName("GET /login is permitted (no redirect)") void loginPagePermitted() throws
     * Exception { mockMvc.perform(get("/login")).andExpect(status().isOk()); } *
     */
    @Test
    @DisplayName("GET /error is accessible without authentication")
    void errorEndpointIsAccessible() throws Exception {
      mockMvc.perform(get("/error?reason=test-error")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /logout-success  is forbidden without login")
    void logoutSuccessPermitted() throws Exception {
      mockMvc
          .perform(get("/logout-success"))
          .andExpect(status().is3xxRedirection())
          .andExpect(flash().attribute("statuscode", 403));
    }

    @Test
    @DisplayName("GET /private is protected => redirect to /login")
    void protectedRedirectsToLogin() throws Exception {
      mockMvc
          .perform(get("/private"))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("GET /index is protected => redirect to /login")
    void dashboardRedirectsToLogin() throws Exception {
      mockMvc.perform(get("/index")).andExpect(status().is3xxRedirection());
    }
  }

  @Nested
  @DisplayName("Authenticated access")
  class Authenticated {

    @Test
    @WithMockUser(username = "u1")
    @DisplayName("GET /private    with user => back to index")
    void privateAllowedWhenAuthenticated() throws Exception {
      mockMvc
          .perform(get("/private"))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", containsString("/index")));
    }

    @Test
    @WithMockUser(username = "u1")
    @DisplayName("Authenticated user can access /error")
    void authenticatedUserCanAccessError() throws Exception {
      mockMvc.perform(get("/error")).andExpect(status().isOk());
    }

    /**
     * @Test @WithMockUser(username = "u1") @DisplayName("GET /index is allowed") void
     * indexAllowedWhenAuthenticated() throws Exception {
     * mockMvc.perform(get("/index")).andExpect(status().isOk()); } *
     */
    @Test
    @WithMockUser(username = "u1")
    @DisplayName("GET /login redirects to index when already logged in")
    void loginRedirectsWhenAuthenticated() throws Exception {
      mockMvc
          .perform(get("/login"))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", containsString("/index")));
    }
  }

  @Nested
  @DisplayName("Logout configuration")
  class LogoutTests {

    @Test
    @WithMockUser
    @DisplayName("POST /logout   invalidates session and redirects to /logout-success")
    void logoutEndpoint() throws Exception {
      mockMvc
          .perform(logout("/logout"))
          .andExpect(status().is3xxRedirection())
          // verify that it redirects to /logout-success with any timestamp parameter
          .andExpect(header().string("Location", containsString("/logout-success?ts=")))
          .andExpect(cookie().maxAge("JSESSIONID", 0));
    }

    /**
     * @Test @WithMockUser(username = "u1") @DisplayName("GET /logout-success with timestamp is
     * accessible when authenticated") void logoutSuccessWithTimestampIsAccessible() throws
     * Exception { long timestamp = System.currentTimeMillis();
     * mockMvc.perform(get("/logout-success?ts=" + timestamp)).andExpect(status().isOk()); } *
     */
  }
}
