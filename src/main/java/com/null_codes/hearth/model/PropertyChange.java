package com.null_codes.hearth.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public record PropertyChange(
    UUID uuid,
    UUID propertyUuid,
    Instant timestamp,
    @Nullable UUID playerUuid,
    ChangeCause cause,
    BlockSnapshot before,
    BlockSnapshot after) {

  public PropertyChange {
    Objects.requireNonNull(uuid, "uuid");
    Objects.requireNonNull(propertyUuid, "propertyUuid");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(cause, "cause");
    Objects.requireNonNull(before, "before");
    Objects.requireNonNull(after, "after");

    if (!before.worldUuid().equals(after.worldUuid())
        || before.x() != after.x()
        || before.y() != after.y()
        || before.z() != after.z()) {
      throw new IllegalArgumentException(
          "Before and after snapshots must represent the same block position.");
    }
  }

  public static PropertyChange create(
      UUID propertyUuid,
      @Nullable UUID playerUuid,
      ChangeCause cause,
      BlockSnapshot before,
      BlockSnapshot after) {

    return new PropertyChange(
        UUID.randomUUID(), propertyUuid, Instant.now(), playerUuid, cause, before, after);
  }

  public enum ChangeCause {
    PLAYER_BREAK,
    PLAYER_PLACE,

    FIRE,
    EXPLOSION,
    LIQUID,

    PISTON,
    FALLING_BLOCK,

    GROWTH,
    DECAY,

    ENTITY_CHANGE,

    PLUGIN,
    RESTORATION
  }
}
