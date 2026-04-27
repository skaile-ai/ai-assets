package com.skaile.excelmcp.tools.namedrange;

import static com.skaile.excelmcp.server.ToolInputs.boolOrDefault;
import static com.skaile.excelmcp.server.ToolInputs.boolProp;
import static com.skaile.excelmcp.server.ToolInputs.intOrDefault;
import static com.skaile.excelmcp.server.ToolInputs.intProp;
import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.requireString;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;

/**
 * {@code named_range.get(handle, name, ...)} — read a defined name's referenced range as a
 * rectangular block.
 */
public final class NamedRangeGetTool implements ToolDefinition {

  private static final int DEFAULT_MAX_CELLS = 10_000;

  private final WorkbookEngine engine;

  public NamedRangeGetTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "named_range.get";
  }

  @Override
  public String description() {
    return "Reads the cells the given defined name points at and returns the resolved"
        + " named_range alongside the cell grid (dates are naive local timestamps like"
        + " \"2024-03-05T10:00:00\" — Excel date serials are wall-clock with no timezone)."
        + " Requires an open workbook handle and a name that resolves to a simple rectangular"
        + " A1 range (e.g. Sheet1!$A$1:$B$10); names whose refers_to is a formula expression"
        + " or a multi-sheet 3D reference are rejected with NAMED_RANGE_NOT_FOUND in v1 —"
        + " enumerate with named_range.list first if unsure.";
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
        "name",
        stringProp(
            "Defined-name identifier as authored in Excel's Name Manager, e.g. \"Total\" or"
                + " \"HeaderRow\". Matched case-sensitively in v1 — Excel treats names as"
                + " case-insensitive, so prefer the canonical spelling reported by"
                + " named_range.list when you aren't sure of the exact casing."));
    props.set(
        "include_formatting",
        boolProp(
            "When true, each returned cell carries a formatting sub-object (number_format,"
                + " font, fill_color, horizontal_alignment, wrap_text). Default false.",
            false));
    props.set(
        "max_cells",
        intProp(
            "Hard cap on the number of cells returned; responses larger than this are row-major"
                + " truncated and marked truncated=true with total_cells set. Units: cell count."
                + " Default "
                + DEFAULT_MAX_CELLS
                + ".",
            DEFAULT_MAX_CELLS));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("name");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String name = requireString(input, "name");
    boolean includeFormatting = boolOrDefault(input, "include_formatting", false);
    int maxCells = intOrDefault(input, "max_cells", DEFAULT_MAX_CELLS);
    return engine.readNamedRange(id, name, includeFormatting, maxCells);
  }
}
