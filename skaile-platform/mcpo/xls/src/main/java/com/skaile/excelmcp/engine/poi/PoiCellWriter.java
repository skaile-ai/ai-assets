package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.Cell;

/**
 * Writes a (value, formula?) pair to a POI cell. Formulas take precedence over values when both are
 * provided on the same cell (§9.2 range.set semantics). Setting a formula does NOT auto-recalc; the
 * caller must follow up with {@code workbook.recalculate} to refresh cached results.
 */
public final class PoiCellWriter {

  private static final DateTimeFormatter ISO_LENIENT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private PoiCellWriter() {}

  public static void write(Cell cell, Object value, String formula) throws McpException {
    if (formula != null && !formula.isBlank()) {
      try {
        String trimmed = formula.startsWith("=") ? formula.substring(1) : formula;
        cell.setCellFormula(trimmed);
      } catch (FormulaParseException e) {
        throw new McpException(
            ErrorCode.FORMULA_INVALID,
            "POI rejected formula: " + e.getMessage(),
            Map.of("formula", formula));
      }
      return;
    }
    if (value == null) {
      cell.setBlank();
      return;
    }
    if (value instanceof Boolean b) {
      cell.setCellValue(b);
    } else if (value instanceof Number n) {
      cell.setCellValue(n.doubleValue());
    } else if (value instanceof String s) {
      if (looksLikeIsoDate(s)) {
        try {
          LocalDateTime ldt = LocalDateTime.parse(s, ISO_LENIENT);
          Date d = Date.from(ldt.toInstant(ZoneOffset.UTC));
          cell.setCellValue(d);
          return;
        } catch (RuntimeException ignored) {
          // fall through to plain string
        }
      }
      cell.setCellValue(s);
    } else {
      cell.setCellValue(value.toString());
    }
  }

  private static boolean looksLikeIsoDate(String s) {
    // Cheap heuristic — avoid misclassifying "2026" as a date. Require 'T' or at least a full date.
    return s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-';
  }
}
