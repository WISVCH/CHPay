package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds expire_at to payment requests. */
public class V20260104__Add_request_expire_at extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute("ALTER TABLE IF EXISTS requests ADD COLUMN IF NOT EXISTS expire_at DATE");

      stmt.execute(
          """
                            UPDATE requests
                            SET expire_at = (created_at::date + INTERVAL '1 month')
                            WHERE expire_at IS NULL
                            """);

      stmt.execute("ALTER TABLE IF EXISTS requests ALTER COLUMN expire_at SET NOT NULL");

      stmt.execute("ALTER TABLE IF EXISTS requests DROP COLUMN IF EXISTS expired");
    }
  }
}
