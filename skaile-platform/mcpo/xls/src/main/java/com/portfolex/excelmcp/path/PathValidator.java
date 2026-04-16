package com.portfolex.excelmcp.path;

import com.portfolex.excelmcp.error.ErrorCode;
import com.portfolex.excelmcp.error.McpException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Validates agent-supplied paths against the rules in §6. Stateful only in that it holds a resolved
 * {@link ExcelMcpRoot}; everything else is computed per call.
 */
public final class PathValidator {

  private final ExcelMcpRoot root;

  public PathValidator(ExcelMcpRoot root) {
    this.root = root;
  }

  /** Validate for an existing file (open/save to existing). */
  public Path validateExisting(String input) throws McpException {
    Path p = parse(input);
    if (!Files.exists(p)) {
      throw new McpException(
          ErrorCode.FILE_NOT_FOUND, "no such file: " + p, Map.of("path", p.toString()));
    }
    assertInsideRoot(p, /* allowParent= */ false);
    return p;
  }

  /** Validate for a destination path that may not yet exist (create / save-to-new). */
  public Path validateDestination(String input) throws McpException {
    Path p = parse(input);
    assertInsideRoot(p, /* allowParent= */ true);
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

  private void assertInsideRoot(Path candidate, boolean allowParent) throws McpException {
    if (!root.isEnabled()) {
      return;
    }
    Path rootPath = root.canonical().orElseThrow();
    Path canonical;
    try {
      if (Files.exists(candidate)) {
        canonical = candidate.toRealPath();
      } else if (allowParent) {
        Path parent = candidate.getParent();
        if (parent == null) {
          throw new McpException(
              ErrorCode.PATH_OUTSIDE_ROOT,
              "path has no parent to canonicalise: " + candidate,
              Map.of("path", candidate.toString(), "root", rootPath.toString()));
        }
        canonical = parent.toRealPath().resolve(candidate.getFileName());
      } else {
        throw new McpException(
            ErrorCode.FILE_NOT_FOUND,
            "no such file: " + candidate,
            Map.of("path", candidate.toString()));
      }
    } catch (IOException ex) {
      throw new McpException(
          ErrorCode.PATH_INVALID,
          "cannot canonicalise: " + ex.getMessage(),
          Map.of("path", candidate.toString()),
          ex);
    }
    if (!canonical.startsWith(rootPath)) {
      throw new McpException(
          ErrorCode.PATH_OUTSIDE_ROOT,
          "path escapes EXCEL_MCP_ROOT",
          Map.of("path", canonical.toString(), "root", rootPath.toString()));
    }
  }
}
