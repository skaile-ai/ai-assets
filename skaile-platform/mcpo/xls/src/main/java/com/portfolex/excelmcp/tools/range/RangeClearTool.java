package com.portfolex.excelmcp.tools.range;

import static com.portfolex.excelmcp.server.ToolInputs.object;
import static com.portfolex.excelmcp.server.ToolInputs.requireHandle;
import static com.portfolex.excelmcp.server.ToolInputs.requireString;
import static com.portfolex.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portfolex.excelmcp.engine.WorkbookEngine;
import com.portfolex.excelmcp.error.McpException;
import com.portfolex.excelmcp.handles.HandleId;
import com.portfolex.excelmcp.server.ToolDefinition;
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
    return "Remove the contents of every non-empty cell in the given A1 range. Styling is preserved.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set("handle", stringProp("Workbook handle."));
    props.set("sheet", stringProp("Sheet name."));
    props.set("range", stringProp("A1 range to clear, e.g. \"A1:C10\"."));
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
