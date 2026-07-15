package com.null_codes.hearth.service;

import com.null_codes.hearth.model.PropertyChange;
import com.null_codes.hearth.storage.PropertyChangeStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PropertyChangeManager {

  private final List<PropertyChange> changes = new ArrayList<>();
  private final PropertyChangeStore store;

  public PropertyChangeManager() {
    store = null;
  }

  public PropertyChangeManager(PropertyChangeStore store) {
    this.store = Objects.requireNonNull(store, "store cannot be null");
    for (PropertyChange change : store.loadChanges()) {
      if (get(change.uuid()).isPresent()) {
        throw new IllegalArgumentException(
            "Store contains duplicate change UUID " + change.uuid() + ".");
      }
      changes.add(change);
    }
  }

  public void record(PropertyChange change) {
    Objects.requireNonNull(change, "change cannot be null");
    // TODO Profile this linear duplicate check before choosing an index.
    if (get(change.uuid()).isPresent()) {
      throw new IllegalArgumentException(
          "A change with UUID " + change.uuid() + " is already recorded.");
    }
    if (store != null) store.insert(change);
    changes.add(change);
  }

  public Optional<PropertyChange> get(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    return changes.stream().filter(change -> change.uuid().equals(uuid)).findFirst();
  }

  public int getChangeCount() {
    return changes.size();
  }

  public List<PropertyChange> getChanges(UUID propertyUuid) {
    Objects.requireNonNull(propertyUuid, "propertyUuid cannot be null");
    return changes.stream().filter(change -> change.propertyUuid().equals(propertyUuid)).toList();
  }
}
