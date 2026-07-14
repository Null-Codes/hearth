package com.null_codes.hearth.service;

import com.null_codes.hearth.model.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class PropertyManager {

  private final List<Property> properties = new ArrayList<>();

  public void registerProperty(Property property) {
    this.properties.add(property);
  }

  public void removeProperty(Property property) {
    this.properties.remove(property);
  }

  @Nullable public Property getProperty(UUID uuid) {
    for (Property property : this.properties) {
      if (property.uuid().equals(uuid)) {
        return property;
      }
    }
    return null;
  }

  @Nullable public Property findProperty(Location location) {
    for (Property property : this.properties) {
      if (property.contains(location)) return property;
    }
    return null;
  }
}
