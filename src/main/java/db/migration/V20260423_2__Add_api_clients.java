package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds API clients with role-based bearer token authorization support. */
public class V20260423_2__Add_api_clients extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS api_clients (
            id UUID PRIMARY KEY,
            name VARCHAR(255) NOT NULL UNIQUE,
            token_id VARCHAR(128) NOT NULL UNIQUE,
            token_hash VARCHAR(255) NOT NULL,
            enabled BOOLEAN NOT NULL DEFAULT TRUE,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            last_used_at TIMESTAMPTZ
          )
          """);

      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS api_client_roles (
            api_client_id UUID NOT NULL,
            role VARCHAR(64) NOT NULL,
            CONSTRAINT fk_api_client_roles_client
              FOREIGN KEY (api_client_id) REFERENCES api_clients(id) ON DELETE CASCADE,
            CONSTRAINT uq_api_client_roles UNIQUE (api_client_id, role)
          )
          """);

      stmt.execute(
          """
          CREATE INDEX IF NOT EXISTS idx_api_clients_name
          ON api_clients(name)
          """);

      stmt.execute(
          """
          CREATE INDEX IF NOT EXISTS idx_api_clients_token_id
          ON api_clients(token_id)
          """);

      stmt.execute(
          """
          CREATE INDEX IF NOT EXISTS idx_api_client_roles_client_id
          ON api_client_roles(api_client_id)
          """);
    }
  }
}
