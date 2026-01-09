package ch.wisv.chpay.core.repository;

import java.util.UUID;

public interface ConfettiUsageCount {
  UUID getConfettiId();

  long getUserCount();
}
