package com.null_codes.hearth.service;

import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.storage.PropertyStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;

/** Maintains property invariants while persistence completes asynchronously. */
public class PropertyManager {

  private final List<Property> properties = new ArrayList<>();
  private final Set<UUID> pendingPropertyUuids = new HashSet<>();
  private final PropertyStore store;
  private final CompletableFuture<Void> ready;

  public PropertyManager() {
    store = null;
    ready = CompletableFuture.completedFuture(null);
  }

  public PropertyManager(PropertyStore store) {
    this.store = Objects.requireNonNull(store, "store cannot be null");
    ready = store.loadProperties().thenAccept(this::loadProperties);
  }

  public CompletableFuture<Void> ready() {
    return ready;
  }

  public boolean isReady() {
    return ready.isDone() && !ready.isCompletedExceptionally();
  }

  public CompletableFuture<Void> register(Property property) {
    Objects.requireNonNull(property, "property cannot be null");
    return ready.thenCompose(
        ignored -> {
          synchronized (this) {
            if (findByUuid(property.uuid()).isPresent()
                || !pendingPropertyUuids.add(property.uuid())) {
              throw new IllegalArgumentException(
                  "A property with UUID " + property.uuid() + " is already registered.");
            }
          }

          CompletableFuture<Void> persistence =
              store == null ? CompletableFuture.completedFuture(null) : store.insert(property);
          return persistence.whenComplete(
              (result, failure) -> {
                synchronized (this) {
                  pendingPropertyUuids.remove(property.uuid());
                  if (failure == null) properties.add(property);
                }
              });
        });
  }

  public CompletableFuture<Void> update(Property property) {
    Objects.requireNonNull(property, "property cannot be null");
    return ready.thenCompose(
        ignored -> {
          synchronized (this) {
            Property existing =
                findByUuid(property.uuid())
                    .orElseThrow(
                        () ->
                            new IllegalArgumentException(
                                "No property with UUID " + property.uuid() + " is registered."));
            if (existing.timestamp() != property.timestamp()) {
              throw new IllegalArgumentException(
                  "A property's creation timestamp cannot be changed.");
            }
            requireNotPending(property.uuid());
          }

          CompletableFuture<Void> persistence =
              store == null ? CompletableFuture.completedFuture(null) : store.update(property);
          return persistence.whenComplete(
              (result, failure) -> {
                synchronized (this) {
                  pendingPropertyUuids.remove(property.uuid());
                  if (failure == null) {
                    for (int index = 0; index < properties.size(); index++) {
                      if (properties.get(index).uuid().equals(property.uuid())) {
                        properties.set(index, property);
                        break;
                      }
                    }
                  }
                }
              });
        });
  }

  public CompletableFuture<Boolean> remove(Property property) {
    Objects.requireNonNull(property, "property cannot be null");
    return ready.thenCompose(
        ignored -> {
          synchronized (this) {
            if (!properties.contains(property)) return CompletableFuture.completedFuture(false);
          }
          return removeExisting(property);
        });
  }

  public CompletableFuture<Boolean> remove(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    return ready.thenCompose(
        ignored -> {
          Property property;
          synchronized (this) {
            property = findByUuid(uuid).orElse(null);
            if (property == null) return CompletableFuture.completedFuture(false);
          }
          return removeExisting(property);
        });
  }

  private CompletableFuture<Boolean> removeExisting(Property property) {
    synchronized (this) {
      requireNotPending(property.uuid());
    }
    CompletableFuture<Void> persistence =
        store == null ? CompletableFuture.completedFuture(null) : store.delete(property.uuid());
    return persistence
        .thenApply(
            ignored -> {
              synchronized (this) {
                return properties.remove(property);
              }
            })
        .whenComplete(
            (removed, failure) -> {
              synchronized (this) {
                pendingPropertyUuids.remove(property.uuid());
              }
            });
  }

  public synchronized Optional<Property> get(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    return findByUuid(uuid);
  }

  public synchronized List<Property> getProperties() {
    return List.copyOf(properties);
  }

  public synchronized Optional<Property> findProperty(Location location) {
    for (Property property : properties) {
      if (property.contains(location)) return Optional.of(property);
    }
    return Optional.empty();
  }

  public synchronized Optional<Property> findProperty(
      UUID worldUuid, double x, double y, double z) {
    for (Property property : properties) {
      if (property.contains(worldUuid, x, y, z)) return Optional.of(property);
    }
    return Optional.empty();
  }

  private synchronized void loadProperties(List<Property> loaded) {
    for (Property property : loaded) {
      if (findByUuid(property.uuid()).isPresent()) {
        throw new IllegalArgumentException(
            "Store contains duplicate property UUID " + property.uuid() + ".");
      }
      properties.add(property);
    }
  }

  private Optional<Property> findByUuid(UUID uuid) {
    return properties.stream().filter(property -> property.uuid().equals(uuid)).findFirst();
  }

  private void requireNotPending(UUID uuid) {
    if (!pendingPropertyUuids.add(uuid)) {
      throw new IllegalStateException("An operation for property " + uuid + " is already pending.");
    }
  }
}
