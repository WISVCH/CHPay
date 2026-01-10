package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds confetti selection and group assignments to users. */
public class V20260109_3__Add_user_confetti_groups extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          ALTER TABLE users
            ADD COLUMN IF NOT EXISTS confetti_id UUID
          """);

      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS user_groups (
            user_id UUID NOT NULL,
            group_name VARCHAR(255) NOT NULL,
            CONSTRAINT fk_user_groups_user
              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
          )
          """);

      stmt.execute(
          """
          DO $$
          BEGIN
            ALTER TABLE users
              ADD CONSTRAINT fk_users_confetti
                FOREIGN KEY (confetti_id) REFERENCES confetti(id);
          EXCEPTION
            WHEN duplicate_object THEN
              NULL;
          END
          $$;
          """);

      stmt.execute(
          """
          CREATE INDEX IF NOT EXISTS idx_users_confetti_id
          ON users(confetti_id)
          """);
    }
  }
}
