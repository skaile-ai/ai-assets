package com.portfolex.excelmcp.tools.workbook;

import static com.portfolex.excelmcp.server.ToolInputs.object;
import static com.portfolex.excelmcp.server.ToolInputs.requireHandle;
import static com.portfolex.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portfolex.excelmcp.engine.WorkbookEngine;
import com.portfolex.excelmcp.error.McpException;
import com.portfolex.excelmcp.handles.HandleId;
import com.portfolex.excelmcp.server.ToolDefinition;
import java.util.Map;

/** {@code workbook.list_sheets(handle)} — name, index, hidden flag per sheet. */
public final class WorkbookListSheetsTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public WorkbookListSheetsTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "workbook.list_sheets";
  }

  @Override
  public String description() {
    return "List all sheets with their name, zero-based index, and hidden flag.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set("handle", stringProp("Workbook handle."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    return Map.of("sheets", engine.listSheets(id));
  }
}
