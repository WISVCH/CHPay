package ch.wisv.chpay.api.client.service;

import ch.wisv.chpay.api.client.model.ApiClient;
import ch.wisv.chpay.api.client.model.ApiClientRole;
import ch.wisv.chpay.api.client.repository.ApiClientRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiClientService {

  private static final int TOKEN_ID_BYTES = 12;
  private static final int TOKEN_SECRET_BYTES = 32;
  private static final int TOKEN_ID_RETRIES = 5;

  private final ApiClientRepository apiClientRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecureRandom secureRandom = new SecureRandom();

  public ApiClientService(
      ApiClientRepository apiClientRepository, PasswordEncoder passwordEncoder) {
    this.apiClientRepository = apiClientRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<ApiClient> findAllClients() {
    return apiClientRepository.findAllByOrderByNameAsc();
  }

  @Transactional(readOnly = true)
  public Optional<ApiClient> findClientById(UUID clientId) {
    return apiClientRepository.findById(clientId);
  }

  @Transactional
  public IssuedToken createClient(String name, Set<ApiClientRole> roles) {
    validateName(name);
    String normalizedName = name.trim();

    if (apiClientRepository.existsByNameIgnoreCase(normalizedName)) {
      throw new IllegalArgumentException("Api client with this name already exists");
    }
    validateRoles(roles);

    String tokenId = generateUniqueTokenId();
    String secret = generateTokenPart(TOKEN_SECRET_BYTES);
    String tokenHash = passwordEncoder.encode(secret);

    ApiClient client = new ApiClient(normalizedName, tokenId, tokenHash, roles);
    ApiClient saved = apiClientRepository.save(client);
    return new IssuedToken(saved, composeToken(tokenId, secret));
  }

  @Transactional
  public IssuedToken rotateToken(UUID clientId) {
    ApiClient client =
        apiClientRepository
            .findById(clientId)
            .orElseThrow(() -> new NoSuchElementException("Api client not found"));
    String tokenId = generateUniqueTokenId();
    String secret = generateTokenPart(TOKEN_SECRET_BYTES);
    client.setTokenId(tokenId);
    client.setTokenHash(passwordEncoder.encode(secret));
    ApiClient saved = apiClientRepository.save(client);
    return new IssuedToken(saved, composeToken(tokenId, secret));
  }

  @Transactional
  public ApiClient setEnabled(UUID clientId, boolean enabled) {
    ApiClient client =
        apiClientRepository
            .findById(clientId)
            .orElseThrow(() -> new NoSuchElementException("Api client not found"));
    client.setEnabled(enabled);
    return apiClientRepository.save(client);
  }

  @Transactional
  public ApiClient updateRoles(UUID clientId, Set<ApiClientRole> roles) {
    validateRoles(roles);
    ApiClient client =
        apiClientRepository
            .findById(clientId)
            .orElseThrow(() -> new NoSuchElementException("Api client not found"));
    client.setRoles(roles);
    return apiClientRepository.save(client);
  }

  @Transactional
  public AuthenticatedApiClient authenticateBearerToken(String rawToken) {
    ParsedToken parsedToken = parseToken(rawToken);
    ApiClient client =
        apiClientRepository
            .findByTokenIdAndEnabledTrue(parsedToken.tokenId())
            .orElseThrow(() -> new InvalidApiClientTokenException("Invalid bearer token"));

    if (!passwordEncoder.matches(parsedToken.secret(), client.getTokenHash())) {
      throw new InvalidApiClientTokenException("Invalid bearer token");
    }

    client.setLastUsedAt(Instant.now());
    apiClientRepository.save(client);

    Collection<GrantedAuthority> authorities =
        client.getRoles().stream()
            .map(ApiClientRole::authority)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toUnmodifiableSet());
    return new AuthenticatedApiClient(client, authorities);
  }

  private ParsedToken parseToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new InvalidApiClientTokenException("Missing bearer token");
    }
    int separator = rawToken.indexOf('.');
    if (separator <= 0 || separator >= rawToken.length() - 1) {
      throw new InvalidApiClientTokenException("Invalid bearer token format");
    }
    return new ParsedToken(rawToken.substring(0, separator), rawToken.substring(separator + 1));
  }

  private String generateUniqueTokenId() {
    for (int i = 0; i < TOKEN_ID_RETRIES; i++) {
      String tokenId = generateTokenPart(TOKEN_ID_BYTES);
      if (!apiClientRepository.existsByTokenId(tokenId)) {
        return tokenId;
      }
    }
    throw new IllegalStateException("Unable to generate unique token id");
  }

  private String generateTokenPart(int bytes) {
    byte[] randomBytes = new byte[bytes];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String composeToken(String tokenId, String secret) {
    return tokenId + "." + secret;
  }

  private void validateRoles(Set<ApiClientRole> roles) {
    if (roles == null || roles.isEmpty()) {
      throw new IllegalArgumentException("Api client must have at least one role");
    }
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Api client name cannot be blank");
    }
  }

  private record ParsedToken(String tokenId, String secret) {}

  public record IssuedToken(ApiClient client, String bearerToken) {}

  public record AuthenticatedApiClient(
      ApiClient client, Collection<GrantedAuthority> authorities) {}
}
