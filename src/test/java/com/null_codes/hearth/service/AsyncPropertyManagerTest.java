package com.null_codes.hearth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.storage.PropertyStore;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

class AsyncPropertyManagerTest {

  @Test
  void propertyBecomesVisibleOnlyAfterPersistenceCompletes() {
    CompletableFuture<Void> insert = new CompletableFuture<>();
    PropertyManager manager = new PropertyManager(new DelayedInsertStore(insert));
    Property property = property();

    CompletableFuture<Void> registration = manager.register(property);

    assertFalse(manager.get(property.uuid()).isPresent());
    assertTrue(
        assertThrows(CompletionException.class, () -> manager.register(property).join()).getCause()
            instanceof IllegalArgumentException);

    insert.complete(null);
    registration.join();

    assertTrue(manager.get(property.uuid()).isPresent());
  }

  private static Property property() {
    return new Property(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Async Property",
        UUID.randomUUID(),
        new BoundingBox(0, 0, 0, 10, 10, 10),
        1);
  }

  private record DelayedInsertStore(CompletableFuture<Void> insertion) implements PropertyStore {
    @Override
    public CompletableFuture<List<Property>> loadProperties() {
      return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<Void> insert(Property property) {
      return insertion;
    }

    @Override
    public CompletableFuture<Void> update(Property property) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> delete(UUID propertyUuid) {
      return CompletableFuture.completedFuture(null);
    }
  }
}
