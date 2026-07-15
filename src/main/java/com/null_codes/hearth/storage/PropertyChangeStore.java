package com.null_codes.hearth.storage;

import com.null_codes.hearth.model.PropertyChange;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Defines ordered asynchronous append-only property history persistence. */
public interface PropertyChangeStore {
  CompletableFuture<List<PropertyChange>> loadChanges();

  CompletableFuture<Void> insert(PropertyChange change);
}
