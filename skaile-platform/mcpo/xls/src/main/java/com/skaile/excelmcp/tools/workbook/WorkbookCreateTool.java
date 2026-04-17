package com.skaile.excelmcp.tools.workbook;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.optionalString;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.path.PathValidator;
import com.skaile.excelmcp.server.ToolDefinition;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code workbook.create(path?)} — create an empty .xlsx; path is remembered for later save. */
public final class WorkbookCreateTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(WorkbookCreateTool.class);

  private final WorkbookEngine engine;
  private final PathValidator pathValidator;

  public WorkbookCreateTool(WorkbookEngine engine, PathValidator pathValidator) {
    this.engine = engine;
    this.pathValidator = pathValidator;
  }

  @Override
  public String name() {
    return "workbook.create";
  }

  @Override
  public String description() {
    return "Creates a new empty .xlsx workbook in memory with a single default \"Sheet1\" and"
        + " returns an opaque handle. If a path is provided it is validated now and remembered so"
        + " a later workbook.save can omit its destination argument; otherwise the first"
        + " workbook.save must pass an explicit path. Nothing is written to disk until save —"
        + " closing without saving silently discards the workbook.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "path",
        stringProp(
            "Optional absolute destination path for later save, e.g. \"/data/new.xlsx\". Must end"
                + " in .xlsx/.xlsm/.xls (case-insensitive); .xlsb is rejected. When"
                + " EXCEL_MCP_ROOT is set, the path must resolve inside that subtree. Validated"
                + " now, written only by workbook.save — omit to defer path selection."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    Optional<String> raw = optionalString(input, "path");
    Optional<Path> validated = Optional.empty();
    if (raw.isPresent()) {
      validated = Optional.of(pathValidator.validateDestination(raw.get()));
    }
    HandleId id = engine.create(validated);
    log.info("workbook.create handle={} sourcePath={}", id.value(), validated.orElse(null));
    return Map.of("handle", id.value());
  }
}
