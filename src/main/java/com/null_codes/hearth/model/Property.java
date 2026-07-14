package com.null_codes.hearth.model;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public record Property(
    UUID uuid, UUID owner, String name, UUID world, BoundingBox region, long timestamp) {

  public boolean contains(Location location) {
    World otherWorld = location.getWorld();
    if (otherWorld == null || !otherWorld.getUID().equals(world)) return false;
    return region.contains(location.toVector());
  }
}
