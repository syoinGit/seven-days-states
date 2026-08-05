package com.yuki.sevendays_states.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Shared hashing helper for stable import idempotency keys. */
public final class Hashing {
  private Hashing() {
  }

  public static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 cannot be calculated", e);
    }
  }
}
