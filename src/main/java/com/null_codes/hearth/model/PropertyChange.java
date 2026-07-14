package com.null_codes.hearth.model;

import java.time.Instant;
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
