package com.portfolex.excelmcp.tools.workbook;

import static com.portfolex.excelmcp.server.ToolInputs.object;
import static com.portfolex.excelmcp.server.ToolInputs.optionalString;
import static com.portfolex.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portfolex.excelmcp.engine.WorkbookEngine;
import com.portfolex.excelmcp.error.McpException;
import com.portfolex.excelmcp.handles.HandleId;
import com.portfolex.excelmcp.path.PathValidator;
import com.portfolex.excelmcp.server.ToolDefinition;
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
    return "Create a new empty .xlsx workbook. If a path is provided, it is remembered for workbook.save.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set("path", stringProp("Optional destination path (must end in .xlsx/.xlsm/.xls)."));
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
