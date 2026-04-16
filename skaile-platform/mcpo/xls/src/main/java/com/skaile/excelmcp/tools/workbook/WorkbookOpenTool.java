package com.skaile.excelmcp.tools.workbook;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireString;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.OpenWorkbook;
import com.skaile.excelmcp.path.PathValidator;
import com.skaile.excelmcp.server.ToolDefinition;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code workbook.open(path)} — load a workbook, return a handle. */
public final class WorkbookOpenTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(WorkbookOpenTool.class);

  private final WorkbookEngine engine;
  private final PathValidator pathValidator;

  public WorkbookOpenTool(WorkbookEngine engine, PathValidator pathValidator) {
    this.engine = engine;
    this.pathValidator = pathValidator;
  }

  @Override
  public String name() {
    return "workbook.open";
  }

  @Override
  public String description() {
    return "Open a .xlsx/.xlsm/.xls workbook from disk and return an opaque handle.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "path",
        stringProp("Absolute path to the workbook (must resolve inside EXCEL_MCP_ROOT if set)."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("path");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    Path path = pathValidator.validateExisting(requireString(input, "path"));
    HandleId id = engine.open(path);
    OpenWorkbook meta = engine.describe(id);
    log.info("workbook.open handle={} format={} path={}", id.value(), meta.format(), path);
    return Map.of(
        "handle", id.value(), "format", meta.format(), "sheet_count", engine.listSheets(id).size());
  }
}
