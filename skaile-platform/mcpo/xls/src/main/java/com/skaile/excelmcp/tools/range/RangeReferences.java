package com.skaile.excelmcp.tools.range;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.util.Map;
import java.util.Optional;

/**
 * Cross-tool helper for validating sheet-prefixed range strings against the separately-passed
 * {@code sheet} parameter. Centralised so the conflict-detection rule stays consistent across
 * range.get / range.set / range.clear.
 */
final class RangeReferences {

  private RangeReferences() {}

  /**
   * Throw {@code RANGE_INVALID} when the parsed prefix and the explicit {@code sheet} arg disagree.
   * If the prefix is absent the call is a no-op. The catch-an-off-by-one rule is deliberately
   * stricter than "prefix wins": silently routing to the wrong sheet would mask an agent error.
   */
  static void assertSheetMatches(Optional<String> prefix, String sheetArg, String rangeInput)
      throws McpException {
    if (prefix.isEmpty()) {
      return;
    }
    if (sheetArg == null || sheetArg.isEmpty()) {
      return;
    }
    if (!prefix.get().equalsIgnoreCase(sheetArg)) {
      throw new McpException(
          ErrorCode.RANGE_INVALID,
          "range contains sheet prefix '"
              + prefix.get()
              + "' which conflicts with the sheet parameter '"
              + sheetArg
              + "'",
          Map.of("range", rangeInput, "sheet", sheetArg, "prefix", prefix.get()));
    }
  }
}
