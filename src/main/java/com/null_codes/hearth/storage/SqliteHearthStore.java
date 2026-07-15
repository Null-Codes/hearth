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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;

/** Stores Hearth state through an ordered, dedicated SQLite worker. */
public final class SqliteHearthStore implements PropertyStore, PropertyChangeStore, AutoCloseable {

  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          Thread.ofPlatform().name("Hearth Database").daemon(false).factory());
  private final CompletableFuture<Connection> connection;
  private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
  private boolean closing;

  public SqliteHearthStore(Path path) {
    connection =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                Connection opened =
                    DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
                createSchema(opened);
                return opened;
              } catch (SQLException exception) {
                throw failure("open database", exception);
              }
            },
            executor);
  }

  private static void createSchema(Connection connection) throws SQLException {
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
  public CompletableFuture<List<Property>> loadProperties() {
    return submit(
        "load properties",
        database -> {
          List<Property> properties = new ArrayList<>();
          try (Statement statement = database.createStatement();
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
          }
          return List.copyOf(properties);
        });
  }

  @Override
  public CompletableFuture<Void> insert(Property property) {
    return submit(
        "persist property " + property.uuid(),
        database -> {
          writeProperty(
              database,
              """
              INSERT INTO properties
                (uuid, owner, name, world, min_x, min_y, min_z, max_x, max_y, max_z, created_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              """,
              property);
          return null;
        });
  }

  @Override
  public CompletableFuture<Void> update(Property property) {
    return submit(
        "persist property " + property.uuid(),
        database -> {
          writeProperty(
              database,
              """
              UPDATE properties SET owner=?, name=?, world=?, min_x=?, min_y=?, min_z=?,
                max_x=?, max_y=?, max_z=?, created_at=? WHERE uuid=?
              """,
              property);
          return null;
        });
  }

  private static void writeProperty(Connection connection, String sql, Property property)
      throws SQLException {
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
    }
  }

  @Override
  public CompletableFuture<Void> delete(UUID propertyUuid) {
    return submit(
        "delete property " + propertyUuid,
        database -> {
          try (PreparedStatement statement =
              database.prepareStatement("DELETE FROM properties WHERE uuid=?")) {
            statement.setString(1, propertyUuid.toString());
            statement.executeUpdate();
          }
          return null;
        });
  }

  @Override
  public CompletableFuture<List<PropertyChange>> loadChanges() {
    return submit(
        "load property changes",
        database -> {
          List<PropertyChange> changes = new ArrayList<>();
          try (Statement statement = database.createStatement();
              ResultSet rows =
                  statement.executeQuery("SELECT * FROM property_changes ORDER BY rowid")) {
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
          }
          return List.copyOf(changes);
        });
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
  public CompletableFuture<Void> insert(PropertyChange change) {
    return submit(
        "persist property change " + change.uuid(),
        database -> {
          try (PreparedStatement statement =
              database.prepareStatement(
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
          }
          return null;
        });
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

  public synchronized CompletableFuture<Void> closeAsync() {
    if (closing) return tail;
    closing = true;
    CompletableFuture<Void> closed =
        submitInternal(
            "close database",
            database -> {
              database.close();
              return null;
            });
    closed.whenComplete((ignored, failure) -> executor.shutdown());
    return closed;
  }

  @Override
  public void close() {
    closeAsync().join();
  }

  private synchronized <T> CompletableFuture<T> submit(
      String operation, DatabaseOperation<T> task) {
    if (closing) {
      return CompletableFuture.failedFuture(
          new StorageException("Database is closing; could not " + operation + ".", null));
    }
    return submitInternal(operation, task);
  }

  private <T> CompletableFuture<T> submitInternal(String operation, DatabaseOperation<T> task) {
    CompletableFuture<T> submitted =
        tail.handle((ignored, previousFailure) -> null)
            .thenCompose(ignored -> connection)
            .thenApplyAsync(
                database -> {
                  try {
                    return task.execute(database);
                  } catch (SQLException exception) {
                    throw failure(operation, exception);
                  }
                },
                executor);
    tail = submitted.handle((ignored, failure) -> null);
    return submitted;
  }

  private static StorageException failure(String operation, SQLException cause) {
    return new StorageException("Could not " + operation + ".", cause);
  }

  @FunctionalInterface
  private interface DatabaseOperation<T> {
    T execute(Connection connection) throws SQLException;
  }
}
