package com.skaile.excelmcp.shape;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.SpreadsheetVersion;

/**
 * Normalised (startRow, startCol, endRow, endCol) range address. 0-based inclusive on both ends.
 * Parsed from A1 strings ({@code "A1"}, {@code "A1:C10"}) or expanded {@code {start, end}} form.
 *
 * <p>Full-column ({@code "A:A"}, {@code "A:C"}) and full-row ({@code "1:1"}, {@code "1:5"}) forms
 * are accepted; the missing dimension is stored as {@link SpreadsheetVersion#EXCEL2007}'s last
 * index sentinel and the engine layer clamps to the actual workbook format's bounds. A {@code
 * Sheet1!A:A} prefix-stripping form preserves the inner full-column expansion.
 */
public record RangeAddress(int startRow, int startCol, int endRow, int endCol) {

  private static final Pattern CELL = Pattern.compile("^([A-Za-z]+)(\\d+)$");
  private static final Pattern RANGE = Pattern.compile("^([A-Za-z]+)(\\d+):([A-Za-z]+)(\\d+)$");
  private static final Pattern FULL_COLUMN = Pattern.compile("^([A-Za-z]+):([A-Za-z]+)$");
  private static final Pattern FULL_ROW = Pattern.compile("^(\\d+):(\\d+)$");

  /** Sheet-prefix grammar: {@code Sheet!A1:B2} or {@code 'Quoted Sheet'!A1:B2}. */
  private static final Pattern SHEET_PREFIX =
      Pattern.compile("^(?:'((?:[^']|'')+)'|([^'!:]+))!(.+)$");

  /** 3D reference form: {@code Sheet1:Sheet3!A1}; intentionally rejected in v1. */
  private static final Pattern THREE_D_PREFIX = Pattern.compile("^[^!]+:[^!]+!.+$");

  /** Result of {@link #parseWithSheet(String)}: range address plus the optional sheet prefix. */
  public record ParsedRange(Optional<String> sheet, RangeAddress address) {}

  public int rowCount() {
    return endRow - startRow + 1;
  }

  public int colCount() {
    return endCol - startCol + 1;
  }

  public int cellCount() {
    return rowCount() * colCount();
  }

  public String toA1() {
    if (startRow == endRow && startCol == endCol) {
      return colLetter(startCol) + (startRow + 1);
    }
    return colLetter(startCol) + (startRow + 1) + ":" + colLetter(endCol) + (endRow + 1);
  }

  public static RangeAddress parse(String a1) throws McpException {
    if (a1 == null || a1.isBlank()) {
      throw invalid(a1);
    }
    String trimmed = a1.trim();
    Matcher rm = RANGE.matcher(trimmed);
    if (rm.matches()) {
      int c1 = colIndex(rm.group(1));
      int r1 = Integer.parseInt(rm.group(2)) - 1;
      int c2 = colIndex(rm.group(3));
      int r2 = Integer.parseInt(rm.group(4)) - 1;
      if (r1 < 0 || r2 < 0) throw invalid(a1);
      return new RangeAddress(
          Math.min(r1, r2), Math.min(c1, c2), Math.max(r1, r2), Math.max(c1, c2));
    }
    Matcher cm = CELL.matcher(trimmed);
    if (cm.matches()) {
      int c = colIndex(cm.group(1));
      int r = Integer.parseInt(cm.group(2)) - 1;
      if (r < 0) throw invalid(a1);
      return new RangeAddress(r, c, r, c);
    }
    Matcher colMatch = FULL_COLUMN.matcher(trimmed);
    if (colMatch.matches()) {
      int c1 = colIndex(colMatch.group(1));
      int c2 = colIndex(colMatch.group(2));
      int rowSentinel = SpreadsheetVersion.EXCEL2007.getLastRowIndex();
      return new RangeAddress(0, Math.min(c1, c2), rowSentinel, Math.max(c1, c2));
    }
    Matcher rowMatch = FULL_ROW.matcher(trimmed);
    if (rowMatch.matches()) {
      int r1 = Integer.parseInt(rowMatch.group(1)) - 1;
      int r2 = Integer.parseInt(rowMatch.group(2)) - 1;
      if (r1 < 0 || r2 < 0) throw invalid(a1);
      int colSentinel = SpreadsheetVersion.EXCEL2007.getLastColumnIndex();
      return new RangeAddress(Math.min(r1, r2), 0, Math.max(r1, r2), colSentinel);
    }
    throw invalid(a1);
  }

  /**
   * Parse with optional sheet prefix. Strips {@code Sheet!} or {@code 'Sheet Name'!} (with {@code
   * ''} as the embedded-apostrophe escape) and parses the remainder via {@link #parse(String)}.
   * Rejects 3D references ({@code Sheet1:Sheet3!A1}) up front with {@code RANGE_INVALID}.
   */
  public static ParsedRange parseWithSheet(String input) throws McpException {
    if (input == null || input.isBlank()) {
      throw invalid(input);
    }
    String trimmed = input.trim();
    Matcher prefixed = SHEET_PREFIX.matcher(trimmed);
    if (prefixed.matches()) {
      String quoted = prefixed.group(1);
      String bare = prefixed.group(2);
      String body = prefixed.group(3);
      String sheet = quoted != null ? quoted.replace("''", "'") : bare;
      return new ParsedRange(Optional.of(sheet), parse(body));
    }
    if (THREE_D_PREFIX.matcher(trimmed).matches()) {
      throw new McpException(
          ErrorCode.RANGE_INVALID,
          "3D references like 'Sheet1:Sheet3!A1' are not supported in v1: " + input,
          Map.of("input", input));
    }
    return new ParsedRange(Optional.empty(), parse(trimmed));
  }

  public static RangeAddress of(String start, String end) throws McpException {
    RangeAddress s = parse(start);
    RangeAddress e = parse(end);
    return new RangeAddress(
        Math.min(s.startRow, e.startRow),
        Math.min(s.startCol, e.startCol),
        Math.max(s.endRow, e.endRow),
        Math.max(s.endCol, e.endCol));
  }

  public static int colIndex(String letters) {
    int idx = 0;
    for (char ch : letters.toUpperCase().toCharArray()) {
      if (ch < 'A' || ch > 'Z') {
        throw new IllegalArgumentException("invalid column letters: " + letters);
      }
      idx = idx * 26 + (ch - 'A' + 1);
    }
    return idx - 1;
  }

  public static String colLetter(int col0) {
    StringBuilder sb = new StringBuilder();
    int n = col0 + 1;
    while (n > 0) {
      int rem = (n - 1) % 26;
      sb.insert(0, (char) ('A' + rem));
      n = (n - 1) / 26;
    }
    return sb.toString();
  }

  private static McpException invalid(String input) {
    return new McpException(
        ErrorCode.RANGE_INVALID,
        "Unparseable A1 reference: " + input,
        Map.of("input", String.valueOf(input)));
  }
}
