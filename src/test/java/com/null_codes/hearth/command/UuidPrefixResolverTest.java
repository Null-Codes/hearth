package com.null_codes.hearth.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidPrefixResolverTest {

  private static final UUID FIRST = UUID.fromString("abc00000-0000-0000-0000-000000000001");
  private static final UUID SECOND = UUID.fromString("abc10000-0000-0000-0000-000000000002");

  @Test
  void resolvesUniqueCanonicalPrefix() {
    assertEquals(FIRST, UuidPrefixResolver.resolve(List.of(FIRST, SECOND), "abc0"));
  }

  @Test
  void resolvesPrefixWithoutHyphens() {
    assertEquals(
        FIRST, UuidPrefixResolver.resolve(List.of(FIRST), "abc00000000000000000000000000001"));
  }

  @Test
  void rejectsAmbiguousPrefix() {
    assertThrows(
        IllegalArgumentException.class,
        () -> UuidPrefixResolver.resolve(List.of(FIRST, SECOND), "abc"));
  }

  @Test
  void rejectsUnknownPrefix() {
    assertThrows(
        IllegalArgumentException.class,
        () -> UuidPrefixResolver.resolve(List.of(FIRST, SECOND), "def"));
  }

  @Test
  void suggestsMatchingFullUuidsInStableOrder() {
    assertEquals(
        List.of(FIRST.toString(), SECOND.toString()),
        UuidPrefixResolver.suggestions(List.of(SECOND, FIRST), "ABC"));
  }
}
