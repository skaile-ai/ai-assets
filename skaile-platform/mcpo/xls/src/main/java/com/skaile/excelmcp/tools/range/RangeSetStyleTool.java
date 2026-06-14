package com.skaile.excelmcp.tools.range;

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
import com.skaile.excelmcp.shape.CellStyleSpec;
import com.skaile.excelmcp.shape.CellStyleSpec.BorderEdge;
import com.skaile.excelmcp.shape.CellStyleSpec.BorderSpec;
import com.skaile.excelmcp.shape.CellStyleSpec.FontSpec;
import com.skaile.excelmcp.shape.RangeAddress;
import java.util.Map;

/**
 * {@code range.set_style(handle, sheet, range, style)} — apply cell formatting (fill, font, border,
 * number format, alignment, wrap) to an A1 range. Merges onto existing styles; XSSF only.
 */
public final class RangeSetStyleTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public RangeSetStyleTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "range.set_style";
  }

  @Override
  public String description() {
    return "Applies cell formatting (background fill, font, borders, number format, alignment, wrap)"
        + " to every cell in an A1 range. Each attribute is independently optional and MERGES onto"
        + " each cell's existing style, so omitted attributes are left as-is (e.g. setting only a"
        + " fill keeps any existing number format). Empty cells in the range are materialised so the"
        + " style takes effect. Styles and fonts are deduplicated, so styling a uniform range is"
        + " cheap and never approaches Excel's 64k-style limit. .xlsx/.xlsm only — .xls (HSSF) fails"
        + " with STYLE_INVALID. In-memory until workbook.save. Colors are \"#RRGGBB\". Use"
        + " sheet.set_format for column widths, row heights, freeze panes, and tab colors. Full"
        + " column/row ranges (\"A:A\", \"1:5\") are rejected — pass a bounded range like \"A1:N1\".";
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
    props.set(
        "range",
        stringProp(
            "Bounded A1 range to style, e.g. \"A1:N1\" (block) or \"B2\" (single cell)."
                + " Sheet-prefixed forms (\"Sheet1!A1:B2\") are accepted but must agree with the"
                + " sheet argument. Full-column/full-row forms are not allowed."));

    ObjectNode style = object();
    style.put("type", "object");
    style.put(
        "description",
        "Style attributes to apply; every field is optional and merges onto the existing style.");
    ObjectNode sp = object();
    sp.set(
        "fill_color",
        stringProp("Solid background fill as \"#RRGGBB\", e.g. \"#7300FF\" (the # is optional)."));
    sp.set("font", fontSchema());
    sp.set("border", borderSchema());
    sp.set(
        "number_format",
        stringProp(
            "Excel number-format code applied as the cell's data format, e.g. \"#,##0\","
                + " \"0%\", \"#,##0.00\", \"\\u20ac#,##0\", \"yyyy-mm-dd\"."));
    sp.set(
        "horizontal_alignment",
        stringProp(
            "One of: general, left, center, right, fill, justify, center_selection, distributed."));
    sp.set(
        "vertical_alignment",
        stringProp("One of: top, middle (alias center), bottom, justify, distributed."));
    ObjectNode wrap = object();
    wrap.put("type", "boolean");
    wrap.put("description", "Wrap text within the cell.");
    sp.set("wrap_text", wrap);
    style.set("properties", sp);
    props.set("style", style);

    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("sheet").add("range").add("style");
    return schema;
  }

  private static ObjectNode fontSchema() {
    ObjectNode font = object();
    font.put("type", "object");
    font.put("description", "Font overrides; omitted attributes inherit the cell's current font.");
    ObjectNode fp = object();
    fp.set("name", stringProp("Font family name, e.g. \"Inter\", \"Calibri\"."));
    ObjectNode size = object();
    size.put("type", "integer");
    size.put("description", "Font size in points, e.g. 10.");
    fp.set("size", size);
    ObjectNode bold = object();
    bold.put("type", "boolean");
    bold.put("description", "Bold.");
    fp.set("bold", bold);
    ObjectNode italic = object();
    italic.put("type", "boolean");
    italic.put("description", "Italic.");
    fp.set("italic", italic);
    fp.set("color", stringProp("Font color as \"#RRGGBB\"."));
    font.set("properties", fp);
    return font;
  }

  private static ObjectNode borderSchema() {
    ObjectNode border = object();
    border.put("type", "object");
    border.put(
        "description",
        "Per-edge borders. Each edge is an object {style, color?}; omit an edge to leave it"
            + " unchanged.");
    ObjectNode bp = object();
    for (String side : new String[] {"top", "bottom", "left", "right"}) {
      ObjectNode edge = object();
      edge.put("type", "object");
      ObjectNode ep = object();
      ep.set(
          "style",
          stringProp(
              "Border style: thin, medium, thick, dashed, dotted, double, hair, none (and other"
                  + " Excel styles like mediumDashed)."));
      ep.set("color", stringProp("Edge color as \"#RRGGBB\" (defaults to automatic)."));
      edge.set("properties", ep);
      bp.set(side, edge);
    }
    border.set("properties", bp);
    return border;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String sheet = requireString(input, "sheet");
    String range = requireString(input, "range");
    JsonNode styleNode = input.get("style");
    if (styleNode == null || !styleNode.isObject()) {
      throw new McpException(
          ErrorCode.STYLE_INVALID, "'style' must be an object", Map.of("field", "style"));
    }
    CellStyleSpec spec = parseStyle(styleNode);
    if (spec.isEmpty()) {
      throw new McpException(
          ErrorCode.STYLE_INVALID,
          "'style' specifies no attributes to apply",
          Map.of("field", "style"));
    }
    RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet(range);
    RangeReferences.assertSheetMatches(pr.sheet(), sheet, range);
    int styled = engine.applyStyle(id, sheet, pr.address().toA1(), spec);
    return Map.of("styled_cells", styled);
  }

  private static CellStyleSpec parseStyle(JsonNode n) {
    return new CellStyleSpec(
        text(n, "fill_color"),
        parseFont(n.get("font")),
        parseBorder(n.get("border")),
        text(n, "number_format"),
        text(n, "horizontal_alignment"),
        text(n, "vertical_alignment"),
        bool(n, "wrap_text"));
  }

  private static FontSpec parseFont(JsonNode f) {
    if (f == null || !f.isObject()) {
      return null;
    }
    return new FontSpec(
        text(f, "name"), integer(f, "size"), bool(f, "bold"), bool(f, "italic"), text(f, "color"));
  }

  private static BorderSpec parseBorder(JsonNode b) {
    if (b == null || !b.isObject()) {
      return null;
    }
    return new BorderSpec(
        parseEdge(b.get("top")),
        parseEdge(b.get("bottom")),
        parseEdge(b.get("left")),
        parseEdge(b.get("right")));
  }

  private static BorderEdge parseEdge(JsonNode e) {
    if (e == null || !e.isObject()) {
      return null;
    }
    String style = text(e, "style");
    if (style == null) {
      return null;
    }
    return new BorderEdge(style, text(e, "color"));
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n.get(field);
    return v == null || v.isNull() || !v.isTextual() ? null : v.textValue();
  }

  private static Integer integer(JsonNode n, String field) {
    JsonNode v = n.get(field);
    return v == null || v.isNull() || !v.canConvertToInt() ? null : v.intValue();
  }

  private static Boolean bool(JsonNode n, String field) {
    JsonNode v = n.get(field);
    return v == null || v.isNull() || !v.isBoolean() ? null : v.booleanValue();
  }
}
