package com.skaile.excelmcp.tools.table;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
import com.skaile.excelmcp.shape.TableRef;
import java.util.List;
import java.util.Map;

/** {@code table.list(handle)} — enumerate every ListObject table across all sheets. */
public final class TableListTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public TableListTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "table.list";
  }

  @Override
  public String description() {
    return "Enumerates every ListObject table defined across every sheet, returning name, sheet,"
        + " and A1 area for each. Requires an open workbook handle; makes no changes. Only .xlsx"
        + " and .xlsm workbooks expose tables — .xls (legacy HSSF) workbooks return an empty list"
        + " because the legacy binary format has no equivalent table concept.";
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
    List<TableRef> tables = engine.listTables(id);
    return Map.of("tables", tables);
  }
}
