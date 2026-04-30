package db.migration;

import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Updates transactions status check constraint to allow CANCELLED. */
public class V20260430_1__Allow_cancelled_transaction_status extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          ALTER TABLE IF EXISTS transactions
          DROP CONSTRAINT IF EXISTS transactions_status_check
          """);

      stmt.execute(
          """
          ALTER TABLE transactions
          ADD CONSTRAINT transactions_status_check
          CHECK (status IN ('SUCCESSFUL', 'PENDING', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'CANCELLED'))
          """);
    }
  }
}
