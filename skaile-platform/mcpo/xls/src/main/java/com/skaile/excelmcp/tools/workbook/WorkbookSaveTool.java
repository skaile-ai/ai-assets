package com.skaile.excelmcp.tools.workbook;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.optionalString;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.path.PathValidator;
import com.skaile.excelmcp.server.ToolDefinition;
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
    return "Writes the workbook's current in-memory state to disk via an atomic temp-file +"
        + " rename, replacing the destination if it exists. Requires an open handle; defaults to"
        + " the path the workbook was opened or created with, and the handle stays valid (and"
        + " editable) after the save. If the workbook was created via workbook.create without a"
        + " path, path is required here or the call fails with SAVE_REQUIRES_PATH; the atomic"
        + " pattern means a crash mid-save leaves the prior file intact.";
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
        "path",
        stringProp(
            "Optional absolute destination path for this save, e.g. \"/data/out.xlsx\"; must end"
                + " in .xlsx/.xlsm/.xls. When EXCEL_MCP_ROOT is set, the path must resolve inside"
                + " that subtree. Overrides the workbook's remembered source path for this call"
                + " only — subsequent saves still default to the original source path."));
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
