package com.skaile.excelmcp.tools.sheet;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.requireString;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
import com.skaile.excelmcp.shape.SheetFormatSpec;
import com.skaile.excelmcp.shape.SheetFormatSpec.ColumnWidth;
import com.skaile.excelmcp.shape.SheetFormatSpec.FreezePane;
import com.skaile.excelmcp.shape.SheetFormatSpec.RowHeight;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code sheet.set_format(handle, sheet, ...)} — sheet-level presentation: column widths, row
 * heights, a frozen header pane, and the worksheet tab color. XSSF only.
 */
public final class SheetSetFormatTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public SheetSetFormatTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "sheet.set_format";
  }

  @Override
  public String description() {
    return "Sets sheet-level presentation: column widths, row heights, a frozen header pane, and the"
        + " worksheet tab color. Every group is optional; only the supplied groups change. Use"
        + " range.set_style for per-cell formatting. .xlsx/.xlsm only — .xls (HSSF) fails with"
        + " STYLE_INVALID. In-memory until workbook.save.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "handle",
        stringProp(
            "Workbook handle previously returned by workbook.open or workbook.create; opaque"
                + " \"wb-\" prefixed string, e.g. \"wb-3f9a1c4d\"."));
    props.set(
        "sheet",
        stringProp(
            "Sheet name matched case-insensitively (e.g. \"Sheet1\"); fails with SHEET_NOT_FOUND"
                + " if no match."));

    ObjectNode colWidths = object();
    colWidths.put("type", "array");
    colWidths.put(
        "description",
        "Column widths. Array of {column, width} where column is a letter (\"A\", \"AB\") and width"
            + " is in Excel character units (e.g. 12).");
    ObjectNode cwItem = object();
    cwItem.put("type", "object");
    ObjectNode cwProps = object();
    cwProps.set("column", stringProp("Column letter, e.g. \"A\"."));
    ObjectNode cwWidth = object();
    cwWidth.put("type", "number");
    cwWidth.put("description", "Width in Excel character units.");
    cwProps.set("width", cwWidth);
    cwItem.set("properties", cwProps);
    cwItem.putArray("required").add("column").add("width");
    colWidths.set("items", cwItem);
    props.set("column_widths", colWidths);

    ObjectNode rowHeights = object();
    rowHeights.put("type", "array");
    rowHeights.put(
        "description",
        "Row heights. Array of {row, height} where row is 1-based and height is in points (e.g."
            + " 28).");
    ObjectNode rhItem = object();
    rhItem.put("type", "object");
    ObjectNode rhProps = object();
    ObjectNode rhRow = object();
    rhRow.put("type", "integer");
    rhRow.put("description", "1-based row number.");
    rhProps.set("row", rhRow);
    ObjectNode rhHeight = object();
    rhHeight.put("type", "number");
    rhHeight.put("description", "Row height in points.");
    rhProps.set("height", rhHeight);
    rhItem.set("properties", rhProps);
    rhItem.putArray("required").add("row").add("height");
    rowHeights.set("items", rhItem);
    props.set("row_heights", rowHeights);

    ObjectNode freeze = object();
    freeze.put("type", "object");
    freeze.put(
        "description",
        "Freeze leading rows/cols (a header freeze is {rows:1, cols:0}); {rows:0, cols:0} unfreezes.");
    ObjectNode fProps = object();
    ObjectNode fRows = object();
    fRows.put("type", "integer");
    fRows.put("description", "Number of leading rows to freeze.");
    fProps.set("rows", fRows);
    ObjectNode fCols = object();
    fCols.put("type", "integer");
    fCols.put("description", "Number of leading columns to freeze.");
    fProps.set("cols", fCols);
    freeze.set("properties", fProps);
    props.set("freeze", freeze);

    props.set("tab_color", stringProp("Worksheet tab color as \"#RRGGBB\"."));

    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("sheet");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String sheet = requireString(input, "sheet");
    SheetFormatSpec spec =
        new SheetFormatSpec(
            parseColumnWidths(input.get("column_widths")),
            parseRowHeights(input.get("row_heights")),
            parseFreeze(input.get("freeze")),
            text(input, "tab_color"));
    if (spec.isEmpty()) {
      throw new McpException(
          ErrorCode.STYLE_INVALID,
          "no formatting supplied; provide at least one of column_widths, row_heights, freeze,"
              + " tab_color",
          Map.of());
    }
    engine.applySheetFormat(id, sheet, spec);
    return Map.of("ok", true);
  }

  private static List<ColumnWidth> parseColumnWidths(JsonNode n) throws McpException {
    if (n == null || !n.isArray()) {
      return null;
    }
    List<ColumnWidth> out = new ArrayList<>(n.size());
    for (JsonNode e : n) {
      String col = text(e, "column");
      JsonNode w = e.get("width");
      if (col == null || w == null || !w.isNumber()) {
        throw new McpException(
            ErrorCode.STYLE_INVALID,
            "each column_widths entry needs a string 'column' and numeric 'width'",
            Map.of());
      }
      out.add(new ColumnWidth(col, w.doubleValue()));
    }
    return out;
  }

  private static List<RowHeight> parseRowHeights(JsonNode n) throws McpException {
    if (n == null || !n.isArray()) {
      return null;
    }
    List<RowHeight> out = new ArrayList<>(n.size());
    for (JsonNode e : n) {
      JsonNode row = e.get("row");
      JsonNode h = e.get("height");
      if (row == null || !row.canConvertToInt() || h == null || !h.isNumber()) {
        throw new McpException(
            ErrorCode.STYLE_INVALID,
            "each row_heights entry needs an integer 'row' and numeric 'height'",
            Map.of());
      }
      out.add(new RowHeight(row.intValue(), h.doubleValue()));
    }
    return out;
  }

  private static FreezePane parseFreeze(JsonNode n) {
    if (n == null || !n.isObject()) {
      return null;
    }
    int rows =
        n.get("rows") != null && n.get("rows").canConvertToInt() ? n.get("rows").intValue() : 0;
    int cols =
        n.get("cols") != null && n.get("cols").canConvertToInt() ? n.get("cols").intValue() : 0;
    return new FreezePane(rows, cols);
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n == null ? null : n.get(field);
    return v == null || v.isNull() || !v.isTextual() ? null : v.textValue();
  }
}
