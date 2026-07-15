package com.null_codes.hearth.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class PropertyTest {

  private static final UUID PROPERTY_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OWNER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID WORLD_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OTHER_WORLD_UUID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Test
  void containsReturnsTrueForPointInsideRegion() {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));

    assertTrue(property.contains(WORLD_UUID, 5, 5, 5));
  }

  @ParameterizedTest
  @CsvSource({"-1, 5, 5", "10, 5, 5", "5, -1, 5", "5, 10, 5", "5, 5, -1", "5, 5, 10"})
  void containsReturnsFalseForCoordinatesOutsideEachAxis(double x, double y, double z) {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));

    assertFalse(property.contains(WORLD_UUID, x, y, z));
  }

  @Test
  void containsReturnsTrueAtExactMinimumBoundary() {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));

    assertTrue(property.contains(WORLD_UUID, 0, 0, 0));
  }

  @Test
  void containsReturnsFalseAtExactMaximumBoundary() {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));

    assertFalse(property.contains(WORLD_UUID, 10, 10, 10));
  }

  @Test
  void containsWorksWithNegativeCoordinates() {
    Property property = property(new BoundingBox(-10, -10, -10, 0, 0, 7));

    assertTrue(property.contains(WORLD_UUID, -2, -3, 4));
  }

  @Test
  void containsReturnsFalseForDifferentWorld() {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));

    assertFalse(property.contains(OTHER_WORLD_UUID, 5, 5, 5));
  }

  @Test
  void containsReturnsTrueImmediatelyBelowMaximumBoundary() {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));

    assertTrue(property.contains(WORLD_UUID, 9.999, 9.999, 9.999));
  }

  @Test
  void containsWorksWithFractionalCoordinates() {
    Property property = property(new BoundingBox(1.25, 2.5, 3.75, 9.5, 10.75, 11.125));

    assertTrue(property.contains(WORLD_UUID, 1.5, 2.75, 10.999));
  }

  @Test
  void containsReturnsFalseForNullLocation() {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));

    assertFalse(property.contains((Location) null));
  }

  @Test
  void containsReturnsFalseForLocationWithNullWorld() {
    Property property = property(new BoundingBox(0, 0, 0, 10, 10, 10));
    Location location = new Location(null, 5, 5, 5);

    assertFalse(property.contains(location));
  }

  @Test
  void rejectsBlankName() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Property(
                PROPERTY_UUID,
                OWNER_UUID,
                " ",
                WORLD_UUID,
                new BoundingBox(0, 0, 0, 10, 10, 10),
                0));
  }

  @Test
  void isolatesRegionFromExternalMutation() {
    BoundingBox region = new BoundingBox(0, 0, 0, 10, 10, 10);
    Property property = property(region);

    region.shift(100, 0, 0);
    BoundingBox returnedRegion = property.region();
    returnedRegion.shift(100, 0, 0);

    assertTrue(property.contains(WORLD_UUID, 5, 5, 5));
    assertNotEquals(returnedRegion, property.region());
  }

  private static Property property(BoundingBox region) {
    return new Property(PROPERTY_UUID, OWNER_UUID, "Test Property", WORLD_UUID, region, 123456789L);
  }
}
