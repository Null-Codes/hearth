package com.null_codes.hearth.model;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;

public record BlockSnapshot(
    Material material, int x, int y, int z, UUID worldUuid, String blockData) {

  public static BlockSnapshot from(Block block) {
    Objects.requireNonNull(block, "block is null");
    return new BlockSnapshot(
        block.getType(),
        block.getX(),
        block.getY(),
        block.getZ(),
        block.getWorld().getUID(),
        block.getBlockData().getAsString());
  }

  public static BlockSnapshot airAt(Block block) {
    Objects.requireNonNull(block, "block is null");
    return new BlockSnapshot(
        Material.AIR,
        block.getX(),
        block.getY(),
        block.getZ(),
        block.getWorld().getUID(),
        block.getBlockData().getAsString());
  }
}
