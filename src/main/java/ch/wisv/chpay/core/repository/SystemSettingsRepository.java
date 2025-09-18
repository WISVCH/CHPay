package ch.wisv.chpay.core.repository;

import ch.wisv.chpay.core.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Integer> {}
