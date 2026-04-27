package com.skaile.excelmcp.tools.workbook;

import static com.skaile.excelmcp.server.ToolInputs.boolOrDefault;
import static com.skaile.excelmcp.server.ToolInputs.boolProp;
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
 * {@code workbook.metadata(handle)} — filename/size/modified/format + sheets/named-ranges/tables.
 */
public final class WorkbookMetadataTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public WorkbookMetadataTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "workbook.metadata";
  }

  @Override
  public String description() {
    return "Returns aggregate workbook metadata — filename, on-disk size in bytes, last-modified"
        + " timestamp, format, sheet summaries, and (optionally) the named-range and table"
        + " inventories. Requires an open handle; read-only. The on-disk size and modified fields"
        + " reflect the source file as it was last loaded or saved — in-memory edits since then"
        + " are not visible here until the next workbook.save rewrites the file.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "handle",
        stringProp(
            "Workbook handle previously returned by workbook.open or workbook.create; opaque"
                + " \"wb-\" prefixed string, e.g. \"wb-3f9a1c4d\"."));
    props.set(
        "include_named_ranges",
        boolProp(
            "When true (default), the response carries the full workbook-wide and sheet-scoped"
                + " defined-name inventory. Set false to omit the named_ranges field on large"
                + " workbooks where only top-level metadata is needed.",
            true));
    props.set(
        "include_tables",
        boolProp(
            "When true (default), the response carries every Excel table (a.k.a. ListObject)"
                + " defined across every sheet. Set false to omit the tables field — useful on"
                + " large workbooks where the table inventory is expensive and not needed.",
            true));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    boolean includeNames = boolOrDefault(input, "include_named_ranges", true);
    boolean includeTables = boolOrDefault(input, "include_tables", true);
    return engine.describeMetadata(id, includeNames, includeTables);
  }
}
