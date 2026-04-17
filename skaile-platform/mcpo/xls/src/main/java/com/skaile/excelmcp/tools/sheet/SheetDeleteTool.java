package com.skaile.excelmcp.tools.sheet;

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

/** {@code sheet.delete(handle, name)} — remove a sheet and all of its cell data. */
public final class SheetDeleteTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(SheetDeleteTool.class);

  private final WorkbookEngine engine;

  public SheetDeleteTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "sheet.delete";
  }

  @Override
  public String description() {
    return "Removes the named sheet and all of its cells from the workbook. Requires an open"
        + " workbook handle; the deletion is in-memory until workbook.save. Excel rejects a"
        + " workbook with zero sheets as corrupt — always keep at least one sheet.";
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
        stringProp("Name of the sheet to delete; matched case-insensitively (e.g. \"Sheet1\")."));
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
    engine.deleteSheet(id, name);
    log.info("sheet.delete handle={} name={}", id.value(), name);
    return Map.of("deleted", true);
  }
}
