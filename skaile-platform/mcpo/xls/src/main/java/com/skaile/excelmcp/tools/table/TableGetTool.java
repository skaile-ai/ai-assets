package com.skaile.excelmcp.tools.table;

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

/** {@code table.get(handle, name, ...)} — read a ListObject table as a rectangular range. */
public final class TableGetTool implements ToolDefinition {

  private static final int DEFAULT_MAX_CELLS = 10_000;

  private final WorkbookEngine engine;

  public TableGetTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "table.get";
  }

  @Override
  public String description() {
    return "Reads the contents of the named ListObject table as a rectangular range and returns"
        + " the resolved table_name alongside the cells (each cell carries type, value, and any"
        + " typed formula). Requires an open workbook handle and a table that exists — use"
        + " table.list first if you don't already know the name. The header row is included in"
        + " the cell grid; the result has the same shape as range.get (dates are naive local"
        + " timestamps like \"2024-03-05T10:00:00\" — Excel date serials are wall-clock with"
        + " no timezone), so column-header-to-value mapping is the caller's job.";
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
            "Table (ListObject) name, matched case-insensitively across every sheet (e.g."
                + " \"tblSales\"). Excel enforces workbook-wide uniqueness so the match is"
                + " unambiguous."));
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
    return engine.readTable(id, name, includeFormatting, maxCells);
  }
}
