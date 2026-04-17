package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.shape.CellShape;
import com.skaile.excelmcp.shape.CellShape.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;

/**
 * Translates a POI {@link Cell} into a {@link CellShape}. Always exposes the typed formula string
 * <em>and</em> the cached/computed value when both are present (§7.1). Formula-bearing cells whose
 * cached result has been cleared surface as {@code formula_uncomputed} so the agent can see that a
 * recalc is needed.
 */
public final class PoiCellReader {

  private static final DateTimeFormatter ISO_NAIVE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

  private PoiCellReader() {}

  public static CellShape read(Cell cell, int row0, int col0) {
    return read(cell, row0, col0, false);
  }

  public static CellShape read(Cell cell, int row0, int col0, boolean includeFormatting) {
    String a1 = new CellReference(row0, col0).formatAsString(false);
    CellShape base = readBase(cell, a1);
    if (!includeFormatting || cell == null) {
      return base;
    }
    Map<String, Object> fmt = PoiFormattingReader.read(cell);
    if (fmt == null) {
      return base;
    }
    return new CellShape(base.a1(), base.type(), base.value(), base.formula(), fmt);
  }

  private static CellShape readBase(Cell cell, String a1) {
    if (cell == null) {
      return CellShape.blank(a1);
    }
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      String formula = cell.getCellFormula();
      // Plan §7.1: a freshly-written formula with no cached value must surface as
      // formula_uncomputed. POI's getCachedFormulaResultType() returns NUMERIC whenever the
      // cell's t attribute is absent or "n" (the default for setCellFormula on a fresh cell),
      // and getNumericCellValue() then returns 0.0 — so the cached-type hint cannot be trusted
      // on its own. The authoritative "has a cached value" signal on XSSF is the presence of
      // the <v> element; if it's absent the cell has never been evaluated.
      if (cell instanceof XSSFCell xc && !xc.getCTCell().isSetV()) {
        return CellShape.uncomputedFormula(a1, formula);
      }
      CellType cached = cell.getCachedFormulaResultType();
      return switch (cached) {
        case NUMERIC -> CellShape.formula(a1, numericType(cell), numericValue(cell), formula);
        case STRING -> CellShape.formula(a1, Type.STRING, cell.getStringCellValue(), formula);
        case BOOLEAN -> CellShape.formula(a1, Type.BOOLEAN, cell.getBooleanCellValue(), formula);
        case ERROR -> CellShape.formula(a1, Type.ERROR, errorString(cell), formula);
        case BLANK -> CellShape.uncomputedFormula(a1, formula);
        default -> CellShape.uncomputedFormula(a1, formula);
      };
    }
    return switch (type) {
      case NUMERIC -> CellShape.value(a1, numericType(cell), numericValue(cell));
      case STRING -> CellShape.value(a1, Type.STRING, cell.getStringCellValue());
      case BOOLEAN -> CellShape.value(a1, Type.BOOLEAN, cell.getBooleanCellValue());
      case ERROR -> CellShape.value(a1, Type.ERROR, errorString(cell));
      case BLANK, _NONE -> CellShape.blank(a1);
      default -> CellShape.blank(a1);
    };
  }

  private static Type numericType(Cell cell) {
    return DateUtil.isCellDateFormatted(cell) ? Type.DATE : Type.NUMBER;
  }

  private static Object numericValue(Cell cell) {
    if (DateUtil.isCellDateFormatted(cell)) {
      Date d = cell.getDateCellValue();
      if (d == null) return null;
      LocalDateTime ldt =
          LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneOffset.UTC);
      return ldt.format(ISO_NAIVE);
    }
    double v = cell.getNumericCellValue();
    if (v == Math.floor(v) && !Double.isInfinite(v) && Math.abs(v) < 1e15) {
      return (long) v;
    }
    return v;
  }

  private static String errorString(Cell cell) {
    try {
      return ErrorEval.valueOf(cell.getErrorCellValue()).getErrorString();
    } catch (Exception ignored) {
      return "#UNKNOWN!";
    }
  }
}
