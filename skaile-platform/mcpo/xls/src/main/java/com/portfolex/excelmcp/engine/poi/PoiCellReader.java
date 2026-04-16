package com.portfolex.excelmcp.engine.poi;

import com.portfolex.excelmcp.shape.CellShape;
import com.portfolex.excelmcp.shape.CellShape.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellReference;

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
    String a1 = new CellReference(row0, col0).formatAsString(false);
    if (cell == null) {
      return CellShape.blank(a1);
    }
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      String formula = cell.getCellFormula();
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
