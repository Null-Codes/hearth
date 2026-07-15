package com.null_codes.hearth.service;

import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.storage.PropertyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;

public class PropertyManager {

  private final List<Property> properties = new ArrayList<>();
  private final PropertyStore store;

  public PropertyManager() {
    store = null;
  }

  public PropertyManager(PropertyStore store) {
    this.store = Objects.requireNonNull(store, "store cannot be null");
    for (Property property : store.loadProperties()) {
      if (get(property.uuid()).isPresent()) {
        throw new IllegalArgumentException(
            "Store contains duplicate property UUID " + property.uuid() + ".");
      }
      properties.add(property);
    }
  }

  public void register(Property property) {
    Objects.requireNonNull(property, "property cannot be null");
    if (get(property.uuid()).isPresent()) {
      throw new IllegalArgumentException(
          "A property with UUID " + property.uuid() + " is already registered.");
    }
    if (store != null) store.insert(property);
    properties.add(property);
  }

  public void update(Property property) {
    Objects.requireNonNull(property, "property cannot be null");
    for (int index = 0; index < properties.size(); index++) {
      Property existing = properties.get(index);
      if (!existing.uuid().equals(property.uuid())) continue;
      if (existing.timestamp() != property.timestamp()) {
        throw new IllegalArgumentException("A property's creation timestamp cannot be changed.");
      }
      if (store != null) store.update(property);
      properties.set(index, property);
      return;
    }
    throw new IllegalArgumentException(
        "No property with UUID " + property.uuid() + " is registered.");
  }

  public Optional<Property> get(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    return properties.stream().filter(property -> property.uuid().equals(uuid)).findFirst();
  }

  public List<Property> getProperties() {
    return List.copyOf(properties);
  }

  public boolean remove(Property property) {
    Objects.requireNonNull(property, "property cannot be null");
    if (!properties.contains(property)) return false;
    if (store != null) store.delete(property.uuid());
    return properties.remove(property);
  }

  public boolean remove(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    Optional<Property> property = get(uuid);
    if (property.isEmpty()) return false;
    if (store != null) store.delete(uuid);
    return properties.remove(property.get());
  }

  public Optional<Property> findProperty(Location location) {
    for (Property property : properties) {
      if (property.contains(location)) return Optional.of(property);
    }
    return Optional.empty();
  }

  public Optional<Property> findProperty(UUID worldUuid, double x, double y, double z) {
    for (Property property : properties) {
      if (property.contains(worldUuid, x, y, z)) return Optional.of(property);
    }
    return Optional.empty();
  }
}
