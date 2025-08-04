package chpay.paymentbackend.Auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class HomeControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void rootRedirectsToIndexWhenAuthenticated() throws Exception {
    mockMvc
        .perform(get("/").with(user("user").roles("USER")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/index"));
  }

  @Test
  void rootRedirectsToLoginWhenUnauthenticated() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void dashboardRenders() throws Exception {
    mockMvc
        .perform(get("/dashboard").with(user("user").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"));
  }

  @Test
  void loginRedirectsToIndexWhenAuthenticated() throws Exception {
    mockMvc
        .perform(get("/login").with(user("user").roles("USER")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/index"));
  }

  @Test
  void loginPageRenders_withLogoutParam() throws Exception {
    mockMvc
        .perform(get("/login").param("logout", "true"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("logoutMessage", "You have been successfully logged out."))
        .andExpect(view().name("login"));
  }

  @Test
  void loginPageRenders_withErrorParam() throws Exception {
    mockMvc
        .perform(get("/login").param("error", "true"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void errorPageRenders() throws Exception {
    mockMvc.perform(get("/error")).andExpect(status().isOk()).andExpect(view().name("error"));
  }

  @Test
  void logoutSuccessRejectsNullTimestamp() throws Exception {
    mockMvc
        .perform(get("/logout-success"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/error"));
  }

  @Test
  void logoutSuccessRedirectsIfTooOld() throws Exception {
    long oldTs = System.currentTimeMillis() - 6000; // older than 5 seconds
    mockMvc
        .perform(get("/logout-success").param("ts", String.valueOf(oldTs)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void logoutSuccessPageRendersForRecentTimestamp() throws Exception {
    long recentTs = System.currentTimeMillis();
    mockMvc
        .perform(get("/logout-success").param("ts", String.valueOf(recentTs)))
        .andExpect(status().isOk())
        .andExpect(view().name("logout-success"));
  }
}
