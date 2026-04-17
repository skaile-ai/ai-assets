package com.skaile.excelmcp.tools.workbook;

import static com.skaile.excelmcp.server.ToolInputs.object;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleRegistry;
import com.skaile.excelmcp.handles.OpenWorkbook;
import com.skaile.excelmcp.server.ToolDefinition;
import java.nio.file.Path;

/**
 * {@code workbook.list_handles()} — snapshot of every currently-registered workbook handle.
 * Diagnostic: intended for agent self-audit after an error-recovery path where a prior {@code
 * workbook.open} may have produced a handle the agent lost track of, and for operator debugging of
 * suspected handle leaks. Registry-only — no POI import, no engine call.
 */
public final class WorkbookListHandlesTool implements ToolDefinition {

  private final HandleRegistry registry;

  public WorkbookListHandlesTool(HandleRegistry registry) {
    this.registry = registry;
  }

  @Override
  public String name() {
    return "workbook.list_handles";
  }

  @Override
  public String description() {
    return "Returns every workbook handle currently held by this server session, with its source"
        + " path (or null for create-without-path workbooks), format, and open timestamp. Requires"
        + " no inputs and makes no changes; safe to call at any time including when no workbooks"
        + " are open (returns an empty array). Diagnostic tool for agent self-audit after error"
        + " recovery or for inspecting a long-running session — it does not close handles and is"
        + " not a substitute for workbook.close.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", object());
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    // Build the response as a JsonNode so the shared ShapeMapper's NON_NULL inclusion policy
    // doesn't strip source_path when it's null — plan §9.1 requires source_path to be present
    // (as JSON null) on every handle entry, including workbooks created via workbook.create
    // without a path.
    JsonNodeFactory jf = JsonNodeFactory.instance;
    ArrayNode handlesArr = jf.arrayNode();
    for (OpenWorkbook entry : registry.all()) {
      ObjectNode item = jf.objectNode();
      item.put("handle", entry.id().value());
      Path src = entry.sourcePath();
      if (src == null) {
        item.putNull("source_path");
      } else {
        item.put("source_path", src.toString());
      }
      item.put("format", entry.format());
      item.put("opened_at", entry.openedAt().toString());
      handlesArr.add(item);
    }
    ObjectNode root = jf.objectNode();
    root.set("handles", handlesArr);
    return root;
  }
}
