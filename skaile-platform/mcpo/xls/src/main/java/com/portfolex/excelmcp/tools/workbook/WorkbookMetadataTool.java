package com.portfolex.excelmcp.tools.workbook;

import static com.portfolex.excelmcp.server.ToolInputs.boolOrDefault;
import static com.portfolex.excelmcp.server.ToolInputs.boolProp;
import static com.portfolex.excelmcp.server.ToolInputs.object;
import static com.portfolex.excelmcp.server.ToolInputs.requireHandle;
import static com.portfolex.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portfolex.excelmcp.engine.WorkbookEngine;
import com.portfolex.excelmcp.error.McpException;
import com.portfolex.excelmcp.handles.HandleId;
import com.portfolex.excelmcp.server.ToolDefinition;

/**
 * {@code workbook.metadata(handle)} — filename/size/modified/format + sheets/named-ranges/tables.
 */
public final class WorkbookMetadataTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public WorkbookMetadataTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "workbook.metadata";
  }

  @Override
  public String description() {
    return "Aggregate workbook metadata: filename, size, modified time, format, sheets, and (optionally) named ranges and tables.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set("handle", stringProp("Workbook handle."));
    props.set(
        "include_named_ranges", boolProp("Include workbook-wide and sheet-scoped names.", true));
    props.set("include_tables", boolProp("Include XSSFTable (ListObject) entries.", true));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    boolean includeNames = boolOrDefault(input, "include_named_ranges", true);
    boolean includeTables = boolOrDefault(input, "include_tables", true);
    return engine.describeMetadata(id, includeNames, includeTables);
  }
}
