package com.null_codes.hearth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PropertyChangeWorkloadTest {

  private static final UUID PROPERTY_UUID = UUID.fromString("10000000-0000-0000-0000-000000000000");
  private static final UUID WORLD_UUID = UUID.fromString("20000000-0000-0000-0000-000000000000");

  @Test
  void sameSeedProducesSameHistory() {
    PropertyChangeManager first = new PropertyChangeManager();
    PropertyChangeManager second = new PropertyChangeManager();

    PropertyChangeWorkload.generate(first, PROPERTY_UUID, WORLD_UUID, 500, 42).join();
    PropertyChangeWorkload.generate(second, PROPERTY_UUID, WORLD_UUID, 500, 42).join();

    assertEquals(first.getChanges(PROPERTY_UUID), second.getChanges(PROPERTY_UUID));
  }

  @Test
  void zeroCountRecordsNothing() {
    PropertyChangeManager manager = new PropertyChangeManager();

    assertEquals(
        0, PropertyChangeWorkload.generate(manager, PROPERTY_UUID, WORLD_UUID, 0, 42).join());
    assertEquals(0, manager.getChangeCount());
  }

  @Test
  void rejectsNegativeCount() {
    PropertyChangeManager manager = new PropertyChangeManager();

    assertThrows(
        IllegalArgumentException.class,
        () -> PropertyChangeWorkload.generate(manager, PROPERTY_UUID, WORLD_UUID, -1, 42));
  }
}
