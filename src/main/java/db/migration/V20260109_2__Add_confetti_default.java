package db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds default flag and seeds initial default confetti configuration. */
public class V20260109_2__Add_confetti_default extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    try (Statement stmt = context.getConnection().createStatement()) {
      stmt.execute(
          """
          ALTER TABLE confetti
            ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE
          """);
    }

    long confettiCount;
    try (Statement stmt = context.getConnection().createStatement()) {
      try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM confetti")) {
        rs.next();
        confettiCount = rs.getLong(1);
      }
    }

    UUID keepId = null;
    if (confettiCount == 0) {
      keepId = insertDefaultConfetti(context);
    } else {
      try (PreparedStatement ps =
          context
              .getConnection()
              .prepareStatement(
                  "SELECT id FROM confetti WHERE is_default = TRUE ORDER BY created_at")) {
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            keepId = (UUID) rs.getObject(1);
          }
        }
      }

      if (keepId == null) {
        try (PreparedStatement ps =
            context
                .getConnection()
                .prepareStatement("SELECT id FROM confetti ORDER BY created_at LIMIT 1")) {
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              keepId = (UUID) rs.getObject(1);
            }
          }
        }
      }
    }

    if (keepId != null) {
      try (PreparedStatement ps =
          context
              .getConnection()
              .prepareStatement("UPDATE confetti SET is_default = FALSE")) {
        ps.executeUpdate();
      }
      try (PreparedStatement ps =
          context
              .getConnection()
              .prepareStatement("UPDATE confetti SET is_default = TRUE WHERE id = ?")) {
        ps.setObject(1, keepId);
        ps.executeUpdate();
      }
    }
  }

  private UUID insertDefaultConfetti(Context context) throws Exception {
    UUID confettiId = UUID.randomUUID();
    Timestamp now = Timestamp.valueOf(LocalDateTime.now());

    try (PreparedStatement ps =
        context
            .getConnection()
            .prepareStatement(
                """
                INSERT INTO confetti
                  (id, name, created_at, scalar, minimum_transactions, group_name, hidden, is_default)
                VALUES
                  (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      ps.setObject(1, confettiId);
      ps.setString(2, "Default Confetti");
      ps.setTimestamp(3, now);
      ps.setDouble(4, 1.0);
      ps.setInt(5, 0);
      ps.setString(6, null);
      ps.setBoolean(7, false);
      ps.setBoolean(8, true);
      ps.executeUpdate();
    }

    List<String> colors =
        List.of(
            "#26ccff",
            "#a25afd",
            "#ff5e7e",
            "#88ff5a",
            "#fcff42",
            "#ffa62d",
            "#ff36ff");

    try (PreparedStatement ps =
        context
            .getConnection()
            .prepareStatement(
                "INSERT INTO confetti_colors (confetti_id, color) VALUES (?, ?)")) {
      for (String color : colors) {
        ps.setObject(1, confettiId);
        ps.setString(2, color);
        ps.addBatch();
      }
      ps.executeBatch();
    }

    try (PreparedStatement ps =
        context
            .getConnection()
            .prepareStatement(
                "INSERT INTO confetti_shapes (confetti_id, shape_type, shape_value) VALUES (?, ?, ?)")) {
      ps.setObject(1, confettiId);
      ps.setString(2, "SQUARE");
      ps.setString(3, null);
      ps.addBatch();

      ps.setObject(1, confettiId);
      ps.setString(2, "CIRCLE");
      ps.setString(3, null);
      ps.addBatch();

      ps.executeBatch();
    }

    return confettiId;
  }
}
