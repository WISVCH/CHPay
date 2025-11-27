package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Adds redirect_payment_id FK to topup_transaction and removes the deprecated recent_payment column
 * from users.
 */
public class V20241122__Add_redirect_payment_fk_and_drop_recent_payment extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute("ALTER TABLE IF EXISTS users DROP COLUMN IF EXISTS recent_payment");

      stmt.execute(
          "ALTER TABLE IF EXISTS topup_transaction "
              + "ADD COLUMN IF NOT EXISTS redirect_payment_id UUID");

      stmt.execute(
          "CREATE INDEX IF NOT EXISTS idx_topup_redirect_payment "
              + "ON topup_transaction(redirect_payment_id)");

      stmt.execute(
          """
          DO $$
          BEGIN
            IF NOT EXISTS (
              SELECT 1 FROM information_schema.table_constraints
              WHERE constraint_name = 'fk_topup_redirect_transaction'
                AND table_name = 'topup_transaction'
            ) THEN
              ALTER TABLE topup_transaction
              ADD CONSTRAINT fk_topup_redirect_transaction
              FOREIGN KEY (redirect_payment_id) REFERENCES transactions(id);
            END IF;
          END$$;
          """);
    }
  }
}
