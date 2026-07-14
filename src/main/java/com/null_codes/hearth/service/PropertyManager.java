package com.null_codes.hearth.service;

import com.null_codes.hearth.model.Property;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class PropertyManager {

  private final List<Property> properties = new ArrayList<>();

  public void register(Property property) {
    Objects.requireNonNull(property, "property");

    if (get(property.uuid()).isPresent()) {
      throw new IllegalArgumentException(
          "A property with UUID " + property.uuid() + " is already registered.");
    }

    properties.add(property);
  }

  public Optional<Property> get(UUID uuid) {
    return properties.stream().filter(property -> property.uuid().equals(uuid)).findFirst();
  }

  public List<Property> getProperties() {
    return Collections.unmodifiableList(this.properties);
  }

  public void remove(Property property) {
    this.properties.remove(property);
  }

  public void remove(UUID uuid) {
    get(uuid).ifPresent(this.properties::remove);
  }

  @Nullable public Property findProperty(Location location) {
    for (Property property : this.properties) {
      if (property.contains(location)) return property;
    }
    return null;
  }

}
