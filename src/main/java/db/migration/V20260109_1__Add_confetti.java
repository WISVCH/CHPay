package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds confetti configuration tables and fields. */
public class V20260109_1__Add_confetti extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS confetti (
            id UUID PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            created_at TIMESTAMP NOT NULL,
            scalar DOUBLE PRECISION NOT NULL DEFAULT 1.0,
            minimum_transactions INTEGER NOT NULL DEFAULT 0,
            group_name VARCHAR(255),
            hidden BOOLEAN NOT NULL DEFAULT FALSE
          )
          """);

      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS confetti_colors (
            confetti_id UUID NOT NULL,
            color VARCHAR(32) NOT NULL,
            CONSTRAINT fk_confetti_colors_confetti
              FOREIGN KEY (confetti_id) REFERENCES confetti(id) ON DELETE CASCADE
          )
          """);

      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS confetti_shapes (
            confetti_id UUID NOT NULL,
            shape_type VARCHAR(16) NOT NULL,
            shape_value TEXT,
            CONSTRAINT fk_confetti_shapes_confetti
              FOREIGN KEY (confetti_id) REFERENCES confetti(id) ON DELETE CASCADE
          )
          """);
    }
  }
}
