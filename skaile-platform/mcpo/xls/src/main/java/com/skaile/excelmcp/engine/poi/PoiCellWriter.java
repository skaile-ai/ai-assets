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
import org.apache.poi.xssf.usermodel.XSSFCell;

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
        // POI's setCellFormula leaves the cached result as NUMERIC 0.0, which would surface via
        // PoiCellReader as {type:"number", value:0} even though the formula has never been
        // evaluated. Plan §7.1 requires a freshly-written formula to read as
        // {type:"formula_uncomputed", value:null} until workbook.recalculate runs. Unset the <v>
        // element on XSSF cells so getCachedFormulaResultType() returns BLANK.
        if (cell instanceof XSSFCell xc && xc.getCTCell().isSetV()) {
          xc.getCTCell().unsetV();
        }
      } catch (FormulaParseException | IllegalStateException e) {
        // FormulaParseException covers syntax errors. IllegalStateException covers the
        // unlinked-external-reference case ("Book not linked for filename Foo.xlsx") that POI
        // raises when setCellFormula sees a reference like [Foo.xlsx]Sheet1!A1 without a
        // matching entry in the workbook's external-links table. Both are parser-stage
        // rejections per plan §8.2 (FORMULA_INVALID = "cannot be parsed by POI's formula
        // parser") — map both to the same code. NB: evaluator.setIgnoreMissingWorkbooks(true)
        // only helps on the recalc path for formulas already stored in the workbook; it does
        // nothing for fresh setCellFormula calls on unlinked external refs, which must be
        // rejected up front.
        throw new McpException(
            ErrorCode.FORMULA_INVALID,
            "POI rejected formula: " + e.getMessage(),
            Map.of("formula", formula, "exception", e.getClass().getSimpleName()));
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
