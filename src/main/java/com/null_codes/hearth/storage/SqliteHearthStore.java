package com.null_codes.hearth.storage;

import com.null_codes.hearth.model.BlockSnapshot;
import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.model.PropertyChange;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;

/**
 * Stores Hearth state in one local SQLite database.
 *
 * <p>Writes intentionally remain synchronous so early profiles show the real baseline cost.
 */
public final class SqliteHearthStore implements PropertyStore, PropertyChangeStore, AutoCloseable {

  private final Connection connection;

  public SqliteHearthStore(Path path) {
    try {
      connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
      createSchema();
    } catch (SQLException exception) {
      throw failure("open database", exception);
    }
  }

  private void createSchema() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          CREATE TABLE IF NOT EXISTS properties (
            uuid TEXT PRIMARY KEY, owner TEXT NOT NULL, name TEXT NOT NULL, world TEXT NOT NULL,
            min_x REAL NOT NULL, min_y REAL NOT NULL, min_z REAL NOT NULL,
            max_x REAL NOT NULL, max_y REAL NOT NULL, max_z REAL NOT NULL,
            created_at INTEGER NOT NULL
          )
          """);
      statement.executeUpdate(
          """
          CREATE TABLE IF NOT EXISTS property_changes (
            uuid TEXT PRIMARY KEY, property_uuid TEXT NOT NULL, changed_at TEXT NOT NULL,
            player_uuid TEXT, cause TEXT NOT NULL,
            before_material TEXT NOT NULL, before_x INTEGER NOT NULL, before_y INTEGER NOT NULL,
            before_z INTEGER NOT NULL, before_world TEXT, before_data TEXT NOT NULL,
            after_material TEXT NOT NULL, after_x INTEGER NOT NULL, after_y INTEGER NOT NULL,
            after_z INTEGER NOT NULL, after_world TEXT, after_data TEXT NOT NULL
          )
          """);
    }
  }

  @Override
  public List<Property> loadProperties() {
    List<Property> properties = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery("SELECT * FROM properties ORDER BY rowid")) {
      while (rows.next()) {
        properties.add(
            new Property(
                UUID.fromString(rows.getString("uuid")),
                UUID.fromString(rows.getString("owner")),
                rows.getString("name"),
                UUID.fromString(rows.getString("world")),
                new BoundingBox(
                    rows.getDouble("min_x"),
                    rows.getDouble("min_y"),
                    rows.getDouble("min_z"),
                    rows.getDouble("max_x"),
                    rows.getDouble("max_y"),
                    rows.getDouble("max_z")),
                rows.getLong("created_at")));
      }
      return List.copyOf(properties);
    } catch (SQLException exception) {
      throw failure("load properties", exception);
    }
  }

  @Override
  public void insert(Property property) {
    writeProperty(
        """
        INSERT INTO properties
          (uuid, owner, name, world, min_x, min_y, min_z, max_x, max_y, max_z, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        property);
  }

  @Override
  public void update(Property property) {
    writeProperty(
        """
        UPDATE properties SET owner=?, name=?, world=?, min_x=?, min_y=?, min_z=?,
          max_x=?, max_y=?, max_z=?, created_at=? WHERE uuid=?
        """,
        property);
  }

  private void writeProperty(String sql, Property property) {
    BoundingBox region = property.region();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      boolean update = sql.startsWith("UPDATE");
      int offset = update ? 0 : 1;
      if (!update) statement.setString(1, property.uuid().toString());
      statement.setString(1 + offset, property.owner().toString());
      statement.setString(2 + offset, property.name());
      statement.setString(3 + offset, property.world().toString());
      statement.setDouble(4 + offset, region.getMinX());
      statement.setDouble(5 + offset, region.getMinY());
      statement.setDouble(6 + offset, region.getMinZ());
      statement.setDouble(7 + offset, region.getMaxX());
      statement.setDouble(8 + offset, region.getMaxY());
      statement.setDouble(9 + offset, region.getMaxZ());
      statement.setLong(10 + offset, property.timestamp());
      if (update) statement.setString(11, property.uuid().toString());
      if (statement.executeUpdate() != 1) {
        throw new StorageException("Could not persist property " + property.uuid() + ".", null);
      }
    } catch (SQLException exception) {
      throw failure("persist property " + property.uuid(), exception);
    }
  }

  @Override
  public void delete(UUID propertyUuid) {
    try (PreparedStatement statement =
        connection.prepareStatement("DELETE FROM properties WHERE uuid=?")) {
      statement.setString(1, propertyUuid.toString());
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw failure("delete property " + propertyUuid, exception);
    }
  }

  @Override
  public List<PropertyChange> loadChanges() {
    List<PropertyChange> changes = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery("SELECT * FROM property_changes ORDER BY rowid")) {
      while (rows.next()) {
        String playerUuid = rows.getString("player_uuid");
        changes.add(
            new PropertyChange(
                UUID.fromString(rows.getString("uuid")),
                UUID.fromString(rows.getString("property_uuid")),
                Instant.parse(rows.getString("changed_at")),
                playerUuid == null ? null : UUID.fromString(playerUuid),
                PropertyChange.ChangeCause.valueOf(rows.getString("cause")),
                readSnapshot(rows, "before"),
                readSnapshot(rows, "after")));
      }
      return List.copyOf(changes);
    } catch (SQLException exception) {
      throw failure("load property changes", exception);
    }
  }

  private static BlockSnapshot readSnapshot(ResultSet rows, String prefix) throws SQLException {
    String worldUuid = rows.getString(prefix + "_world");
    return new BlockSnapshot(
        Material.valueOf(rows.getString(prefix + "_material")),
        rows.getInt(prefix + "_x"),
        rows.getInt(prefix + "_y"),
        rows.getInt(prefix + "_z"),
        worldUuid == null ? null : UUID.fromString(worldUuid),
        rows.getString(prefix + "_data"));
  }

  @Override
  public void insert(PropertyChange change) {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO property_changes VALUES
              (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
      statement.setString(1, change.uuid().toString());
      statement.setString(2, change.propertyUuid().toString());
      statement.setString(3, change.timestamp().toString());
      if (change.playerUuid() == null) statement.setNull(4, Types.VARCHAR);
      else statement.setString(4, change.playerUuid().toString());
      statement.setString(5, change.cause().name());
      writeSnapshot(statement, 6, change.before());
      writeSnapshot(statement, 12, change.after());
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw failure("persist property change " + change.uuid(), exception);
    }
  }

  private static void writeSnapshot(PreparedStatement statement, int offset, BlockSnapshot snapshot)
      throws SQLException {
    statement.setString(offset, snapshot.material().name());
    statement.setInt(offset + 1, snapshot.x());
    statement.setInt(offset + 2, snapshot.y());
    statement.setInt(offset + 3, snapshot.z());
    if (snapshot.worldUuid() == null) statement.setNull(offset + 4, Types.VARCHAR);
    else statement.setString(offset + 4, snapshot.worldUuid().toString());
    statement.setString(offset + 5, snapshot.blockData());
  }

  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException exception) {
      throw failure("close database", exception);
    }
  }

  private static StorageException failure(String operation, SQLException cause) {
    return new StorageException("Could not " + operation + ".", cause);
  }
}
