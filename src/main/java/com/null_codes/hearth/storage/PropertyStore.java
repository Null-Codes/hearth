package com.null_codes.hearth.storage;

import com.null_codes.hearth.model.Property;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Defines ordered asynchronous property persistence operations. */
public interface PropertyStore {
  CompletableFuture<List<Property>> loadProperties();

  CompletableFuture<Void> insert(Property property);

  CompletableFuture<Void> update(Property property);

  CompletableFuture<Void> delete(UUID propertyUuid);
}
