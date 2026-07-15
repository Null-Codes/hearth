package com.null_codes.hearth.model;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

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

  public static BlockSnapshot from(BlockState blockState) {
    Objects.requireNonNull(blockState, "block state is null");
    return new BlockSnapshot(
        blockState.getType(),
        blockState.getX(),
        blockState.getY(),
        blockState.getZ(),
        blockState.getWorld().getUID(),
        blockState.getBlockData().getAsString());
  }

  public static BlockSnapshot airAt(Block block) {
    Objects.requireNonNull(block, "block is null");
    return airAt(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
  }

  public static BlockSnapshot airAt(UUID worldUuid, int x, int y, int z) {
    Objects.requireNonNull(worldUuid, "world UUID is null");
    return new BlockSnapshot(Material.AIR, x, y, z, worldUuid, "minecraft:air");
  }
}
