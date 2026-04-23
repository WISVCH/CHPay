package ch.wisv.chpay.api.client.repository;

import ch.wisv.chpay.api.client.model.ApiClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {
  Optional<ApiClient> findByTokenIdAndEnabledTrue(String tokenId);

  boolean existsByNameIgnoreCase(String name);

  boolean existsByTokenId(String tokenId);

  List<ApiClient> findAllByOrderByNameAsc();
}
