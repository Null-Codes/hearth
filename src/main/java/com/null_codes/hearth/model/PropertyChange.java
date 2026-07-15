package com.null_codes.hearth.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PropertyChange(
    UUID uuid,
    UUID propertyUuid,
    Instant timestamp,
    UUID playerUuid,
    ChangeCause cause,
    BlockSnapshot before,
    BlockSnapshot after) {

  public PropertyChange(
      UUID propertyUuid,
      UUID playerUuid,
      ChangeCause cause,
      BlockSnapshot before,
      BlockSnapshot after) {

    this(UUID.randomUUID(), propertyUuid, Instant.now(), playerUuid, cause, before, after);
  }

  public PropertyChange {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    Objects.requireNonNull(propertyUuid, "propertyUuid cannot be null");
    Objects.requireNonNull(timestamp, "timestamp cannot be null");
    Objects.requireNonNull(cause, "cause cannot be null");
    Objects.requireNonNull(before, "before snapshot cannot be null");
    Objects.requireNonNull(after, "after snapshot cannot be null");

    if (before.x() != after.x() || before.y() != after.y() || before.z() != after.z()) {
      throw new IllegalArgumentException(
          "Before and after snapshots must refer to the same block coordinates.");
    }

    if (!Objects.equals(before.worldUuid(), after.worldUuid())) {
      throw new IllegalArgumentException(
          "Before and after snapshots must refer to the same world.");
    }

    if (before.material() == after.material()
        && Objects.equals(before.blockData(), after.blockData())) {
      throw new IllegalArgumentException("PropertyChange must represent an actual block change.");
    }

    if (cause.requiresPlayer() && playerUuid == null) {
      throw new IllegalArgumentException("A player UUID is required for " + cause + ".");
    }
  }

  public enum ChangeCause {
    PLAYER_PLACE(true),
    PLAYER_BREAK(true),
    FIRE(false),
    EXPLOSION(false),
    LIQUID(false),
    PISTON(false),
    FALLING_BLOCK(false),
    GROWTH(false),
    DECAY(false),
    ENTITY_CHANGE(false),
    PLUGIN(false),
    RESTORATION(false);

    private final boolean requiresPlayer;

    ChangeCause(boolean requiresPlayer) {
      this.requiresPlayer = requiresPlayer;
    }

    public boolean requiresPlayer() {
      return requiresPlayer;
    }
  }
}
