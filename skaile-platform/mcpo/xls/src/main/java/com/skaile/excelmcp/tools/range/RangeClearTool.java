package com.skaile.excelmcp.tools.range;

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
import java.util.Map;

/** {@code range.clear(handle, sheet, range)} — remove cell contents; styling is preserved. */
public final class RangeClearTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public RangeClearTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "range.clear";
  }

  @Override
  public String description() {
    return "Removes the contents of every non-empty cell inside the given A1 range, leaving the"
        + " surrounding cells unchanged. Requires an open handle and an existing sheet; the"
        + " change is in-memory until workbook.save. Styling, merged regions, and column widths"
        + " are preserved — only cell values and formulas are cleared. Formulas elsewhere that"
        + " reference a cleared cell will carry stale cached results until workbook.recalculate"
        + " runs.";
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
            "A1 range to clear (e.g. \"A1:C10\" for a block or \"C5\" for a single cell); must"
                + " be a bounded rectangle within the sheet's format limits."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("sheet").add("range");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String sheet = requireString(input, "sheet");
    String range = requireString(input, "range");
    int cleared = engine.clearRange(id, sheet, range);
    return Map.of("cleared_cells", cleared);
  }
}
