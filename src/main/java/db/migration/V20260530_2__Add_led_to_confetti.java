package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20260530_2__Add_led_to_confetti extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          ALTER TABLE confetti
            ADD COLUMN IF NOT EXISTS led_color VARCHAR(7) NOT NULL DEFAULT '#ffffff',
            ADD COLUMN IF NOT EXISTS led_pattern VARCHAR(32) NOT NULL DEFAULT 'oplopen'
          """);

      stmt.execute(
          """
          UPDATE confetti c
          SET led_color = (
            SELECT cc.color
            FROM confetti_colors cc
            WHERE cc.confetti_id = c.id
            ORDER BY cc.ctid
            LIMIT 1
          )
          WHERE EXISTS (
            SELECT 1 FROM confetti_colors cc WHERE cc.confetti_id = c.id
          )
          """);

      stmt.execute(
          """
          ALTER TABLE confetti
            ALTER COLUMN led_color DROP DEFAULT,
            ALTER COLUMN led_pattern DROP DEFAULT
          """);
    }
  }
}
