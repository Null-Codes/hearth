package com.null_codes.hearth.service;

import com.null_codes.hearth.model.PropertyChange;
import com.null_codes.hearth.storage.PropertyChangeStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Maintains append-only history while persistence completes asynchronously. */
public class PropertyChangeManager {

  private final List<PropertyChange> changes = new ArrayList<>();
  private final Set<UUID> pendingChangeUuids = new HashSet<>();
  private final PropertyChangeStore store;
  private final CompletableFuture<Void> ready;

  public PropertyChangeManager() {
    store = null;
    ready = CompletableFuture.completedFuture(null);
  }

  public PropertyChangeManager(PropertyChangeStore store) {
    this.store = Objects.requireNonNull(store, "store cannot be null");
    ready = store.loadChanges().thenAccept(this::loadChanges);
  }

  public CompletableFuture<Void> ready() {
    return ready;
  }

  public boolean isReady() {
    return ready.isDone() && !ready.isCompletedExceptionally();
  }

  public CompletableFuture<Void> record(PropertyChange change) {
    Objects.requireNonNull(change, "change cannot be null");
    return ready.thenCompose(
        ignored -> {
          synchronized (this) {
            if (findByUuid(change.uuid()).isPresent() || !pendingChangeUuids.add(change.uuid())) {
              throw new IllegalArgumentException(
                  "A change with UUID " + change.uuid() + " is already recorded.");
            }
          }

          CompletableFuture<Void> persistence =
              store == null ? CompletableFuture.completedFuture(null) : store.insert(change);
          return persistence.whenComplete(
              (result, failure) -> {
                synchronized (this) {
                  pendingChangeUuids.remove(change.uuid());
                  if (failure == null) changes.add(change);
                }
              });
        });
  }

  public synchronized Optional<PropertyChange> get(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    return findByUuid(uuid);
  }

  public synchronized int getChangeCount() {
    return changes.size();
  }

  public synchronized Set<UUID> getPropertyUuids() {
    LinkedHashSet<UUID> propertyUuids = new LinkedHashSet<>();
    for (PropertyChange change : changes) {
      propertyUuids.add(change.propertyUuid());
    }
    return Collections.unmodifiableSet(propertyUuids);
  }

  public synchronized List<PropertyChange> getChanges(UUID propertyUuid) {
    Objects.requireNonNull(propertyUuid, "propertyUuid cannot be null");
    return changes.stream().filter(change -> change.propertyUuid().equals(propertyUuid)).toList();
  }

  private synchronized void loadChanges(List<PropertyChange> loaded) {
    for (PropertyChange change : loaded) {
      if (findByUuid(change.uuid()).isPresent()) {
        throw new IllegalArgumentException(
            "Store contains duplicate change UUID " + change.uuid() + ".");
      }
      changes.add(change);
    }
  }

  private Optional<PropertyChange> findByUuid(UUID uuid) {
    return changes.stream().filter(change -> change.uuid().equals(uuid)).findFirst();
  }
}
