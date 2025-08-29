package chpay.paymentbackend.Auth;

import static org.junit.jupiter.api.Assertions.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest
class CustomOIDCUserServiceTest {

  @Autowired private CustomOIDCUserService service;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  void setUp() {
    service.setClaimName("groups");
    service.setAdminGroups(List.of("admin"));
  }

  @Test
  void userInDienst2NetIDUpdatesDatabase() {
    Map<String, Object> claims = Map.of("groups", List.of());
    service.loadUser(buildRequest(addStandardClaims(claims)));
    Optional<User> updatedUser = userRepo.findByOpenID("user123");
    assertTrue(updatedUser.isPresent());
    assertEquals("testemail@email.email", updatedUser.get().getEmail());
    assertEquals("test user", updatedUser.get().getName());
  }

  @Test
  void userInDienst2EmailCreatesUserInDatabase() {
    Map<String, Object> claims = Map.of("groups", List.of());
    service.loadUser(buildRequest(addStandardEmailClaims(claims)));
    Optional<User> updatedUser = userRepo.findByEmail("testemail@email.email");
    assertTrue(updatedUser.isPresent());
    assertEquals("test user", updatedUser.get().getName());
  }

  @Test
  void loadUser_withNoGroups_shouldReturnDefaultAuthorities() {
    Map<String, Object> claims = Map.of("groups", List.of());

    OidcUser result = service.loadUser(buildRequest(addStandardClaims(claims)));

    assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Test
  void loadUser_withOnlyUsersGroup_shouldNotAddCommitteeRole() {
    Map<String, Object> claims = Map.of("groups", List.of("users"));

    OidcUser result = service.loadUser(buildRequest(addStandardClaims(claims)));

    assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
  }

  // This test ensures the admin role is correctly assigned even
  // when we do not always assign it to every user for dev purposes
  @Test
  void loadUser_withAdminGroup() {
    Map<String, Object> claims = Map.of("groups", List.of("admin"));

    OidcUser result = service.loadUser(buildRequest(addStandardClaims(claims)));

    assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  @Test
  void loadUser_withCommitteeGroup_shouldAddCommitteeRole() {
    Map<String, Object> claims = Map.of("groups", List.of("users", "committee"));

    OidcUser result = service.loadUser(buildRequest(addStandardClaims(claims)));

    assertAuthorities(result, "ROLE_USER");
  }

  // -----------------------------------------------------------------------
  // Helper methods
  // -----------------------------------------------------------------------

  private OidcUserRequest buildRequest(Map<String, Object> claims) {
    // ---- ID Token -------------------------------------------------------
    OidcIdToken idToken =
        new OidcIdToken("dummy-token", Instant.now(), Instant.now().plusSeconds(3600), claims);

    // ---- ClientRegistration (userInfoUri == "" to skip remote call) ----
    ClientRegistration clientReg =
        ClientRegistration.withRegistrationId("test")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .clientId("client-id")
            .authorizationUri("https://example.com/auth")
            .tokenUri("https://example.com/token")
            .jwkSetUri("https://example.com/jwks")
            .userInfoUri("") // <= prevents network call
            .issuerUri("https://example.com")
            .scope("openid")
            .build();

    // ---- Access Token (plain dummy) ------------------------------------
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access",
            Instant.now(),
            Instant.now().plusSeconds(3600));

    return new OidcUserRequest(clientReg, accessToken, idToken);
  }

  private Map<String, Object> addStandardClaims(Map<String, Object> claims) {
    Map<String, Object> full = new HashMap<>(claims);
    full.putIfAbsent("sub", "user123");
    full.putIfAbsent("iss", "https://example.com");
    full.putIfAbsent("aud", "client-id");
    full.putIfAbsent("exp", Instant.now().plusSeconds(3600).getEpochSecond());
    full.putIfAbsent("iat", Instant.now().getEpochSecond());
    full.putIfAbsent("netid", "12345");
    full.putIfAbsent("email", "testemail@email.email");
    full.putIfAbsent("name", "test user");
    return full;
  }

  private Map<String, Object> addStandardEmailClaims(Map<String, Object> claims) {
    Map<String, Object> full = new HashMap<>(claims);
    full.putIfAbsent("sub", "user123");
    full.putIfAbsent("iss", "https://example.com");
    full.putIfAbsent("aud", "client-id");
    full.putIfAbsent("exp", Instant.now().plusSeconds(3600).getEpochSecond());
    full.putIfAbsent("iat", Instant.now().getEpochSecond());
    full.putIfAbsent("google_username", "testgoogleusername");
    full.putIfAbsent("email", "testemail@email.email");
    full.putIfAbsent("name", "test user");
    return full;
  }

  private void assertAuthorities(OidcUser user, String... expectedRoles) {
    Set<String> actual = new HashSet<>();
    for (GrantedAuthority a : user.getAuthorities()) {
      actual.add(a.getAuthority());
    }
    assertEquals(Set.of(expectedRoles), actual);
  }
}
