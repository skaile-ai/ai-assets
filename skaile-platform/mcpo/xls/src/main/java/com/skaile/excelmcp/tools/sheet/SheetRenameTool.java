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

/** {@code sheet.rename(handle, old_name, new_name)} — rename in place. */
public final class SheetRenameTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(SheetRenameTool.class);

  private final WorkbookEngine engine;

  public SheetRenameTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "sheet.rename";
  }

  @Override
  public String description() {
    return "Renames an existing sheet; POI rewrites formulas that reference the old name"
        + " automatically. Requires an open workbook handle; the rename is in-memory until"
        + " workbook.save. Names are case-insensitive for uniqueness — renaming to a value that"
        + " already exists (ignoring case) fails with SHEET_ALREADY_EXISTS.";
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
        "old_name",
        stringProp(
            "Current name of the sheet to rename; matched case-insensitively (e.g."
                + " \"Sheet1\")."));
    props.set(
        "new_name",
        stringProp(
            "New sheet name; 1–31 chars, must not contain : \\ / ? * [ ] or start/end with an"
                + " apostrophe."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("old_name").add("new_name");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String oldName = requireString(input, "old_name");
    String newName = requireString(input, "new_name");
    engine.renameSheet(id, oldName, newName);
    log.info("sheet.rename handle={} {} -> {}", id.value(), oldName, newName);
    return Map.of("old_name", oldName, "new_name", newName);
  }
}
