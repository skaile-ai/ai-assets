package com.skaile.excelmcp.tools.sheet;

import static com.skaile.excelmcp.server.ToolInputs.intOrDefault;
import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.requireInt;
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

/** {@code sheet.insert_rows(handle, sheet, start_row, count)} — insert empty rows. */
public final class SheetInsertRowsTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(SheetInsertRowsTool.class);

  private final WorkbookEngine engine;

  public SheetInsertRowsTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "sheet.insert_rows";
  }

  @Override
  public String description() {
    return "Inserts count empty rows starting at start_row; existing rows at or below that index"
        + " are shifted down by count. Requires an open workbook handle and an existing sheet;"
        + " the structural change is in-memory until workbook.save. start_row is 1-based (the"
        + " same numbering Excel shows in the row header) and must fit within the format limit"
        + " of 1,048,576 rows for .xlsx/.xlsm or 65,536 for .xls.";
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
    ObjectNode startRow = object();
    startRow.put("type", "integer");
    startRow.put(
        "description",
        "1-based row index at which to start inserting (e.g. 5 inserts above the current row 5).");
    startRow.put("minimum", 1);
    props.set("start_row", startRow);
    ObjectNode count = object();
    count.put("type", "integer");
    count.put(
        "description", "Number of empty rows to insert; must be >= 1. Defaults to 1 if omitted.");
    count.put("minimum", 1);
    count.put("default", 1);
    props.set("count", count);
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("sheet").add("start_row");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String sheet = requireString(input, "sheet");
    int startRow = requireInt(input, "start_row");
    int count = intOrDefault(input, "count", 1);
    engine.insertRows(id, sheet, startRow, count);
    log.info(
        "sheet.insert_rows handle={} sheet={} start_row={} count={}",
        id.value(),
        sheet,
        startRow,
        count);
    return Map.of("inserted_at", startRow, "count", count);
  }
}
