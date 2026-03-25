package ch.wisv.chpay.customer.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class IndexPageRenderTest {

  private static final String OPEN_ID = "oidc-test-user";

  private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;
  @Autowired private WebApplicationContext context;

  @BeforeEach
  void setUpUser() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    userRepository.deleteAll();
    userRepository.saveAndFlush(new User("Index Test User", "index-test@chpay.invalid", OPEN_ID));
  }

  @Test
  void indexPageRendersSuccessfully() throws Exception {
    mockMvc
        .perform(
            get("/index")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .idToken(token -> token.claim("sub", OPEN_ID))))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(content().string(containsString("Welcome to CHPay!")));
  }

  @Test
  void indexPageResolvesViteAssets() throws Exception {
    mockMvc
        .perform(
            get("/index")
                .with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .idToken(token -> token.claim("sub", OPEN_ID))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/assets/main-test.js")))
        .andExpect(content().string(not(containsString("<vite:"))));
  }
}
