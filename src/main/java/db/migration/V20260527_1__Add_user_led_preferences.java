package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds LED color and pattern preferences to users. */
public class V20260527_1__Add_user_led_preferences extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          ALTER TABLE users
            ADD COLUMN IF NOT EXISTS led_r INTEGER,
            ADD COLUMN IF NOT EXISTS led_g INTEGER,
            ADD COLUMN IF NOT EXISTS led_b INTEGER,
            ADD COLUMN IF NOT EXISTS led_pattern VARCHAR(32)
          """);
    }
  }
}
