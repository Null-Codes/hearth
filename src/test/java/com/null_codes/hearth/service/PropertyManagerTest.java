package com.null_codes.hearth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.null_codes.hearth.model.Property;
import java.util.UUID;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

public class PropertyManagerTest {

  @Test
  void duplicatePropertyThrowsException() {
    UUID uuid = UUID.randomUUID();
    Property property = new Property(uuid, uuid, "test", uuid, new BoundingBox(), 0);
    PropertyManager propertyManager = new PropertyManager();
    propertyManager.register(property);
    assertThrows(IllegalArgumentException.class, () -> propertyManager.register(property));
  }
}
