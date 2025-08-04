package chpay.frontend.shared;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.paymentbackend.service.SettingService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

public class GlobalModelAttributesTest {

  private UserRepository userRepository;
  private GlobalModelAttributes attributes;
  private SettingService settingService;
  private Model model;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    settingService = mock(SettingService.class);
    attributes = new GlobalModelAttributes(userRepository, settingService);
    model = new ExtendedModelMap(); // Implements Model
  }

  @Test
  void addsCurrentUserToModel_whenOidcUserWithPreferredUsername() {
    Authentication authentication = mock(Authentication.class);
    OidcUser oidcUser = mock(OidcUser.class);

    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(oidcUser.getAttribute("sub")).thenReturn("wisv.number");

    User user = new User("John Doe", "jdoe@example.com", "wisv.number", BigDecimal.TEN);
    when(userRepository.findByOpenID("wisv.number"))
        .thenReturn(Optional.of(user)); // wrapped in Optional

    attributes.addCurrentUserToModel(model, authentication);

    assertThat((Map<String, Object>) model).containsEntry("currentUser", user);
  }

  @Test
  void doesNotAddCurrentUser_whenAuthenticationIsNull() {
    attributes.addCurrentUserToModel(model, null);

    assertThat((Map<String, Object>) model).doesNotContainKey("currentUser");
  }

  @Test
  void doesNotAddCurrentUser_whenPrincipalIsNotOidcUser() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn("not-an-oidc-user");

    attributes.addCurrentUserToModel(model, authentication);

    assertThat((Map<String, Object>) model).doesNotContainKey("currentUser");
  }

  @Test
  void doesNotAddCurrentUser_whenPreferredUsernameIsNull() {
    Authentication authentication = mock(Authentication.class);
    OidcUser oidcUser = mock(OidcUser.class);
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(oidcUser.getAttribute("preferred_username")).thenReturn(null);

    attributes.addCurrentUserToModel(model, authentication);

    assertThat((Map<String, Object>) model).doesNotContainKey("currentUser");
  }
}
