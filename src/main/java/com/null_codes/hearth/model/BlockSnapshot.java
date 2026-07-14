package com.null_codes.hearth.model;

import org.bukkit.Material;
import org.bukkit.block.Block;

public record BlockSnapshot(Material material, int x, int y, int z, String blockData) {

  public BlockSnapshot from(Block block) {
    return new BlockSnapshot(block.getType(), block.getX(), block.getY(), block.getZ(), block.getBlockData().getAsString());
  }

}
