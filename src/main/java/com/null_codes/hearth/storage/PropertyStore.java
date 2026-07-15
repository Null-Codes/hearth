package com.null_codes.hearth.storage;

import com.null_codes.hearth.model.Property;
import java.util.List;
import java.util.UUID;

/** Defines durable property operations without coupling business logic to SQLite. */
public interface PropertyStore {
  List<Property> loadProperties();

  void insert(Property property);

  void update(Property property);

  void delete(UUID propertyUuid);
}
