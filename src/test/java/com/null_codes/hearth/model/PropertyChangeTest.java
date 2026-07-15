package com.null_codes.hearth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PropertyChangeTest {

  private BlockSnapshot snapshot(Material material) {
    return new BlockSnapshot(material, 10, 64, -20, null, material.getKey().toString());
  }

  @Test
  void storesPlayerPlacementChange() {
    UUID changeUuid = UUID.randomUUID();
    UUID propertyUuid = UUID.randomUUID();
    UUID playerUuid = UUID.randomUUID();
    Instant timestamp = Instant.parse("2026-07-14T12:00:00Z");

    BlockSnapshot before = new BlockSnapshot(Material.AIR, 10, 64, -20, null, "minecraft:air");

    BlockSnapshot after = new BlockSnapshot(Material.STONE, 10, 64, -20, null, "minecraft:stone");

    PropertyChange change =
        new PropertyChange(
            changeUuid,
            propertyUuid,
            timestamp,
            playerUuid,
            PropertyChange.ChangeCause.PLAYER_PLACE,
            before,
            after);

    assertEquals(changeUuid, change.uuid());
    assertEquals(propertyUuid, change.propertyUuid());
    assertEquals(timestamp, change.timestamp());
    assertEquals(playerUuid, change.playerUuid());
    assertEquals(PropertyChange.ChangeCause.PLAYER_PLACE, change.cause());
    assertEquals(before, change.before());
    assertEquals(after, change.after());
  }

  @Test
  void playerPlacementTransitionsFromAirToPlacedBlock() {
    BlockSnapshot before = new BlockSnapshot(Material.AIR, 10, 64, -20, null, "minecraft:air");

    BlockSnapshot after = new BlockSnapshot(Material.STONE, 10, 64, -20, null, "minecraft:stone");

    PropertyChange change =
        new PropertyChange(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            UUID.randomUUID(),
            PropertyChange.ChangeCause.PLAYER_PLACE,
            before,
            after);

    assertEquals(Material.AIR, change.before().material());
    assertEquals(Material.STONE, change.after().material());
    assertEquals(PropertyChange.ChangeCause.PLAYER_PLACE, change.cause());
  }

  @Test
  void playerBreakTransitionsFromBlockToAir() {
    BlockSnapshot before =
        new BlockSnapshot(Material.OAK_LOG, 10, 64, -20, null, "minecraft:oak_log[axis=y]");

    BlockSnapshot after = new BlockSnapshot(Material.AIR, 10, 64, -20, null, "minecraft:air");

    PropertyChange change =
        new PropertyChange(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            UUID.randomUUID(),
            PropertyChange.ChangeCause.PLAYER_BREAK,
            before,
            after);

    assertEquals(Material.OAK_LOG, change.before().material());
    assertEquals(Material.AIR, change.after().material());
    assertEquals(PropertyChange.ChangeCause.PLAYER_BREAK, change.cause());
  }

  @Test
  void environmentalChangeCanHaveNoPlayer() {
    BlockSnapshot before =
        new BlockSnapshot(Material.OAK_PLANKS, 10, 64, -20, null, "minecraft:oak_planks");

    BlockSnapshot after = new BlockSnapshot(Material.AIR, 10, 64, -20, null, "minecraft:air");

    PropertyChange change =
        new PropertyChange(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            null,
            PropertyChange.ChangeCause.FIRE,
            before,
            after);

    assertNull(change.playerUuid());
    assertEquals(PropertyChange.ChangeCause.FIRE, change.cause());
  }

  @Test
  void replacementPreservesBeforeAndAfterMaterials() {
    BlockSnapshot before = new BlockSnapshot(Material.STONE, 10, 64, -20, null, "minecraft:stone");

    BlockSnapshot after = new BlockSnapshot(Material.DIRT, 10, 64, -20, null, "minecraft:dirt");

    PropertyChange change =
        new PropertyChange(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            UUID.randomUUID(),
            PropertyChange.ChangeCause.PLAYER_PLACE,
            before,
            after);

    assertEquals(Material.STONE, change.before().material());
    assertEquals(Material.DIRT, change.after().material());
  }

  @Test
  void beforeAndAfterSnapshotsUseSameCoordinates() {
    BlockSnapshot before = new BlockSnapshot(Material.STONE, 10, 64, -20, null, "minecraft:stone");

    BlockSnapshot after = new BlockSnapshot(Material.AIR, 10, 64, -20, null, "minecraft:air");

    PropertyChange change =
        new PropertyChange(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now(),
            UUID.randomUUID(),
            PropertyChange.ChangeCause.PLAYER_BREAK,
            before,
            after);

    assertEquals(change.before().x(), change.after().x());
    assertEquals(change.before().y(), change.after().y());
    assertEquals(change.before().z(), change.after().z());
  }

  @Test
  void timestampIsPreservedExactly() {
    Instant timestamp = Instant.parse("2026-07-14T19:45:30.123456Z");

    PropertyChange change =
        new PropertyChange(
            UUID.randomUUID(),
            UUID.randomUUID(),
            timestamp,
            null,
            PropertyChange.ChangeCause.EXPLOSION,
            snapshot(Material.STONE),
            snapshot(Material.AIR));

    assertEquals(timestamp, change.timestamp());
  }
}
