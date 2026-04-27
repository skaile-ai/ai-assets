package com.skaile.excelmcp.path;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Canonicalised form of the {@code EXCEL_MCP_ROOT} environment variable. Resolved once at startup
 * so every subsequent containment check is a prefix comparison, not a fresh {@code toRealPath}.
 */
public record ExcelMcpRoot(Optional<Path> canonical) {

  public static ExcelMcpRoot resolve(Optional<Path> configured) throws McpException {
    if (configured.isEmpty()) {
      return new ExcelMcpRoot(Optional.empty());
    }
    Path raw = configured.get();
    try {
      return new ExcelMcpRoot(Optional.of(raw.toRealPath()));
    } catch (IOException ex) {
      throw new McpException(
          ErrorCode.PATH_INVALID,
          "EXCEL_MCP_ROOT does not resolve: " + ex.getMessage(),
          Map.of("root", raw.toString()),
          ex);
    }
  }

  public boolean isEnabled() {
    return canonical.isPresent();
  }
}
