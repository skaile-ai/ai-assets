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
    return "Loads an Excel workbook from disk into memory and returns an opaque handle for"
        + " subsequent tool calls. The handle stays valid until workbook.close or process exit;"
        + " call workbook.save to flush in-memory edits back to disk. Only .xlsx, .xlsm, and .xls"
        + " are accepted — .xlsb is rejected with FORMAT_UNSUPPORTED, and workbooks exceeding the"
        + " configured size or cell limits (EXCEL_MCP_MAX_FILE_BYTES / EXCEL_MCP_MAX_CELLS) fail"
        + " at load.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "path",
        stringProp(
            "Absolute filesystem path to the workbook file, e.g. \"/data/report.xlsx\". Must"
                + " already exist and end in .xlsx/.xlsm/.xls (case-insensitive); .xlsb is"
                + " rejected. When EXCEL_MCP_ROOT is set, the path must resolve inside that"
                + " subtree or the call fails with PATH_OUTSIDE_ROOT."));
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
