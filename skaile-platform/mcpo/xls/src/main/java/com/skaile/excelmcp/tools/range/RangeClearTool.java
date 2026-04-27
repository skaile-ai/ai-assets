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
import com.skaile.excelmcp.shape.RangeAddress;
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
        + " runs. The range string accepts sheet-prefixed (\"Sheet1!A1:B2\") and full-column /"
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
            "A1 range to clear, e.g. \"A1:C10\" (block), \"C5\" (single cell), \"A:A\" or"
                + " \"A:C\" (full-column), \"1:1\" or \"1:5\" (full-row). Sheet-prefixed forms"
                + " (\"Sheet1!A1:B2\", \"'Sheet Name'!A1:B2\" — embedded apostrophes escaped"
                + " as \"''\") are accepted but if the prefix and the sheet argument disagree"
                + " the call fails with RANGE_INVALID. 3D refs (\"Sheet1:Sheet3!A1\") are not"
                + " supported."));
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
    RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet(range);
    RangeReferences.assertSheetMatches(pr.sheet(), sheet, range);
    int cleared = engine.clearRange(id, sheet, pr.address().toA1());
    return Map.of("cleared_cells", cleared);
  }
}
