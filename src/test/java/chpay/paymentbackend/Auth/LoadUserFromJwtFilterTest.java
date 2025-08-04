package chpay.paymentbackend.Auth;

import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import chpay.DatabaseHandler.transactiondb.repositories.UserRepository;
import chpay.paymentbackend.entities.Dienst2ApiResponse;
import chpay.paymentbackend.service.Dienst2Service;
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
  @Mock private Dienst2Service dienst2Service;
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
            userRepository, dienst2Service, customOIDCUserService, restTemplate);
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
            "google_username", "john.doe@gmail.com");
    ResponseEntity<Map<String, Object>> userInfoResponse =
        new ResponseEntity<>(userInfo, HttpStatus.OK);
    when(restTemplate.exchange(
            eq("https://connect.ch.tudelft.nl/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(userInfoResponse);

    Dienst2ApiResponse.UserData userData = new Dienst2ApiResponse.UserData();
    userData.setNetid("netid123");
    userData.setEmail("john@example.com");

    Dienst2ApiResponse apiResponse = new Dienst2ApiResponse();
    apiResponse.setCount(1);
    apiResponse.setResults(List.of(userData));
    when(dienst2Service.fetchUserDataNetID("netid123")).thenReturn(apiResponse);

    User persistedUser = new User();
    when(customOIDCUserService.saveOrUpdateUserNetID(userData, subject)).thenReturn(persistedUser);

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

  @Test
  void shouldUseGoogleUsernameWhenNetidIsMissing() throws Exception {
    when(userRepository.findByOpenID(any())).thenReturn(Optional.empty());

    Authentication auth = mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(auth.getPrincipal()).thenReturn(jwt);
    when(jwt.getSubject()).thenReturn("subject");
    when(jwt.getTokenValue()).thenReturn("token");

    Map<String, Object> userInfo = Map.of("google_username", "guser");
    ResponseEntity<Map<String, Object>> responseEntity =
        new ResponseEntity<>(userInfo, HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
        .thenReturn(responseEntity);

    Dienst2ApiResponse.UserData data = new Dienst2ApiResponse.UserData();
    Dienst2ApiResponse responseFromDienst2 = new Dienst2ApiResponse();
    responseFromDienst2.setCount(1);
    responseFromDienst2.setResults(List.of(data));
    when(dienst2Service.fetchUserDataGoogleUsername("guser")).thenReturn(responseFromDienst2);

    User user = new User();
    when(customOIDCUserService.saveOrUpdateUserEmail(data, "subject")).thenReturn(user);

    filter.doFilterInternal(request, response, filterChain);

    verify(request).setAttribute("user", user);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldReturnUnauthorizedWhenDienst2ReturnsZeroUsers() throws Exception {
    when(userRepository.findByOpenID(any())).thenReturn(Optional.empty());

    Authentication auth = mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(auth.getPrincipal()).thenReturn(jwt);
    when(jwt.getSubject()).thenReturn("subject");
    when(jwt.getTokenValue()).thenReturn("token");

    Map<String, Object> userInfo = Map.of("preferred_username", "netid123");
    ResponseEntity<Map<String, Object>> responseEntity =
        new ResponseEntity<>(userInfo, HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
        .thenReturn(responseEntity);

    Dienst2ApiResponse emptyResponse = new Dienst2ApiResponse();
    emptyResponse.setCount(0);
    emptyResponse.setResults(List.of());
    when(dienst2Service.fetchUserDataNetID("netid123")).thenReturn(emptyResponse);

    filter.doFilterInternal(request, response, filterChain);

    verify(response).sendError(HttpStatus.UNAUTHORIZED.value(), "User not found in Dienst2");
    verify(filterChain, never()).doFilter(any(), any());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }
}
