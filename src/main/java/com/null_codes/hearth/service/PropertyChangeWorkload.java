package com.null_codes.hearth.service;

import com.null_codes.hearth.model.BlockSnapshot;
import com.null_codes.hearth.model.PropertyChange;
import java.time.Instant;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Material;

/**
 * Produces repeatable property history without requiring a running Minecraft world.
 *
 * <p>The generated changes use realistic block transitions and the normal history service so
 * profiling includes validation, duplicate checks, and storage behavior.
 */
public final class PropertyChangeWorkload {

  private static final Instant BASE_TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");
  private static final Material[] MATERIALS = {
    Material.STONE,
    Material.DIRT,
    Material.OAK_PLANKS,
    Material.COBBLESTONE,
    Material.GLASS,
    Material.BRICKS
  };

  private PropertyChangeWorkload() {}

  /**
   * Records a deterministic sequence of player block changes.
   *
   * @param manager destination that receives every generated change
   * @param propertyUuid property whose history is populated
   * @param worldUuid world used by each block snapshot
   * @param count number of changes to generate
   * @param seed seed controlling UUIDs, positions, materials, and players
   * @return the number of changes recorded
   */
  public static int generate(
      PropertyChangeManager manager, UUID propertyUuid, UUID worldUuid, int count, long seed) {
    Objects.requireNonNull(manager, "manager cannot be null");
    Objects.requireNonNull(propertyUuid, "propertyUuid cannot be null");
    Objects.requireNonNull(worldUuid, "worldUuid cannot be null");
    if (count < 0) throw new IllegalArgumentException("count cannot be negative");

    Random random = new Random(seed);
    for (int index = 0; index < count; index++) {
      int x = random.nextInt(-2048, 2049);
      int y = random.nextInt(-64, 321);
      int z = random.nextInt(-2048, 2049);
      Material material = MATERIALS[random.nextInt(MATERIALS.length)];
      boolean placement = random.nextBoolean();
      UUID playerUuid = new UUID(random.nextLong(), random.nextLong());
      UUID changeUuid = new UUID(random.nextLong(), random.nextLong());

      BlockSnapshot solid =
          new BlockSnapshot(material, x, y, z, worldUuid, material.getKey().asString());
      BlockSnapshot air = BlockSnapshot.airAt(worldUuid, x, y, z);
      PropertyChange change =
          new PropertyChange(
              changeUuid,
              propertyUuid,
              BASE_TIMESTAMP.plusMillis(index),
              playerUuid,
              placement
                  ? PropertyChange.ChangeCause.PLAYER_PLACE
                  : PropertyChange.ChangeCause.PLAYER_BREAK,
              placement ? air : solid,
              placement ? solid : air);
      manager.record(change);
    }

    return count;
  }
}
