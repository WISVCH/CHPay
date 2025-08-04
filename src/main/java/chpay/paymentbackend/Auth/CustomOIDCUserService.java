package chpay.paymentbackend.Auth;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.paymentbackend.entities.Dienst2ApiResponse;
import chpay.paymentbackend.service.Dienst2Service;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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
@ConfigurationProperties(prefix = "wisvch.chpay")
@Validated
public class CustomOIDCUserService extends OidcUserService {

  @Getter @Setter private List<String> adminGroups;
  @Getter @Setter private String claimName;

  private final Dienst2Service dienst2Service;

  private final UserRepository userRepository;

  @Autowired
  public CustomOIDCUserService(Dienst2Service dienst2Service, UserRepository userRepository) {
    this.dienst2Service = dienst2Service;
    this.userRepository = userRepository;
  }

  /**
   * Loads a user based on the provided OpenID Connect (OIDC) user request. It customizes the
   * default behavior of the {@code OidcUserService} to fetch data from Dienst2, update it in the
   * local database and process user roles and authentication attributes.
   *
   * @param userRequest the OIDC user request containing information about the client and the
   *     incoming authentication request
   * @return an {@link OidcUser} object containing user details, roles, and claims derived from the
   *     identity token
   * @throws OAuth2AuthenticationException if there is an issue with user authentication, such as
   *     missing or invalid user data in Dienst2
   */
  @Transactional
  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);
    OidcIdToken idToken = oidcUser.getIdToken();

    String netid;

    try {
      netid = oidcUser.getClaims().get("netid").toString();
    } catch (NullPointerException e) {
      netid = null;
    }

    String sub = oidcUser.getSubject();

    Dienst2ApiResponse dienst2User;
    // NetID may be null for Google users, so we have to treat them as separate cases
    boolean netidAuth = netid != null;

    // Fetch the user data from dienst2
    if (!netidAuth) {
      String googleUsername = oidcUser.getClaims().get("google_username").toString();
      dienst2User = dienst2Service.fetchUserDataGoogleUsername(googleUsername);
    } else {
      dienst2User = dienst2Service.fetchUserDataNetID(netid);
    }
    if (dienst2User.getCount() != 1) {
      if (dienst2User.getCount() < 1)
        throw new OAuth2AuthenticationException(
            new OAuth2Error("NOT_FOUND"), "User not found in Dienst2");
      else
        throw new OAuth2AuthenticationException(
            new OAuth2Error("CONFLICT"), "Multiple users found in Dienst2");
    }
    Dienst2ApiResponse.UserData userData = dienst2User.getResults().getFirst();
    User user = null;
    // Create or update the user data in the local database
    if (netidAuth) {
      saveOrUpdateUserNetID(userData, sub);
      user = userRepository.findAndLockByOpenID(sub).orElseThrow();
    } else {
      saveOrUpdateUserEmail(userData, sub);
      user = userRepository.findAndLockByEmail(userData.getEmail()).orElseThrow();
    }
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    if (user.getBanned()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_BANNED"));
    } else {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    Object rawGroups = idToken.getClaims().get(claimName);
    Collection<String> groups =
        (rawGroups instanceof Collection<?> collection)
            ? collection.stream().filter(Objects::nonNull).map(Object::toString).toList()
            : List.of();

    // Commented out for dev purposes, all users get ADMIN role to test and implement admin
    // functionality for now.
    /*if (groups.stream().anyMatch(adminGroups::contains)) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }*/
    if (groups.stream().anyMatch(group -> !group.equals("users"))) {
      authorities.add(new SimpleGrantedAuthority("ROLE_COMMITTEE"));
    }

    return new DefaultOidcUser(authorities, idToken, oidcUser.getUserInfo());
  }

  /**
   * Saves or updates a user in the local database based on the NetID provided in the given user
   * data. If a user with the specified NetID already exists, their name and email will be updated.
   * Otherwise, a new user entry will be created with the given details.
   *
   * @param userData the user data containing information such as first name, surname, email, and
   *     NetID, which is used to identify and update or create a user in the database
   */
  @Transactional
  public User saveOrUpdateUserNetID(Dienst2ApiResponse.UserData userData, String sub) {
    Optional<User> existingUser = userRepository.findAndLockByOpenID(sub);
    User user;
    String preposition;
    if (userData.getPreposition() != null && !userData.getPreposition().isEmpty()) {
      preposition = userData.getPreposition() + " ";
    } else preposition = "";
    if (existingUser.isPresent()) {
      user = existingUser.get();
      user.setName(userData.getFirstname() + " " + preposition + userData.getSurname());
      user.setEmail(userData.getEmail());
    } else {
      user =
          new User(
              userData.getFirstname() + " " + preposition + userData.getSurname(),
              userData.getEmail(),
              sub);
    }

    return userRepository.save(user);
  }

  /**
   * Saves or updates a user in the local database based on the email provided in the given user
   * data. If a user with the specified email already exists, their name and email will be updated.
   * Otherwise, a new user entry will be created with the given details.
   *
   * @param userData the user data containing information such as first name, surname, email, and
   *     NetID, which is used to identify and update or create a user in the database
   */
  @Transactional
  public User saveOrUpdateUserEmail(Dienst2ApiResponse.UserData userData, String sub) {
    String email = userData.getEmail();
    Optional<User> existingUser = userRepository.findAndLockByEmail(email);
    User user;
    String preposition;
    if (userData.getPreposition() != null && !userData.getPreposition().isEmpty()) {
      preposition = userData.getPreposition() + " ";
    } else preposition = "";
    if (existingUser.isPresent()) {
      user = existingUser.get();
      user.setName(userData.getFirstname() + " " + preposition + userData.getSurname());
      user.setEmail(userData.getEmail());
    } else {
      user =
          new User(
              userData.getFirstname() + " " + preposition + userData.getSurname(),
              userData.getEmail(),
              sub);
    }

    return userRepository.save(user);
  }
}
