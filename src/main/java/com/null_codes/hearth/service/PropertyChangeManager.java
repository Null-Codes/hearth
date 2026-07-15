package com.null_codes.hearth.service;

import com.null_codes.hearth.model.PropertyChange;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PropertyChangeManager {

  private final List<PropertyChange> changes = new ArrayList<>();

  public void record(PropertyChange change) {
    Objects.requireNonNull(change, "change cannot be null");

    if (get(change.uuid()).isPresent()) {
      throw new IllegalArgumentException(
          "A change with UUID " + change.uuid() + " is already recorded.");
    }

    changes.add(change);
  }

  public Optional<PropertyChange> get(UUID uuid) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    return changes.stream().filter(change -> change.uuid().equals(uuid)).findFirst();
  }

  public List<PropertyChange> getChanges(UUID propertyUuid) {
    Objects.requireNonNull(propertyUuid, "propertyUuid cannot be null");
    return changes.stream().filter(change -> change.propertyUuid().equals(propertyUuid)).toList();
  }
}
