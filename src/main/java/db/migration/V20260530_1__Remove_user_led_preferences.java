package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20260530_1__Remove_user_led_preferences extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          ALTER TABLE users
            DROP COLUMN IF EXISTS led_r,
            DROP COLUMN IF EXISTS led_g,
            DROP COLUMN IF EXISTS led_b,
            DROP COLUMN IF EXISTS led_pattern
          """);
    }
  }
}
