package com.yuki.sevendays_states.web;

import java.util.Set;

/** Shared visibility rules for high-signal game events. */
final class TimelineEventPolicy {

  private static final Set<String> ALWAYS_VISIBLE_KINDS = Set.of(
      "LEAVE",
      "WANDERING_HORDE",
      "SCOUT_HORDE");

  private TimelineEventPolicy() {
  }

  static boolean isAlwaysVisible(String kind) {
    return kind != null && ALWAYS_VISIBLE_KINDS.contains(kind);
  }
}
