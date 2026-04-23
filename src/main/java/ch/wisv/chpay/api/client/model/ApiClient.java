package ch.wisv.chpay.api.client.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "api_clients",
    indexes = {
      @Index(name = "idx_api_clients_name", columnList = "name", unique = true),
      @Index(name = "idx_api_clients_token_id", columnList = "token_id", unique = true)
    })
@Getter
@NoArgsConstructor
public class ApiClient {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Setter
  @Column(nullable = false, unique = true)
  private String name;

  @Setter
  @Column(name = "token_id", nullable = false, unique = true)
  private String tokenId;

  @Setter
  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;

  @Setter
  @Column(nullable = false)
  private boolean enabled = true;

  @Setter
  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "api_client_roles", joinColumns = @JoinColumn(name = "api_client_id"))
  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private Set<ApiClientRole> roles = EnumSet.noneOf(ApiClientRole.class);

  public ApiClient(String name, String tokenId, String tokenHash, Set<ApiClientRole> roles) {
    this.name = name;
    this.tokenId = tokenId;
    this.tokenHash = tokenHash;
    setRoles(roles);
    this.enabled = true;
  }

  public void setRoles(Set<ApiClientRole> roles) {
    this.roles.clear();
    if (roles != null) {
      this.roles.addAll(roles);
    }
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
