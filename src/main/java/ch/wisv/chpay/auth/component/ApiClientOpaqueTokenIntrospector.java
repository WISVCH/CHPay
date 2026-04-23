package ch.wisv.chpay.auth.component;

import ch.wisv.chpay.api.client.service.ApiClientService;
import ch.wisv.chpay.api.client.service.InvalidApiClientTokenException;
import java.util.Map;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

@Component
public class ApiClientOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

  private static final OAuth2Error INVALID_TOKEN_ERROR = new OAuth2Error("invalid_token");
  private final ApiClientService apiClientService;

  public ApiClientOpaqueTokenIntrospector(ApiClientService apiClientService) {
    this.apiClientService = apiClientService;
  }

  @Override
  public OAuth2AuthenticatedPrincipal introspect(String token) {
    try {
      ApiClientService.AuthenticatedApiClient authenticated =
          apiClientService.authenticateBearerToken(token);
      return new DefaultOAuth2AuthenticatedPrincipal(
          authenticated.client().getId().toString(),
          Map.of(
              "client_id", authenticated.client().getId().toString(),
              "client_name", authenticated.client().getName(),
              "token_id", authenticated.client().getTokenId()),
          authenticated.authorities());
    } catch (InvalidApiClientTokenException ex) {
      throw new OAuth2AuthenticationException(INVALID_TOKEN_ERROR, ex.getMessage(), ex);
    }
  }
}
