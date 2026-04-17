package com.skaile.excelmcp.tools.sheet;

import static com.skaile.excelmcp.server.ToolInputs.intOrDefault;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code sheet.delete_cols(handle, sheet, start_col, count)} — remove columns. */
public final class SheetDeleteColsTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(SheetDeleteColsTool.class);

  private final WorkbookEngine engine;

  public SheetDeleteColsTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "sheet.delete_cols";
  }

  @Override
  public String description() {
    return "Deletes count columns starting at start_col; columns to the right are shifted left"
        + " by count. Requires an open workbook handle and an existing sheet; the structural"
        + " change is in-memory until workbook.save. start_col is 1-based (A=1, B=2, …); column"
        + " structural edits are not available on legacy .xls workbooks — the call fails with"
        + " INTERNAL_ERROR on that format, so convert to .xlsx/.xlsm first when columns need to"
        + " shift.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "handle",
        stringProp(
            "Workbook handle previously returned by workbook.open or workbook.create; opaque"
                + " \"wb-\" prefixed string, e.g. \"wb-3f9a1c4d\"."));
    props.set("sheet", stringProp("Sheet name; matched case-insensitively (e.g. \"Sheet1\")."));
    ObjectNode startCol = object();
    startCol.put("type", "integer");
    startCol.put("description", "1-based column index of the first column to delete (A=1).");
    startCol.put("minimum", 1);
    props.set("start_col", startCol);
    ObjectNode count = object();
    count.put("type", "integer");
    count.put(
        "description", "Number of contiguous columns to delete; must be >= 1. Defaults to 1.");
    count.put("minimum", 1);
    count.put("default", 1);
    props.set("count", count);
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("sheet").add("start_col");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String sheet = requireString(input, "sheet");
    int startCol = intOrDefault(input, "start_col", 0);
    int count = intOrDefault(input, "count", 1);
    engine.deleteCols(id, sheet, startCol, count);
    log.info(
        "sheet.delete_cols handle={} sheet={} start_col={} count={}",
        id.value(),
        sheet,
        startCol,
        count);
    return Map.of("deleted_at", startCol, "count", count);
  }
}
