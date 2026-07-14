package com.null_codes.hearth.model;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public record Property(
    UUID uuid, UUID owner, String name, UUID world, BoundingBox region, long timestamp) {

  public boolean contains(Location location) {
    return region.contains(location.toVector());
  }
}
