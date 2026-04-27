package com.skaile.excelmcp.tools.sheet;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.requireString;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** {@code sheet.merged_regions(handle, sheet)} — list merged-cell ranges on a sheet. */
public final class SheetMergedRegionsTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public SheetMergedRegionsTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "sheet.merged_regions";
  }

  @Override
  public String description() {
    return "Returns the merged-cell regions defined on the given sheet as A1 ranges. Requires an"
        + " open workbook handle and an existing sheet name; makes no changes. Read-only in v1 —"
        + " creating or removing merged regions is not yet exposed, so this only reports regions"
        + " already present in the loaded workbook.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "handle",
        stringProp(
            "Workbook handle previously returned by workbook.open or workbook.create; opaque"
                + " \"wb-\" prefixed string, e.g. \"wb-3f9a1c4d\"."));
    props.set("sheet", stringProp("Sheet name; matched case-insensitively (e.g. \"Sheet1\")."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("sheet");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String sheet = requireString(input, "sheet");
    List<String> regions = engine.mergedRegions(id, sheet);
    List<Map<String, String>> out = new ArrayList<>(regions.size());
    for (String r : regions) {
      out.add(Map.of("range", r));
    }
    return Map.of("merged_regions", out);
  }
}
