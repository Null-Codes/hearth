package com.null_codes.hearth.model;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public record Property(
    UUID uuid, UUID owner, String name, UUID world, BoundingBox region, long timestamp) {

  public Property {
    if (uuid == null) throw new NullPointerException("uuid cannot be null");
    if (owner == null) throw new NullPointerException("owner cannot be null");
    if (name == null) throw new NullPointerException("name cannot be null");
    if (world == null) throw new NullPointerException("world cannot be null");
    if (region == null) throw new NullPointerException("region cannot be null");
    if (name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
    if (timestamp < 0) throw new IllegalArgumentException("timestamp cannot be negative");

    region = region.clone();
  }

  @Override
  public BoundingBox region() {
    return region.clone();
  }

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
