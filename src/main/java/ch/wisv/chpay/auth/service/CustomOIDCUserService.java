package ch.wisv.chpay.auth.service;

import ch.wisv.chpay.core.model.User;
import ch.wisv.chpay.core.repository.UserRepository;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * CustomOIDCUserService is an extension of the {@link OidcUserService} class that provides custom
 * handling of OpenID Connect (OIDC) user authentication.
 *
 * <p>This service overrides the {@code loadUser} method to allow customization of the OIDC user
 * loading process, including potential integration with additional backend services or database
 * logic.
 *
 * <p>This class is utilized in the application to process user information during OAuth2 OIDC
 * authentication workflows.
 *
 * <p>- Logs the start of the OIDC authentication process for debugging or monitoring purposes. -
 * Delegates to the superclass implementation while allowing for custom behavior to be added.
 *
 * <p>Note: Further customization logic will be added
 */
@Service
@ConfigurationProperties(prefix = "chpay")
@Validated
public class CustomOIDCUserService extends OidcUserService {

  @Getter @Setter private List<String> adminGroups;
  @Getter @Setter private String claimName;

  private final UserRepository userRepository;

  @Autowired
  public CustomOIDCUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Loads a user based on the provided OpenID Connect (OIDC) user request. It customizes the
   * default behavior of the {@code OidcUserService} update data from connect it in the local
   * database and process user roles and authentication attributes.
   *
   * @param userRequest the OIDC user request containing information about the client and the
   *     incoming authentication request
   * @return an {@link OidcUser} object containing user details, roles, and claims derived from the
   *     identity token
   * @throws OAuth2AuthenticationException if there is an issue with user authentication
   */
  @Transactional
  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);
    OidcIdToken idToken = oidcUser.getIdToken();
    String sub = oidcUser.getSubject();
    String name = oidcUser.getGivenName() + " " + oidcUser.getFamilyName();
    String email = oidcUser.getEmail();

    User user = saveOrUpdateUser(name, email, sub);

    Object rawGroups = idToken.getClaims().get(claimName);
    Collection<String> groups =
        (rawGroups instanceof Collection<?> collection)
            ? collection.stream().filter(Objects::nonNull).map(Object::toString).toList()
            : List.of();

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    if (user.getBanned()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_BANNED"));
    }
    if (groups.stream().anyMatch(adminGroups::contains)) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    return new DefaultOidcUser(authorities, idToken, oidcUser.getUserInfo());
  }

  /**
   * Saves or updates a user in the local database based on the provided name, email, and OpenID
   * subject. If a user with the specified OpenID subject already exists, their name and email will
   * be updated. Otherwise, a new user entry will be created with the given details.
   *
   * @param name the user's full name
   * @param email the user's email address
   * @param sub the user's OpenID subject (unique identifier)
   * @return the saved or updated User entity
   */
  @Transactional
  public User saveOrUpdateUser(String name, String email, String sub) {
    Optional<User> existingUser = userRepository.findAndLockByOpenID(sub);
    User user;
    if (existingUser.isPresent()) {
      user = existingUser.get();
      user.setName(name);
      user.setEmail(email);
    } else {
      user = new User(name, email, sub);
    }

    return userRepository.save(user);
  }
}
