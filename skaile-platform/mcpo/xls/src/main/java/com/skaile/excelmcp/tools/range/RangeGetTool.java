package com.skaile.excelmcp.tools.range;

import static com.skaile.excelmcp.server.ToolInputs.boolOrDefault;
import static com.skaile.excelmcp.server.ToolInputs.boolProp;
import static com.skaile.excelmcp.server.ToolInputs.intOrDefault;
import static com.skaile.excelmcp.server.ToolInputs.intProp;
import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.optionalString;
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
import com.skaile.excelmcp.shape.RangeAddress;
import java.util.Map;
import java.util.Optional;

/**
 * {@code range.get(handle, sheet, range, ...)} — return a rectangular region as {@link
 * com.skaile.excelmcp.shape.RangeShape}.
 */
public final class RangeGetTool implements ToolDefinition {

  private static final int DEFAULT_MAX_CELLS = 10_000;

  private final WorkbookEngine engine;

  public RangeGetTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "range.get";
  }

  @Override
  public String description() {
    return "Reads the cells of a rectangular range and returns them as a row-major 2D grid where"
        + " every cell carries its type, value, and (if any) typed formula. Requires an open"
        + " handle and an existing sheet; read-only. Empty cells appear as type:\"blank\" (not"
        + " omitted), formula cells with no cached result read as type:\"formula_uncomputed\""
        + " until workbook.recalculate runs, and responses beyond max_cells are truncated"
        + " row-major with truncated=true and total_cells set. The range string accepts"
        + " sheet-prefixed (\"Sheet1!A1:B2\", \"'Sheet Name'!A1:B2\") and full-column /"
        + " full-row (\"A:A\", \"1:5\") forms; if the prefix names a different sheet than the"
        + " sheet argument the call fails with RANGE_INVALID.";
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
            "A1 range string, e.g. \"A1:C10\", \"C5\" (single cell), \"A:A\" or \"A:C\""
                + " (full-column), \"1:1\" or \"1:5\" (full-row). Sheet-prefixed forms"
                + " (\"Sheet1!A1:B2\", \"'Sheet Name'!A1:B2\" — embedded apostrophes escaped"
                + " as \"''\") are accepted but if the prefix and the sheet argument disagree"
                + " the call fails with RANGE_INVALID. 3D refs (\"Sheet1:Sheet3!A1\") are not"
                + " supported. Either range or both start+end must be provided."));
    props.set(
        "start",
        stringProp(
            "Alternative to range — A1 address of the top-left cell (e.g. \"A1\"); must be"
                + " paired with end."));
    props.set(
        "end",
        stringProp(
            "Alternative to range — A1 address of the bottom-right cell (e.g. \"C10\"); must be"
                + " paired with start."));
    props.set(
        "include_formatting",
        boolProp(
            "When true, each returned cell carries a formatting sub-object (number_format, font,"
                + " fill_color, horizontal_alignment, wrap_text). Default false.",
            false));
    props.set(
        "max_cells",
        intProp(
            "Hard cap on the number of cells returned; responses larger than this are row-major"
                + " truncated and marked truncated=true with total_cells set. Units: cell count."
                + " Minimum 1; default "
                + DEFAULT_MAX_CELLS
                + ".",
            DEFAULT_MAX_CELLS));
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
    boolean includeFormatting = boolOrDefault(input, "include_formatting", false);
    int maxCells = intOrDefault(input, "max_cells", DEFAULT_MAX_CELLS);

    Optional<String> rangeStr = optionalString(input, "range");
    Optional<String> start = optionalString(input, "start");
    Optional<String> end = optionalString(input, "end");
    String a1;
    if (rangeStr.isPresent()) {
      RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet(rangeStr.get());
      RangeReferences.assertSheetMatches(pr.sheet(), sheet, rangeStr.get());
      a1 = pr.address().toA1();
    } else if (start.isPresent() && end.isPresent()) {
      a1 = RangeAddress.of(start.get(), end.get()).toA1();
    } else {
      throw new McpException(
          ErrorCode.RANGE_INVALID,
          "either 'range' or both 'start' and 'end' must be provided",
          Map.of());
    }
    return engine.readRange(id, sheet, a1, includeFormatting, maxCells);
  }
}
