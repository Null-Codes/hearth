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
  void propertiesSurviveCreateUpdateAndRemove() {
    UUID propertyUuid = UUID.randomUUID();
    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("properties.db"))) {
      PropertyManager manager = new PropertyManager(store);
      Property original =
          new Property(
              propertyUuid,
              UUID.randomUUID(),
              "Original",
              UUID.randomUUID(),
              new BoundingBox(0, 0, 0, 10, 10, 10),
              100);
      manager.register(original);
      manager.update(
          new Property(
              original.uuid(),
              original.owner(),
              "Renamed",
              original.world(),
              original.region(),
              original.timestamp()));
    }

    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("properties.db"))) {
      PropertyManager manager = new PropertyManager(store);
      assertEquals("Renamed", manager.get(propertyUuid).orElseThrow().name());
      assertTrue(manager.remove(propertyUuid));
    }

    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("properties.db"))) {
      assertTrue(new PropertyManager(store).getProperties().isEmpty());
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
      new PropertyChangeManager(store).record(change);
    }

    try (SqliteHearthStore store = new SqliteHearthStore(directory.resolve("history.db"))) {
      assertEquals(
          java.util.List.of(change), new PropertyChangeManager(store).getChanges(propertyUuid));
    }
  }
}
