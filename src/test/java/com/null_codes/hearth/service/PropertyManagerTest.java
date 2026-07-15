package com.null_codes.hearth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.null_codes.hearth.model.Property;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PropertyManagerTest {

  private static final UUID WORLD_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private PropertyManager propertyManager;

  @BeforeEach
  void setUp() {
    propertyManager = new PropertyManager();
  }

  private Property testProperty() {
    return new Property(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Test Property",
        WORLD_UUID,
        new BoundingBox(0, 0, 0, 10, 10, 10),
        System.currentTimeMillis());
  }

  @Test
  void startsEmpty() {
    assertTrue(propertyManager.getProperties().isEmpty());
  }

  @Test
  void registerPropertyMakesPropertyRetrievable() {
    Property property = testProperty();
    UUID propertyUuid = property.uuid();

    propertyManager.register(property);
    assertTrue(
        propertyManager.get(propertyUuid).isPresent()
            && propertyManager.get(propertyUuid).get() == property);
  }

  @Test
  void registerPropertyRejectsDuplicateUuid() {
    Property property = testProperty();

    propertyManager.register(property);
    assertThrows(IllegalArgumentException.class, () -> propertyManager.register(property));
  }

  @Test
  void removePropertyMakesPropertyUnavailable() {
    Property property = testProperty();
    UUID propertyUuid = property.uuid();

    propertyManager.register(property);
    assertTrue(propertyManager.remove(propertyUuid));
    assertFalse(propertyManager.get(propertyUuid).isPresent());
  }

  @Test
  void removeUnknownPropertyDoesNothing() {
    Property propertyOne = testProperty();
    Property propertyTwo = testProperty();

    propertyManager.register(propertyOne);
    List<Property> props1 = propertyManager.getProperties();

    assertFalse(propertyManager.remove(propertyTwo));
    List<Property> props2 = propertyManager.getProperties();

    assertEquals(props1, props2);
  }

  @Test
  void updateReplacesPropertyWithSameUuid() {
    Property original = testProperty();
    Property updated =
        new Property(
            original.uuid(),
            original.owner(),
            "Renamed Property",
            original.world(),
            original.region(),
            original.timestamp());
    propertyManager.register(original);

    propertyManager.update(updated);

    assertSame(updated, propertyManager.get(original.uuid()).orElseThrow());
  }

  @Test
  void updateRejectsUnknownProperty() {
    Property property = testProperty();

    assertThrows(IllegalArgumentException.class, () -> propertyManager.update(property));
  }

  @Test
  void updateRejectsChangedCreationTimestamp() {
    Property original = testProperty();
    Property changedTimestamp =
        new Property(
            original.uuid(),
            original.owner(),
            original.name(),
            original.world(),
            original.region(),
            original.timestamp() + 1);
    propertyManager.register(original);

    assertThrows(IllegalArgumentException.class, () -> propertyManager.update(changedTimestamp));
  }

  @Test
  void findPropertyReturnsContainingProperty() {
    Property property =
        new Property(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test Property",
            WORLD_UUID,
            new BoundingBox(0, 0, 0, 10, 10, 10),
            System.currentTimeMillis());

    propertyManager.register(property);
    assertTrue(propertyManager.findProperty(WORLD_UUID, 5, 5, 5).isPresent());
  }

  @Test
  void findPropertyReturnsEmptyWhenNoPropertyContainsLocation() {
    Property property = testProperty();
    Location location = new Location(null, -5, -5, -5);

    propertyManager.register(property);
    assertTrue(propertyManager.findProperty(location).isEmpty());
  }
}
