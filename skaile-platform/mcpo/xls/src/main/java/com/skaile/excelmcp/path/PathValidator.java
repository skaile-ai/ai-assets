package com.skaile.excelmcp.path;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Validates agent-supplied paths against the rules in §6. Stateful only in that it holds a resolved
 * {@link ExcelMcpRoot}; everything else is computed per call.
 *
 * <p>Check ordering (§6.1) is load-bearing for security — the sandbox containment check must run
 * before the format whitelist and the file-existence check, otherwise an attacker can distinguish
 * PATH_OUTSIDE_ROOT from FILE_NOT_FOUND / FORMAT_UNSUPPORTED and the sandbox signal becomes
 * unreachable for any file outside the sandbox.
 *
 * <p>Canonicalisation uses {@link Path#toRealPath()} on the target when it exists and on the parent
 * otherwise, so symlink escapes ({@code /data/link -> /etc/passwd}) are caught even for destination
 * paths that don't yet exist.
 */
public final class PathValidator {

  private final ExcelMcpRoot root;

  public PathValidator(ExcelMcpRoot root) {
    this.root = root;
  }

  /** Validate for an existing file (open, save-to-existing). */
  public Path validateExisting(String input) throws McpException {
    Path p = parse(input);
    assertInsideRoot(p);
    FormatWhitelist.format(p);
    if (!Files.exists(p)) {
      throw new McpException(
          ErrorCode.FILE_NOT_FOUND, "no such file: " + p, Map.of("path", p.toString()));
    }
    return p;
  }

  /** Validate for a destination path that may not yet exist (create, save-to-new). */
  public Path validateDestination(String input) throws McpException {
    Path p = parse(input);
    assertInsideRoot(p);
    FormatWhitelist.format(p);
    return p;
  }

  private Path parse(String input) throws McpException {
    if (input == null || input.isEmpty()) {
      throw new McpException(ErrorCode.PATH_INVALID, "empty path", Map.of());
    }
    if (input.indexOf('\u0000') >= 0) {
      throw new McpException(
          ErrorCode.PATH_INVALID, "path contains NUL byte", Map.of("path", input));
    }
    try {
      return Path.of(input).toAbsolutePath().normalize();
    } catch (RuntimeException ex) {
      throw new McpException(
          ErrorCode.PATH_INVALID,
          "unparseable path: " + ex.getMessage(),
          Map.of("path", input),
          ex);
    }
  }

  private void assertInsideRoot(Path candidate) throws McpException {
    if (!root.isEnabled()) {
      return;
    }
    Path rootPath = root.canonical().orElseThrow();
    Path canonical = canonicaliseForContainment(candidate);
    if (!canonical.startsWith(rootPath)) {
      throw new McpException(
          ErrorCode.PATH_OUTSIDE_ROOT,
          "path escapes EXCEL_MCP_ROOT",
          Map.of("path", canonical.toString(), "root", rootPath.toString()));
    }
  }

  /**
   * Best-effort canonicalisation for containment checks.
   *
   * <ul>
   *   <li>Target exists: {@code target.toRealPath()} — resolves symlinks and dotdot segments.
   *   <li>Target missing but parent exists: {@code parent.toRealPath().resolve(filename)} — so
   *       intended destinations inside the sandbox are accepted even before the file is written.
   *   <li>Neither exists: fall back to the already-normalised path. Symlinks can't be resolved but
   *       the normalised form is still usable for a prefix comparison — when it falls outside root,
   *       containment correctly rejects it.
   * </ul>
   */
  private Path canonicaliseForContainment(Path candidate) throws McpException {
    try {
      if (Files.exists(candidate)) {
        return candidate.toRealPath();
      }
      Path parent = candidate.getParent();
      if (parent != null && Files.exists(parent)) {
        return parent.toRealPath().resolve(candidate.getFileName());
      }
      return candidate;
    } catch (IOException ex) {
      throw new McpException(
          ErrorCode.PATH_INVALID,
          "cannot canonicalise: " + ex.getMessage(),
          Map.of("path", candidate.toString()),
          ex);
    }
  }
}
