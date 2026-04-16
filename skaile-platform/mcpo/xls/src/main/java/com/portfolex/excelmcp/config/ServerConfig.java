package com.portfolex.excelmcp.config;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Parsed server configuration. Immutable record; constructed once at startup from environment
 * variables.
 */
public record ServerConfig(Optional<Path> excelMcpRoot, long maxFileBytes, long maxCells) {

  public static final long DEFAULT_MAX_FILE_BYTES = 100L * 1024L * 1024L;
  public static final long DEFAULT_MAX_CELLS = 1_000_000L;

  public static ServerConfig fromEnvironment() {
    Optional<Path> root =
        Optional.ofNullable(System.getenv("EXCEL_MCP_ROOT"))
            .filter(s -> !s.isBlank())
            .map(Path::of);
    long maxBytes = parseLong("EXCEL_MCP_MAX_FILE_BYTES", DEFAULT_MAX_FILE_BYTES);
    long maxCells = parseLong("EXCEL_MCP_MAX_CELLS", DEFAULT_MAX_CELLS);
    return new ServerConfig(root, maxBytes, maxCells);
  }

  private static long parseLong(String name, long fallback) {
    String raw = System.getenv(name);
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      return Long.parseLong(raw.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Environment variable " + name + " is not a valid long: " + raw, e);
    }
  }
}
