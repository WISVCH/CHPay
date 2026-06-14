package ch.wisv.chpay.auth.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class LoginControllerTest {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void loginRedirectsAnonymousUsersToSso() throws Exception {
    mockMvc
        .perform(get("/login"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/oauth2/authorization/wisvchconnect"));
  }

  @Test
  void loginRedirectsAuthenticatedUsersToIndex() throws Exception {
    mockMvc
        .perform(get("/login").with(oidcLogin()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/index"));
  }

  @Test
  void loginErrorStillRendersRecoveryPage() throws Exception {
    mockMvc
        .perform(get("/login").param("error", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attribute("notificationType", "error"))
        .andExpect(
            model().attribute("notificationMessage", "Authentication failed. Please try again."));
  }

  @Test
  void loginLogoutStillRendersRecoveryPage() throws Exception {
    mockMvc
        .perform(get("/login").param("logout", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attribute("logoutMessage", "You have been successfully logged out."));
  }

  @Test
  void loginExpiredStillRendersRecoveryPage() throws Exception {
    mockMvc
        .perform(get("/login").param("expired", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attribute("notificationType", "message"))
        .andExpect(
            model()
                .attribute(
                    "notificationMessage",
                    "You have been banned/unbanned. Please contact support."));
  }

  @Test
  void expiredRedirectsToRecoveryPage() throws Exception {
    mockMvc
        .perform(get("/expired"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?expired"));
  }
}
