package com.null_codes.hearth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.null_codes.hearth.model.BlockSnapshot;
import com.null_codes.hearth.model.PropertyChange;
import com.null_codes.hearth.model.PropertyChange.ChangeCause;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyChangeManagerTest {

  private static final UUID WORLD_UUID = UUID.fromString("10000000-0000-0000-0000-000000000000");
  private static final UUID PROPERTY_UUID = UUID.fromString("20000000-0000-0000-0000-000000000000");
  private PropertyChangeManager manager;

  @BeforeEach
  void setUp() {
    manager = new PropertyChangeManager();
  }

  @Test
  void lookupRejectsNullUuids() {
    assertThrows(NullPointerException.class, () -> manager.get(null));
    assertThrows(NullPointerException.class, () -> manager.getChanges(null));
  }

  @Test
  void getReturnsEmptyWhenChangeDoesNotExist() {
    assertTrue(manager.get(UUID.randomUUID()).isEmpty());
  }

  @Test
  void recordMakesChangeRetrievableByUuid() {
    PropertyChange change =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z"));

    manager.record(change);

    assertTrue(manager.get(change.uuid()).isPresent());
    assertSame(change, manager.get(change.uuid()).orElseThrow());
  }

  @Test
  void recordRejectsNullChange() {
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> manager.record(null));

    assertEquals("change cannot be null", exception.getMessage());
  }

  @Test
  void recordRejectsDuplicateChangeUuid() {
    UUID changeUuid = UUID.randomUUID();

    PropertyChange first =
        createChange(changeUuid, PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z"));

    PropertyChange duplicate =
        createChange(changeUuid, PROPERTY_UUID, Instant.parse("2026-07-14T20:01:00Z"));

    manager.record(first);

    CompletionException completion =
        assertThrows(CompletionException.class, () -> manager.record(duplicate).join());
    IllegalArgumentException exception = (IllegalArgumentException) completion.getCause();

    assertEquals(
        "A change with UUID " + changeUuid + " is already recorded.", exception.getMessage());
  }

  @Test
  void failedDuplicateRecordKeepsOriginalChange() {
    UUID changeUuid = UUID.randomUUID();

    PropertyChange original =
        createChange(changeUuid, PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z"));

    PropertyChange duplicate =
        createChange(changeUuid, PROPERTY_UUID, Instant.parse("2026-07-14T20:01:00Z"));

    manager.record(original);

    assertTrue(
        assertThrows(CompletionException.class, () -> manager.record(duplicate).join()).getCause()
            instanceof IllegalArgumentException);

    assertSame(original, manager.get(changeUuid).orElseThrow());
  }

  @Test
  void getChangesReturnsEmptyListWhenPropertyHasNoChanges() {
    List<PropertyChange> changes = manager.getChanges(UUID.randomUUID());

    assertTrue(changes.isEmpty());
  }

  @Test
  void getChangesReturnsChangesForRequestedProperty() {
    PropertyChange first =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z"));

    PropertyChange second =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:01:00Z"));

    manager.record(first);
    manager.record(second);

    List<PropertyChange> changes = manager.getChanges(PROPERTY_UUID);

    assertEquals(List.of(first, second), changes);
  }

  @Test
  void getChangesDoesNotReturnChangesForOtherProperties() {
    UUID otherPropertyUuid = UUID.randomUUID();

    PropertyChange requestedPropertyChange =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z"));

    PropertyChange otherPropertyChange =
        createChange(UUID.randomUUID(), otherPropertyUuid, Instant.parse("2026-07-14T20:01:00Z"));

    manager.record(requestedPropertyChange);
    manager.record(otherPropertyChange);

    List<PropertyChange> changes = manager.getChanges(PROPERTY_UUID);

    assertEquals(List.of(requestedPropertyChange), changes);
  }

  @Test
  void getChangesPreservesRecordingOrder() {
    PropertyChange first =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:02:00Z"));

    PropertyChange second =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:01:00Z"));

    PropertyChange third =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z"));

    manager.record(first);
    manager.record(second);
    manager.record(third);

    List<PropertyChange> changes = manager.getChanges(PROPERTY_UUID);

    assertEquals(List.of(first, second, third), changes);
  }

  @Test
  void exposesDistinctPropertyUuidsForHistoryLookup() {
    UUID otherPropertyUuid = UUID.randomUUID();
    manager.record(
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z")));
    manager.record(
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:01:00Z")));
    manager.record(
        createChange(UUID.randomUUID(), otherPropertyUuid, Instant.parse("2026-07-14T20:02:00Z")));

    assertEquals(Set.of(PROPERTY_UUID, otherPropertyUuid), manager.getPropertyUuids());
  }

  @Test
  void recordingDifferentChangeUuidsSucceeds() {
    PropertyChange first =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:00:00Z"));

    PropertyChange second =
        createChange(UUID.randomUUID(), PROPERTY_UUID, Instant.parse("2026-07-14T20:01:00Z"));

    manager.record(first);
    manager.record(second);

    assertSame(first, manager.get(first.uuid()).orElseThrow());
    assertSame(second, manager.get(second.uuid()).orElseThrow());
  }

  private PropertyChange createChange(UUID changeUuid, UUID propertyUuid, Instant timestamp) {

    BlockSnapshot before =
        new BlockSnapshot(Material.STONE, 10, 64, -20, WORLD_UUID, "minecraft:stone");

    BlockSnapshot after = new BlockSnapshot(Material.AIR, 10, 64, -20, WORLD_UUID, "minecraft:air");

    return new PropertyChange(
        changeUuid,
        propertyUuid,
        timestamp,
        UUID.randomUUID(),
        ChangeCause.PLAYER_BREAK,
        before,
        after);
  }
}
