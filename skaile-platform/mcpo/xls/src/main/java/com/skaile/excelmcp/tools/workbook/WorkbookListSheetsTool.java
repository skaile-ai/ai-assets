package com.skaile.excelmcp.tools.workbook;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
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
    return "Returns the ordered list of sheets in the workbook, each with its name, zero-based"
        + " index, and hidden flag. Requires an open handle; read-only. Sheet indices reflect the"
        + " current sheet order which is mutated by sheet.create / sheet.delete / sheet.rename —"
        + " don't cache them across structural edits.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "handle",
        stringProp(
            "Workbook handle previously returned by workbook.open or workbook.create; opaque"
                + " \"wb-\" prefixed string, e.g. \"wb-3f9a1c4d\"."));
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
