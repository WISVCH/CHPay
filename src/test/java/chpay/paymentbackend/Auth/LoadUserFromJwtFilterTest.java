package chpay.paymentbackend.Auth;

import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class LoadUserFromJwtFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private CustomOIDCUserService customOIDCUserService;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;
  @Mock private Jwt jwt;
  @Mock private RestTemplate restTemplate;

  private LoadUserFromJwtFilter filter;

  @BeforeEach
  void setUp() {
    filter =
        new LoadUserFromJwtFilter(
            userRepository, customOIDCUserService, restTemplate);
  }

  @Test
  void shouldFetchUserInfoFromOidcAndPersistWhenNotInDatabase() throws Exception {
    String subject = "test-subject";
    String token = "jwt-token";

    Authentication auth = mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(auth.getPrincipal()).thenReturn(jwt);
    when(jwt.getSubject()).thenReturn(subject);
    when(jwt.getTokenValue()).thenReturn(token);

    when(userRepository.findByOpenID(subject)).thenReturn(Optional.empty());

    Map<String, Object> userInfo =
            Map.of(
                    "preferred_username", "netid123",
                    "google_username", "john.doe@gmail.com",
                    "name", "test",
                    "email", "test@example.com");
    ResponseEntity<Map<String, Object>> userInfoResponse =
            new ResponseEntity<>(userInfo, HttpStatus.OK);
    when(restTemplate.exchange(
            eq("https://connect.ch.tudelft.nl/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
            .thenReturn(userInfoResponse);

    User persistedUser = new User("test", "test@example.com", subject);
    when(customOIDCUserService.saveOrUpdateUser("test", "test@example.com", subject)).thenReturn(persistedUser);

    filter.doFilterInternal(request, response, filterChain);

    verify(request).setAttribute("user", persistedUser);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldReturnUnauthorizedWhenUserInfoIsNull() throws Exception {
    when(userRepository.findByOpenID(any())).thenReturn(Optional.empty());

    Authentication auth = mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(auth.getPrincipal()).thenReturn(jwt);
    when(jwt.getSubject()).thenReturn("subject");
    when(jwt.getTokenValue()).thenReturn("token");

    ResponseEntity<Map<String, Object>> nullBody = new ResponseEntity<>(null, HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
        .thenReturn(nullBody);

    filter.doFilterInternal(request, response, filterChain);

    verify(response).sendError(HttpStatus.UNAUTHORIZED.value(), "Missing userinfo response");
    verify(filterChain, never()).doFilter(request, response);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
