package com.null_codes.hearth.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.null_codes.hearth.model.BlockSnapshot;
import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.model.PropertyChange;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteHearthStoreTest {

  @TempDir Path directory;

  @Test
  void databaseOperationsRunOnDedicatedWorker() {
    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("thread.db"))) {
      String threadName =
          store.loadProperties().thenApply(ignored -> Thread.currentThread().getName()).join();

      assertEquals("Hearth Database", threadName);
    }
  }

  @Test
  void propertiesSurviveCreateUpdateAndRemove() {
    UUID propertyUuid = UUID.randomUUID();
    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("properties.db"))) {
      PropertyManager manager = new PropertyManager(store);
      manager.ready().join();
      Property original =
          new Property(
              propertyUuid,
              UUID.randomUUID(),
              "Original",
              UUID.randomUUID(),
              new BoundingBox(0, 0, 0, 10, 10, 10),
              100);
      manager.register(original).join();
      manager
          .update(
              new Property(
                  original.uuid(),
                  original.owner(),
                  "Renamed",
                  original.world(),
                  original.region(),
                  original.timestamp()))
          .join();
    }

    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("properties.db"))) {
      PropertyManager manager = new PropertyManager(store);
      manager.ready().join();
      assertEquals("Renamed", manager.get(propertyUuid).orElseThrow().name());
      assertTrue(manager.remove(propertyUuid).join());
    }

    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("properties.db"))) {
      PropertyManager manager = new PropertyManager(store);
      manager.ready().join();
      assertTrue(manager.getProperties().isEmpty());
    }
  }

  @Test
  void propertyHistoryRoundTripsInRecordingOrder() {
    UUID propertyUuid = UUID.randomUUID();
    UUID worldUuid = UUID.randomUUID();
    PropertyChange change =
        new PropertyChange(
            UUID.randomUUID(),
            propertyUuid,
            Instant.parse("2026-07-15T12:00:00Z"),
            null,
            PropertyChange.ChangeCause.EXPLOSION,
            new BlockSnapshot(Material.STONE, 1, 2, 3, worldUuid, "minecraft:stone"),
            BlockSnapshot.airAt(worldUuid, 1, 2, 3));

    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("history.db"))) {
      PropertyChangeManager manager = new PropertyChangeManager(store);
      manager.ready().join();
      manager.record(change).join();
    }

    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("history.db"))) {
      PropertyChangeManager manager = new PropertyChangeManager(store);
      manager.ready().join();
      assertEquals(java.util.List.of(change), manager.getChanges(propertyUuid));
    }
  }
}
