package com.portfolex.excelmcp.tools.workbook;

import static com.portfolex.excelmcp.server.ToolInputs.object;
import static com.portfolex.excelmcp.server.ToolInputs.optionalString;
import static com.portfolex.excelmcp.server.ToolInputs.requireHandle;
import static com.portfolex.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portfolex.excelmcp.engine.WorkbookEngine;
import com.portfolex.excelmcp.error.McpException;
import com.portfolex.excelmcp.handles.HandleId;
import com.portfolex.excelmcp.path.PathValidator;
import com.portfolex.excelmcp.server.ToolDefinition;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code workbook.save(handle, path?)} — atomic temp+rename to source or explicit destination. */
public final class WorkbookSaveTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(WorkbookSaveTool.class);

  private final WorkbookEngine engine;
  private final PathValidator pathValidator;

  public WorkbookSaveTool(WorkbookEngine engine, PathValidator pathValidator) {
    this.engine = engine;
    this.pathValidator = pathValidator;
  }

  @Override
  public String name() {
    return "workbook.save";
  }

  @Override
  public String description() {
    return "Save the workbook. Atomic write via temp+rename. Workbook stays open afterwards.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set("handle", stringProp("Workbook handle returned from workbook.open/workbook.create."));
    props.set("path", stringProp("Optional explicit destination; defaults to the source path."));
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    Optional<String> rawDest = optionalString(input, "path");
    Optional<Path> destination = Optional.empty();
    if (rawDest.isPresent()) {
      destination = Optional.of(pathValidator.validateDestination(rawDest.get()));
    }
    Path resolved = destination.orElseGet(() -> engine.peekSourcePath(id));
    long bytes = engine.save(id, destination);
    log.info("workbook.save handle={} bytes={} dest={}", id.value(), bytes, resolved);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("saved_to", resolved == null ? null : resolved.toString());
    out.put("size_bytes", bytes);
    return out;
  }
}
