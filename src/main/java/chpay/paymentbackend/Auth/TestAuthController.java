package chpay.paymentbackend.Auth;

import chpay.paymentbackend.entities.Dienst2ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("test")
public class TestAuthController {

  private final CustomOIDCUserService customOIDCUserService;

  private static final Logger logger = LoggerFactory.getLogger(TestAuthController.class);

  @Autowired
  public TestAuthController(CustomOIDCUserService customOIDCUserService) {
    this.customOIDCUserService = customOIDCUserService;
  }

  /**
   * Simulates a login process for testing purposes. Creates an OAuth2 authentication context for a
   * specified username and roles, and stores the authentication in the session. Returns an HTML
   * response indicating the authentication status.
   *
   * @param request the HttpServletRequest containing session and request-related data
   * @param username the username of the user to simulate login for
   * @param roles an array of roles assigned to the user; defaults to "USER" if not provided
   * @return a ResponseEntity with a String indicating the authentication result; contains either a
   *     success message with a link to the app or an error message with the failure reason
   */
  @GetMapping("/test/login-get")
  public ResponseEntity<String> testLoginGet(
      HttpServletRequest request,
      @RequestParam String username,
      @RequestParam(defaultValue = "USER") String[] roles) {
    try {
      logger.info(
          "Test login attempt for user: {} with roles: {}", username, Arrays.toString(roles));

      Collection<GrantedAuthority> authorities =
          Arrays.stream(roles)
              .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
              .collect(Collectors.toList());

      logger.info("Created authorities: {}", authorities);

      // Create OAuth2User attributes similar to your real OAuth2 flow
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("sub", username);
      attributes.put("name", username);
      attributes.put("email", username + "@test.com");
      attributes.put("preferred_username", username);
      attributes.put("netid", username);

      Dienst2ApiResponse.UserData userData = new Dienst2ApiResponse.UserData();
      userData.setEmail(username + "@test.com");
      userData.setFirstname("test");
      userData.setSurname(username);
      userData.setNetid(username);

      customOIDCUserService.saveOrUpdateUserNetID(userData, username);

      logger.info("Created attributes: {}", attributes);

      OidcUser oidcUser =
          new DefaultOidcUser(
              authorities,
              new OidcIdToken(
                  "dummy-token", Instant.now(), Instant.now().plusSeconds(3600), attributes));

      logger.info("Created OAuth2User: {}", oidcUser.getName());

      OAuth2AuthenticationToken auth =
          new OAuth2AuthenticationToken(oidcUser, authorities, "wisvchconnect");

      logger.info("Created authentication token");

      SecurityContextHolder.getContext().setAuthentication(auth);
      logger.info("Set authentication in SecurityContextHolder");

      // Store in session
      request
          .getSession()
          .setAttribute(
              HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
              SecurityContextHolder.getContext());

      logger.info("Successfully authenticated user: {}", username);

      return ResponseEntity.ok(
          "<html><body>Authenticated as " + username + "! <a href='/'>Go to App</a></body></html>");

    } catch (Exception e) {
      logger.error("Error during test authentication for user: {}", username, e);
      return ResponseEntity.status(500).body("Authentication failed: " + e.getMessage());
    }
  }

  /**
   * Retrieves the current authentication status of the user from the security context. If the user
   * is authenticated, their username, granted authorities, and principal details are included in
   * the response.
   *
   * @return a ResponseEntity containing a map with the authentication status. The map includes: -
   *     "authenticated": a boolean indicating whether the user is authenticated - "username": the
   *     username of the authenticated user, present if authenticated - "authorities": a string
   *     representation of the user's granted authorities, present if authenticated - "principal":
   *     the class name of the user's principal, present if authenticated If an error occurs, the
   *     response contains an "error" key with the error details.
   */
  @GetMapping("/test/auth-status")
  public ResponseEntity<Map<String, Object>> getAuthStatus() {
    try {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      Map<String, Object> status = new HashMap<>();

      if (auth != null && auth.isAuthenticated()) {
        status.put("authenticated", true);
        status.put("username", auth.getName());
        status.put("authorities", auth.getAuthorities().toString());
        status.put("principal", auth.getPrincipal().getClass().getSimpleName());
      } else {
        status.put("authenticated", false);
      }

      return ResponseEntity.ok(status);
    } catch (Exception e) {
      logger.error("Error getting auth status", e);
      return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Provides a simple endpoint to verify that the test controller is functioning.
   *
   * @return a ResponseEntity containing the message "Test controller is working!" with an HTTP
   *     status of 200 (OK)
   */
  @GetMapping("/test/ping")
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("Test controller is working!");
  }
}
