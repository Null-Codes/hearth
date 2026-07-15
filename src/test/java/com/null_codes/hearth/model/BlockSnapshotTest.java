package com.null_codes.hearth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class BlockSnapshotTest {

  @Test
  void storesMaterialCoordinatesAndBlockData() {
    BlockSnapshot snapshot =
        new BlockSnapshot(Material.STONE, 10, 64, -20, null, "minecraft:stone");

    assertEquals(Material.STONE, snapshot.material());
    assertEquals(10, snapshot.x());
    assertEquals(64, snapshot.y());
    assertEquals(-20, snapshot.z());
    assertNull(snapshot.worldUuid());
    assertEquals("minecraft:stone", snapshot.blockData());
  }

  @Test
  void equalSnapshotsAreEqual() {
    BlockSnapshot first =
        new BlockSnapshot(Material.OAK_LOG, 5, 70, 12, null, "minecraft:oak_log[axis=y]");

    BlockSnapshot second =
        new BlockSnapshot(Material.OAK_LOG, 5, 70, 12, null, "minecraft:oak_log[axis=y]");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void snapshotsWithDifferentMaterialsAreNotEqual() {
    BlockSnapshot stone = new BlockSnapshot(Material.STONE, 5, 70, 12, null, "minecraft:stone");

    BlockSnapshot dirt = new BlockSnapshot(Material.DIRT, 5, 70, 12, null, "minecraft:dirt");

    assertNotEquals(stone, dirt);
  }

  @Test
  void snapshotsWithDifferentCoordinatesAreNotEqual() {
    BlockSnapshot first = new BlockSnapshot(Material.STONE, 5, 70, 12, null, "minecraft:stone");

    BlockSnapshot second = new BlockSnapshot(Material.STONE, 6, 70, 12, null, "minecraft:stone");

    assertNotEquals(first, second);
  }

  @Test
  void snapshotsWithDifferentBlockDataAreNotEqual() {
    BlockSnapshot verticalLog =
        new BlockSnapshot(Material.OAK_LOG, 5, 70, 12, null, "minecraft:oak_log[axis=y]");

    BlockSnapshot horizontalLog =
        new BlockSnapshot(Material.OAK_LOG, 5, 70, 12, null, "minecraft:oak_log[axis=x]");

    assertNotEquals(verticalLog, horizontalLog);
  }

  @Test
  void airCanBeRepresentedExplicitly() {
    BlockSnapshot snapshot = new BlockSnapshot(Material.AIR, 5, 70, 12, null, "minecraft:air");

    assertEquals(Material.AIR, snapshot.material());
    assertEquals("minecraft:air", snapshot.blockData());
  }
}
