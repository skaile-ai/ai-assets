package com.skaile.excelmcp.shape;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalised (startRow, startCol, endRow, endCol) range address. 0-based inclusive on both ends.
 * Parsed from A1 strings ({@code "A1"}, {@code "A1:C10"}) or expanded {@code {start, end}} form.
 */
public record RangeAddress(int startRow, int startCol, int endRow, int endCol) {

  private static final Pattern CELL = Pattern.compile("^([A-Za-z]+)(\\d+)$");
  private static final Pattern RANGE = Pattern.compile("^([A-Za-z]+)(\\d+):([A-Za-z]+)(\\d+)$");

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
    throw invalid(a1);
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
