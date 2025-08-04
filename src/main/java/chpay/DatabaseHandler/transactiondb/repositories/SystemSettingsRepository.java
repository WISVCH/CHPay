package chpay.DatabaseHandler.transactiondb.repositories;

import chpay.DatabaseHandler.transactiondb.entities.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Integer> {}
