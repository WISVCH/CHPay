package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds nullable API client reference to external transactions. */
public class V20260423_3__Add_external_transaction_api_client_reference extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          ALTER TABLE IF EXISTS external_transaction
          ADD COLUMN IF NOT EXISTS api_client_id UUID
          """);

      stmt.execute(
          """
          UPDATE external_transaction
          SET api_client_id = NULL
          WHERE api_client_id IS NOT NULL
          """);

      stmt.execute(
          """
          CREATE INDEX IF NOT EXISTS idx_external_transaction_api_client_id
          ON external_transaction(api_client_id)
          """);

      stmt.execute(
          """
          DO $$
          BEGIN
            IF NOT EXISTS (
              SELECT 1 FROM information_schema.table_constraints
              WHERE constraint_name = 'fk_external_transaction_api_client'
                AND table_name = 'external_transaction'
            ) THEN
              ALTER TABLE external_transaction
              ADD CONSTRAINT fk_external_transaction_api_client
              FOREIGN KEY (api_client_id) REFERENCES api_clients(id)
              ON DELETE SET NULL;
            END IF;
          END$$;
          """);
    }
  }
}
