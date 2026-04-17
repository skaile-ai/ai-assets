package com.skaile.excelmcp.tools.vba;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
import com.skaile.excelmcp.shape.VbaModuleShape;
import java.util.List;
import java.util.Map;

/** {@code vba.list_modules(handle)} — enumerate VBA modules embedded in the workbook. */
public final class VbaListModulesTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public VbaListModulesTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "vba.list_modules";
  }

  @Override
  public String description() {
    return "Lists every VBA module embedded in the workbook's macro project: Document modules"
        + " (per-sheet ThisWorkbook / Sheet1), standard Module entries, and Class modules."
        + " Requires an open workbook handle whose source file on disk actually contains a VBA"
        + " project — workbooks created via workbook.create() or saved as plain .xlsx (not"
        + " .xlsm) raise VBA_NOT_PRESENT. UserForms are not exposed in v1 (extraction is"
        + " known-incomplete across Java and Python tooling) — only the three listed module"
        + " types appear.";
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
    List<VbaModuleShape> modules = engine.listVbaModules(id);
    return Map.of("modules", modules);
  }
}
