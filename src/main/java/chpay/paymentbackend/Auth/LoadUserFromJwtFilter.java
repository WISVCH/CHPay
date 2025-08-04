package chpay.paymentbackend.Auth;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.paymentbackend.entities.Dienst2ApiResponse;
import chpay.paymentbackend.service.Dienst2Service;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
public class LoadUserFromJwtFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;
  private final Dienst2Service dienst2Service;
  private final CustomOIDCUserService customOIDCUserService;
  private final RestTemplate restTemplate;

  public LoadUserFromJwtFilter(
      UserRepository userRepository,
      Dienst2Service dienst2Service,
      CustomOIDCUserService customOIDCUserService,
      RestTemplate restTemplate) {
    this.userRepository = userRepository;
    this.dienst2Service = dienst2Service;
    this.customOIDCUserService = customOIDCUserService;
    this.restTemplate = restTemplate;
  }

  /**
   * Loads user data from the local database if it exists, otherwise fetches user data from Dienst2.
   * Need this in order to create transactions tied to users coming from events, even if they do not
   * exist of the CHPay database
   *
   * @param request The HTTP request that contains the client request.
   * @param response The HTTP response that contains the response to the client.
   * @param filterChain The filter chain that allows invoking the next filter in the chain.
   * @throws ServletException if an error occurs while processing the request or response.
   * @throws IOException if an I/O error occurs while processing the request or response.
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      String openID = jwt.getSubject();
      Optional<User> user = userRepository.findByOpenID(openID);

      if (user.isEmpty()) {
        user = fetchAndPersistUser(jwt, response);
        if (user.isEmpty()) {
          return;
        }
        request.setAttribute("user", user.get());
      }
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Fetches user data from Dienst2 and persists it in the local database. User is fetched by netID
   * where possible, otherwise email is used. User is only fetched if it does not exist in the local
   * database. Updating user details is left to the {@code CustomOIDCUserService}.
   *
   * @param jwt The JWT token to fetch user data from Dienst2 with.
   * @param response The HTTP response to send errors to if the user data could not be fetched.
   * @return An {@link Optional} containing the {@link User} object if the user data could be
   * @throws IOException if an I/O error occurs while processing the request or response.
   */
  private Optional<User> fetchAndPersistUser(Jwt jwt, HttpServletResponse response)
      throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwt.getTokenValue());
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Map<String, Object>> res =
        restTemplate.exchange(
            "https://connect.ch.tudelft.nl/userinfo",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<>() {});

    Map<String, Object> userInfo = res.getBody();
    if (userInfo == null) {
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing userinfo response");
      return Optional.empty();
    }
    String netid = (String) userInfo.get("preferred_username");
    String googleUsername = (String) userInfo.get("google_username");
    String sub = jwt.getSubject();

    Dienst2ApiResponse dienst2User =
        (netid != null)
            ? dienst2Service.fetchUserDataNetID(netid)
            : dienst2Service.fetchUserDataGoogleUsername(googleUsername);

    if (dienst2User.getCount() != 1) {
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "User not found in Dienst2");
      return Optional.empty();
    }

    Dienst2ApiResponse.UserData data = dienst2User.getResults().getFirst();
    User saved =
        (netid != null)
            ? customOIDCUserService.saveOrUpdateUserNetID(data, sub)
            : customOIDCUserService.saveOrUpdateUserEmail(data, sub);

    return Optional.of(saved);
  }
}
