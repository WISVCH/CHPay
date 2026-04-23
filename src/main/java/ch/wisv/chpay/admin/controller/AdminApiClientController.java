package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.api.client.model.ApiClient;
import ch.wisv.chpay.api.client.model.ApiClientRole;
import ch.wisv.chpay.api.client.service.ApiClientService;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api-clients")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminApiClientController {

  private final ApiClientService apiClientService;

  public AdminApiClientController(ApiClientService apiClientService) {
    this.apiClientService = apiClientService;
  }

  @GetMapping
  public List<ApiClientSummary> listClients() {
    return apiClientService.findAllClients().stream().map(this::toSummary).toList();
  }

  @PostMapping
  public ResponseEntity<ApiClientSecretResponse> createClient(
      @RequestBody CreateApiClientRequest request) {
    ApiClientService.IssuedToken issuedToken = apiClientService.createClient(request.name(), request.roles());
    return ResponseEntity.status(HttpStatus.CREATED).body(toSecretResponse(issuedToken));
  }

  @PostMapping("/{id}/rotate-token")
  public ApiClientSecretResponse rotateToken(@PathVariable UUID id) {
    return toSecretResponse(apiClientService.rotateToken(id));
  }

  @PostMapping("/{id}/disable")
  public ApiClientSummary disableClient(@PathVariable UUID id) {
    return toSummary(apiClientService.setEnabled(id, false));
  }

  @PostMapping("/{id}/enable")
  public ApiClientSummary enableClient(@PathVariable UUID id) {
    return toSummary(apiClientService.setEnabled(id, true));
  }

  @PutMapping("/{id}/roles")
  public ApiClientSummary updateRoles(
      @PathVariable UUID id, @RequestBody UpdateApiClientRolesRequest request) {
    return toSummary(apiClientService.updateRoles(id, request.roles()));
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  private ApiClientSummary toSummary(ApiClient apiClient) {
    return new ApiClientSummary(
        apiClient.getId(),
        apiClient.getName(),
        apiClient.getTokenId(),
        apiClient.isEnabled(),
        apiClient.getRoles(),
        apiClient.getCreatedAt(),
        apiClient.getUpdatedAt(),
        apiClient.getLastUsedAt());
  }

  private ApiClientSecretResponse toSecretResponse(ApiClientService.IssuedToken issuedToken) {
    ApiClient client = issuedToken.client();
    return new ApiClientSecretResponse(
        client.getId(),
        client.getName(),
        client.getTokenId(),
        issuedToken.bearerToken(),
        client.isEnabled(),
        client.getRoles(),
        client.getCreatedAt(),
        client.getUpdatedAt(),
        client.getLastUsedAt());
  }

  public record CreateApiClientRequest(String name, Set<ApiClientRole> roles) {}

  public record UpdateApiClientRolesRequest(Set<ApiClientRole> roles) {}

  public record ApiClientSummary(
      UUID id,
      String name,
      String tokenId,
      boolean enabled,
      Set<ApiClientRole> roles,
      Instant createdAt,
      Instant updatedAt,
      Instant lastUsedAt) {}

  public record ApiClientSecretResponse(
      UUID id,
      String name,
      String tokenId,
      String bearerToken,
      boolean enabled,
      Set<ApiClientRole> roles,
      Instant createdAt,
      Instant updatedAt,
      Instant lastUsedAt) {}
}
