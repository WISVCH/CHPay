package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Removes the deprecated RFID column from users. */
public class V20260423_1__Remove_rfid_column extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute("ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS rfid CASCADE");
    }
  }
}
