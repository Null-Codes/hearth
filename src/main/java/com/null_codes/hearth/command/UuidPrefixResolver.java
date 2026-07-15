package com.null_codes.hearth.command;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Resolves canonical or compact UUID prefixes while rejecting ambiguous matches. */
final class UuidPrefixResolver {

  private UuidPrefixResolver() {}

  static UUID resolve(Collection<UUID> candidates, String value) {
    Objects.requireNonNull(candidates, "candidates cannot be null");
    Objects.requireNonNull(value, "value cannot be null");

    String normalized = normalize(value);
    if (normalized.isEmpty()) throw new IllegalArgumentException("A property UUID is required.");

    List<UUID> matches =
        candidates.stream()
            .distinct()
            .filter(uuid -> normalize(uuid.toString()).startsWith(normalized))
            .toList();
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("Unknown property UUID " + value + ".");
    }
    if (matches.size() > 1) {
      throw new IllegalArgumentException("Property UUID " + value + " is ambiguous.");
    }
    return matches.getFirst();
  }

  static List<String> suggestions(Collection<UUID> candidates, String value) {
    String normalized = normalize(value);
    return candidates.stream()
        .distinct()
        .map(UUID::toString)
        .filter(uuid -> normalize(uuid).startsWith(normalized))
        .sorted(Comparator.naturalOrder())
        .toList();
  }

  private static String normalize(String value) {
    return value.replace("-", "").toLowerCase(Locale.ROOT);
  }
}
