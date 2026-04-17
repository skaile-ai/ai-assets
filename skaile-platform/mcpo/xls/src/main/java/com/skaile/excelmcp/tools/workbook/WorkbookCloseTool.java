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

/** {@code workbook.close(handle)} — release POI resources and drop the handle. */
public final class WorkbookCloseTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(WorkbookCloseTool.class);

  private final WorkbookEngine engine;

  public WorkbookCloseTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "workbook.close";
  }

  @Override
  public String description() {
    return "Releases the in-memory workbook and drops its handle from the server's registry."
        + " Requires an open handle; subsequent tool calls on the same handle fail with"
        + " HANDLE_UNKNOWN. This does not save — any in-memory edits since the last workbook.save"
        + " are discarded, so call workbook.save first if you want to persist changes.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "handle",
        stringProp(
            "Workbook handle previously returned by workbook.open or workbook.create; opaque"
                + " \"wb-\" prefixed string, e.g. \"wb-3f9a1c4d\". Fails with HANDLE_UNKNOWN if"
                + " the id isn't currently registered."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    engine.close(id);
    log.info("workbook.close handle={}", id.value());
    return Map.of("closed", true);
  }
}
