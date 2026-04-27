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

/**
 * {@code workbook.capabilities_report(handle)} — which Excel features the engine can and cannot
 * recalculate for this workbook.
 */
public final class WorkbookCapabilitiesReportTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public WorkbookCapabilitiesReportTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "workbook.capabilities_report";
  }

  @Override
  public String description() {
    return "Scans the loaded workbook and returns an inventory of which Excel functions and"
        + " structural features the recalculation engine can and cannot handle. Requires an open"
        + " workbook handle; makes no changes. Call this before writing or editing formulas so"
        + " you know which cells workbook.recalculate will leave with stale cached values"
        + " (FILTER, LAMBDA, dynamic-array spill, Linked Data Types, and similar post-2019"
        + " features are unsupported).";
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
    return engine.capabilitiesReport(id);
  }
}
