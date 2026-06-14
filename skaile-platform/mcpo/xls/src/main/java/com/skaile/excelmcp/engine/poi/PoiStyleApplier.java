package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.shape.CellStyleSpec;
import com.skaile.excelmcp.shape.CellStyleSpec.BorderEdge;
import com.skaile.excelmcp.shape.CellStyleSpec.BorderSpec;
import com.skaile.excelmcp.shape.CellStyleSpec.FontSpec;
import com.skaile.excelmcp.shape.RangeAddress;
import com.skaile.excelmcp.shape.SheetFormatSpec;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Applies {@link CellStyleSpec} / {@link SheetFormatSpec} to an XSSF workbook.
 *
 * <p>Two design points keep this from corrupting files the way the exceljs/openpyxl workarounds
 * did:
 *
 * <ol>
 *   <li><b>Merge, don't replace.</b> Each cell's new style is cloned from its existing style and
 *       the spec is layered on top, so unspecified attributes (e.g. a number format set by an
 *       earlier call) survive.
 *   <li><b>Dedupe styles and fonts.</b> A POI workbook is hard-capped at 64k cell styles / 32k
 *       fonts; naively calling {@code createCellStyle} per cell over a multi-thousand-cell range
 *       blows the cap and itself produces an unreadable file. Because the spec is constant within a
 *       call, every cell sharing the same <i>base</i> style collapses to a single new style (and
 *       likewise for fonts), so a uniform range costs exactly one new style.
 * </ol>
 */
public final class PoiStyleApplier {

  private PoiStyleApplier() {}

  /**
   * Styles every cell in {@code addr}, creating cells as needed. Returns the cell count touched.
   */
  public static int applyStyle(
      XSSFWorkbook wb, XSSFSheet sheet, RangeAddress addr, CellStyleSpec spec) throws McpException {
    Resolved rs = resolve(wb, spec);
    Map<Integer, XSSFCellStyle> styleCache = new HashMap<>();
    Map<Integer, XSSFFont> fontCache = new HashMap<>();
    int styled = 0;
    for (int r = addr.startRow(); r <= addr.endRow(); r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        row = sheet.createRow(r);
      }
      for (int c = addr.startCol(); c <= addr.endCol(); c++) {
        Cell cell = row.getCell(c);
        if (cell == null) {
          cell = row.createCell(c);
        }
        XSSFCellStyle base = (XSSFCellStyle) cell.getCellStyle();
        XSSFCellStyle merged =
            styleCache.computeIfAbsent(
                (int) base.getIndex(), k -> buildStyle(wb, base, rs, fontCache));
        cell.setCellStyle(merged);
        styled++;
      }
    }
    return styled;
  }

  /** Applies sheet-level presentation. Idempotent; only the supplied groups are touched. */
  public static void applyFormat(XSSFWorkbook wb, XSSFSheet sheet, SheetFormatSpec spec)
      throws McpException {
    if (spec.columnWidths() != null) {
      for (SheetFormatSpec.ColumnWidth cw : spec.columnWidths()) {
        int col;
        try {
          col = RangeAddress.colIndex(cw.column());
        } catch (IllegalArgumentException ex) {
          throw styleInvalid("invalid column letter: " + cw.column(), "column", cw.column());
        }
        if (cw.width() < 0) {
          throw styleInvalid("column width must be >= 0: " + cw.width(), "width", cw.width());
        }
        // POI column width is in 1/256th of a character. Round to the nearest unit.
        sheet.setColumnWidth(col, (int) Math.round(cw.width() * 256));
      }
    }
    if (spec.rowHeights() != null) {
      for (SheetFormatSpec.RowHeight rh : spec.rowHeights()) {
        if (rh.row() < 1) {
          throw styleInvalid("row must be >= 1: " + rh.row(), "row", rh.row());
        }
        if (rh.height() < 0) {
          throw styleInvalid("row height must be >= 0: " + rh.height(), "height", rh.height());
        }
        Row row = sheet.getRow(rh.row() - 1);
        if (row == null) {
          row = sheet.createRow(rh.row() - 1);
        }
        row.setHeightInPoints((float) rh.height());
      }
    }
    if (spec.freeze() != null) {
      int rows = spec.freeze().rows();
      int cols = spec.freeze().cols();
      if (rows < 0 || cols < 0) {
        throw styleInvalid(
            "freeze rows/cols must be >= 0: rows=" + rows + " cols=" + cols, "freeze", rows);
      }
      // createFreezePane(colSplit, rowSplit); (0, 0) removes the pane.
      sheet.createFreezePane(cols, rows);
    }
    if (spec.tabColor() != null) {
      sheet.setTabColor(color(spec.tabColor(), "tab_color"));
    }
  }

  // ── Resolution (all validation happens here, before any cell is mutated) ──

  /** Spec converted to POI types, with every value pre-validated. */
  private record Resolved(
      XSSFColor fill,
      Short numberFormat,
      HorizontalAlignment horizontal,
      VerticalAlignment vertical,
      Boolean wrapText,
      ResolvedBorder border,
      FontSpec font,
      XSSFColor fontColor) {}

  private record ResolvedBorder(
      ResolvedEdge top, ResolvedEdge bottom, ResolvedEdge left, ResolvedEdge right) {
    boolean isEmpty() {
      return top == null && bottom == null && left == null && right == null;
    }
  }

  private record ResolvedEdge(BorderStyle style, XSSFColor color) {}

  private static Resolved resolve(XSSFWorkbook wb, CellStyleSpec spec) throws McpException {
    XSSFColor fill = spec.fillColor() == null ? null : color(spec.fillColor(), "fill_color");
    Short numFmt =
        spec.numberFormat() == null ? null : wb.createDataFormat().getFormat(spec.numberFormat());
    HorizontalAlignment h =
        spec.horizontalAlignment() == null ? null : horizontal(spec.horizontalAlignment());
    VerticalAlignment v =
        spec.verticalAlignment() == null ? null : vertical(spec.verticalAlignment());
    ResolvedBorder border = resolveBorder(spec.border());
    FontSpec font = spec.font() == null || spec.font().isEmpty() ? null : spec.font();
    XSSFColor fontColor =
        font == null || font.color() == null ? null : color(font.color(), "font.color");
    return new Resolved(fill, numFmt, h, v, spec.wrapText(), border, font, fontColor);
  }

  private static ResolvedBorder resolveBorder(BorderSpec b) throws McpException {
    if (b == null || b.isEmpty()) {
      return null;
    }
    ResolvedBorder rb =
        new ResolvedBorder(
            edge(b.top(), "top"),
            edge(b.bottom(), "bottom"),
            edge(b.left(), "left"),
            edge(b.right(), "right"));
    return rb.isEmpty() ? null : rb;
  }

  private static ResolvedEdge edge(BorderEdge e, String side) throws McpException {
    if (e == null || e.style() == null) {
      return null;
    }
    BorderStyle style;
    try {
      style = BorderStyle.valueOf(e.style().trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw styleInvalid(
          "invalid border style for " + side + ": " + e.style(), "border." + side, e.style());
    }
    XSSFColor color = e.color() == null ? null : color(e.color(), "border." + side + ".color");
    return new ResolvedEdge(style, color);
  }

  // ── Style/font construction (pure — no McpException, runs inside the dedupe caches) ──

  private static XSSFCellStyle buildStyle(
      XSSFWorkbook wb, XSSFCellStyle base, Resolved rs, Map<Integer, XSSFFont> fontCache) {
    XSSFCellStyle ns = wb.createCellStyle();
    ns.cloneStyleFrom(base);
    if (rs.fill() != null) {
      ns.setFillForegroundColor(rs.fill());
      ns.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    if (rs.numberFormat() != null) {
      ns.setDataFormat(rs.numberFormat());
    }
    if (rs.horizontal() != null) {
      ns.setAlignment(rs.horizontal());
    }
    if (rs.vertical() != null) {
      ns.setVerticalAlignment(rs.vertical());
    }
    if (rs.wrapText() != null) {
      ns.setWrapText(rs.wrapText());
    }
    if (rs.border() != null) {
      applyBorder(ns, rs.border());
    }
    if (rs.font() != null) {
      XSSFFont merged =
          fontCache.computeIfAbsent(base.getFontIndexAsInt(), k -> mergeFont(wb, k, rs));
      ns.setFont(merged);
    }
    return ns;
  }

  private static void applyBorder(XSSFCellStyle ns, ResolvedBorder b) {
    if (b.top() != null) {
      ns.setBorderTop(b.top().style());
      if (b.top().color() != null) {
        ns.setTopBorderColor(b.top().color());
      }
    }
    if (b.bottom() != null) {
      ns.setBorderBottom(b.bottom().style());
      if (b.bottom().color() != null) {
        ns.setBottomBorderColor(b.bottom().color());
      }
    }
    if (b.left() != null) {
      ns.setBorderLeft(b.left().style());
      if (b.left().color() != null) {
        ns.setLeftBorderColor(b.left().color());
      }
    }
    if (b.right() != null) {
      ns.setBorderRight(b.right().style());
      if (b.right().color() != null) {
        ns.setRightBorderColor(b.right().color());
      }
    }
  }

  private static XSSFFont mergeFont(XSSFWorkbook wb, int baseFontIndex, Resolved rs) {
    XSSFFont base = wb.getFontAt(baseFontIndex);
    XSSFFont nf = wb.createFont();
    // Inherit the base font, then override only the specified attributes.
    nf.setFontName(base.getFontName());
    nf.setFontHeightInPoints(base.getFontHeightInPoints());
    nf.setBold(base.getBold());
    nf.setItalic(base.getItalic());
    XSSFColor baseColor = base.getXSSFColor();
    if (baseColor != null) {
      nf.setColor(baseColor);
    }
    FontSpec f = rs.font();
    if (f.name() != null) {
      nf.setFontName(f.name());
    }
    if (f.size() != null) {
      nf.setFontHeightInPoints((short) (int) f.size());
    }
    if (f.bold() != null) {
      nf.setBold(f.bold());
    }
    if (f.italic() != null) {
      nf.setItalic(f.italic());
    }
    if (rs.fontColor() != null) {
      nf.setColor(rs.fontColor());
    }
    return nf;
  }

  // ── Shared converters ──

  private static XSSFColor color(String hex, String field) throws McpException {
    String h = hex.startsWith("#") ? hex.substring(1) : hex;
    if (!h.matches("(?i)[0-9a-f]{6}")) {
      throw styleInvalid(
          field + " must be a 6-digit hex color like \"#7300FF\": " + hex, field, hex);
    }
    int r = Integer.parseInt(h.substring(0, 2), 16);
    int g = Integer.parseInt(h.substring(2, 4), 16);
    int b = Integer.parseInt(h.substring(4, 6), 16);
    return new XSSFColor(new byte[] {(byte) r, (byte) g, (byte) b}, null);
  }

  private static HorizontalAlignment horizontal(String s) throws McpException {
    return switch (s.trim().toLowerCase(Locale.ROOT)) {
      case "general" -> HorizontalAlignment.GENERAL;
      case "left" -> HorizontalAlignment.LEFT;
      case "center", "centre" -> HorizontalAlignment.CENTER;
      case "right" -> HorizontalAlignment.RIGHT;
      case "fill" -> HorizontalAlignment.FILL;
      case "justify" -> HorizontalAlignment.JUSTIFY;
      case "center_selection", "center-selection" -> HorizontalAlignment.CENTER_SELECTION;
      case "distributed" -> HorizontalAlignment.DISTRIBUTED;
      default ->
          throw styleInvalid("invalid horizontal_alignment: " + s, "horizontal_alignment", s);
    };
  }

  private static VerticalAlignment vertical(String s) throws McpException {
    return switch (s.trim().toLowerCase(Locale.ROOT)) {
      case "top" -> VerticalAlignment.TOP;
      case "center", "centre", "middle" -> VerticalAlignment.CENTER;
      case "bottom" -> VerticalAlignment.BOTTOM;
      case "justify" -> VerticalAlignment.JUSTIFY;
      case "distributed" -> VerticalAlignment.DISTRIBUTED;
      default -> throw styleInvalid("invalid vertical_alignment: " + s, "vertical_alignment", s);
    };
  }

  private static McpException styleInvalid(String message, String field, Object value) {
    return new McpException(
        ErrorCode.STYLE_INVALID, message, Map.of("field", field, "value", String.valueOf(value)));
  }
}
