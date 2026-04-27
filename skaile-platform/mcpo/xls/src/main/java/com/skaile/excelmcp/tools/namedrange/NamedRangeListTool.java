package com.skaile.excelmcp.tools.namedrange;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
import com.skaile.excelmcp.shape.NamedRangeRef;
import java.util.List;
import java.util.Map;

/** {@code named_range.list(handle)} — enumerate every defined name in the workbook. */
public final class NamedRangeListTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public NamedRangeListTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "named_range.list";
  }

  @Override
  public String description() {
    return "Enumerates every defined name in the workbook (both workbook-scoped and"
        + " sheet-scoped), returning name, sheet (null for workbook scope), refers_to range,"
        + " and scope. Requires an open workbook handle; makes no changes. Names that refer to"
        + " formula expressions rather than simple ranges still appear in the list — the"
        + " expression text is in the range field as-is; treat it as opaque unless it parses as"
        + " an A1 reference.";
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
    List<NamedRangeRef> names = engine.listNamedRanges(id);
    return Map.of("named_ranges", names);
  }
}
