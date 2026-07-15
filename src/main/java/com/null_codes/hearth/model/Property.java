package com.null_codes.hearth.model;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public record Property(
    UUID uuid, UUID owner, String name, UUID world, BoundingBox region, long timestamp) {

  public boolean contains(Location location) {
    if (location == null || location.getWorld() == null) return false;
    return contains(
        location.getWorld().getUID(), location.getX(), location.getY(), location.getZ());
  }

  public boolean contains(UUID worldUuid, double x, double y, double z) {
    if (worldUuid == null || !worldUuid.equals(world)) return false;
    return x >= region.getMinX()
        && x < region.getMaxX()
        && y >= region.getMinY()
        && y < region.getMaxY()
        && z >= region.getMinZ()
        && z < region.getMaxZ();
  }
}
