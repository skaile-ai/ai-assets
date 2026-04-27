package com.skaile.excelmcp.config;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

/**
 * Parsed server configuration. Immutable record; constructed once at startup from environment
 * variables.
 *
 * <p>{@code allowUnsandboxed} is the explicit opt-in escape hatch that lets the server start with
 * no {@code EXCEL_MCP_ROOT}. Default posture is fail-closed: if the root is empty and this flag is
 * not {@code true}, startup aborts.
 */
public record ServerConfig(
    Optional<Path> excelMcpRoot, boolean allowUnsandboxed, long maxFileBytes, long maxCells) {

  public static final long DEFAULT_MAX_FILE_BYTES = 100L * 1024L * 1024L;
  public static final long DEFAULT_MAX_CELLS = 1_000_000L;

  public static ServerConfig fromEnvironment() {
    return fromEnvironment(System::getenv);
  }

  /** Test seam: env-var reads go through the supplied lookup rather than the live process env. */
  static ServerConfig fromEnvironment(Function<String, String> env) {
    Optional<Path> root =
        Optional.ofNullable(env.apply("EXCEL_MCP_ROOT")).filter(s -> !s.isBlank()).map(Path::of);
    boolean allowUnsandboxed = "true".equals(env.apply("EXCEL_MCP_ALLOW_UNSANDBOXED"));
    long maxBytes = parseLong(env, "EXCEL_MCP_MAX_FILE_BYTES", DEFAULT_MAX_FILE_BYTES);
    long maxCells = parseLong(env, "EXCEL_MCP_MAX_CELLS", DEFAULT_MAX_CELLS);
    return new ServerConfig(root, allowUnsandboxed, maxBytes, maxCells);
  }

  private static long parseLong(Function<String, String> env, String name, long fallback) {
    String raw = env.apply(name);
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
