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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code workbook.recalculate(handle)} — refresh cached formula results in memory. */
public final class WorkbookRecalculateTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(WorkbookRecalculateTool.class);

  private final WorkbookEngine engine;

  public WorkbookRecalculateTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "workbook.recalculate";
  }

  @Override
  public String description() {
    return "Recomputes every formula in the workbook and refreshes the cached result stored on"
        + " each cell. Requires an open workbook handle; the refreshed values are in-memory only"
        + " until workbook.save. Functions added to Excel after 2019 (FILTER, SORT, LAMBDA,"
        + " dynamic-array spill, etc.) are not implemented by the engine and keep their existing"
        + " cached values — call workbook.capabilities_report first to see which formulas will be"
        + " skipped.";
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
    int evaluated = engine.recalculate(id);
    log.info("workbook.recalculate handle={} evaluated_cells={}", id.value(), evaluated);
    return Map.of("evaluated_cells", evaluated);
  }
}
