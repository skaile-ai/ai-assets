package com.skaile.excelmcp.engine.poi;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

/**
 * Extracts the minimal §7.1 formatting map from a POI cell. Only populated when {@code
 * include_formatting=true}. HSSF colour palettes aren't exposed here; HSSF workbooks return {@code
 * null} for font colour and fill colour. See the future-work doc for richer formatting exposure.
 */
public final class PoiFormattingReader {

  private PoiFormattingReader() {}

  public static Map<String, Object> read(Cell cell) {
    if (cell == null) {
      return null;
    }
    CellStyle style = cell.getCellStyle();
    if (style == null) {
      return null;
    }
    Workbook wb = cell.getSheet().getWorkbook();

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("number_format", safeDataFormat(style));
    out.put("font", readFont(wb, style));
    out.put("fill_color", readFillColor(style));
    out.put("horizontal_alignment", alignmentWire(style.getAlignment()));
    out.put("wrap_text", style.getWrapText());
    return out;
  }

  private static String safeDataFormat(CellStyle style) {
    try {
      String fmt = style.getDataFormatString();
      return fmt == null || fmt.isEmpty() ? "General" : fmt;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static Map<String, Object> readFont(Workbook wb, CellStyle style) {
    try {
      Font font = wb.getFontAt(style.getFontIndexAsInt());
      if (font == null) {
        return null;
      }
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("bold", font.getBold());
      m.put("italic", font.getItalic());
      m.put("color", fontColor(font));
      m.put("size", (int) font.getFontHeightInPoints());
      m.put("name", font.getFontName());
      return m;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static String fontColor(Font font) {
    if (font instanceof XSSFFont xf) {
      XSSFColor c = xf.getXSSFColor();
      return hexFromXssf(c);
    }
    return null; // HSSF palette lookup intentionally deferred; see future-work.
  }

  private static String readFillColor(CellStyle style) {
    FillPatternType pattern;
    try {
      pattern = style.getFillPattern();
    } catch (RuntimeException ignored) {
      return null;
    }
    if (pattern == null || pattern == FillPatternType.NO_FILL) {
      return null;
    }
    if (style instanceof XSSFCellStyle xs) {
      XSSFColor c = xs.getFillForegroundXSSFColor();
      return hexFromXssf(c);
    }
    return null;
  }

  private static String hexFromXssf(XSSFColor c) {
    if (c == null) {
      return null;
    }
    String argb = c.getARGBHex();
    if (argb == null) {
      return null;
    }
    // Strip leading alpha if present (POI returns AARRGGBB); emit #RRGGBB.
    String rgb = argb.length() == 8 ? argb.substring(2) : argb;
    return "#" + rgb.toUpperCase(Locale.ROOT);
  }

  private static String alignmentWire(HorizontalAlignment a) {
    if (a == null) {
      return "general";
    }
    return switch (a) {
      case LEFT -> "left";
      case CENTER, CENTER_SELECTION -> "center";
      case RIGHT -> "right";
      case FILL, JUSTIFY, DISTRIBUTED -> a.name().toLowerCase(Locale.ROOT);
      case GENERAL -> "general";
    };
  }
}
