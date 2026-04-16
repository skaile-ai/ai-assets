package com.portfolex.excelmcp.handles;

import java.util.UUID;

/** Opaque workbook handle (§5.1). Format: {@code wb-<8 hex>}. */
public record HandleId(String value) {

  public HandleId {
    if (value == null || !value.startsWith("wb-")) {
      throw new IllegalArgumentException("handle id must start with 'wb-': " + value);
    }
  }

  public static HandleId newRandom() {
    String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    return new HandleId("wb-" + hex);
  }

  @Override
  public String toString() {
    return value;
  }
}
