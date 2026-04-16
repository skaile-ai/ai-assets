package com.portfolex.excelmcp.tools.range;

import static com.portfolex.excelmcp.server.ToolInputs.boolOrDefault;
import static com.portfolex.excelmcp.server.ToolInputs.boolProp;
import static com.portfolex.excelmcp.server.ToolInputs.intOrDefault;
import static com.portfolex.excelmcp.server.ToolInputs.intProp;
import static com.portfolex.excelmcp.server.ToolInputs.object;
import static com.portfolex.excelmcp.server.ToolInputs.optionalString;
import static com.portfolex.excelmcp.server.ToolInputs.requireHandle;
import static com.portfolex.excelmcp.server.ToolInputs.requireString;
import static com.portfolex.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portfolex.excelmcp.engine.WorkbookEngine;
import com.portfolex.excelmcp.error.ErrorCode;
import com.portfolex.excelmcp.error.McpException;
import com.portfolex.excelmcp.handles.HandleId;
import com.portfolex.excelmcp.server.ToolDefinition;
import com.portfolex.excelmcp.shape.RangeAddress;
import java.util.Map;
import java.util.Optional;

/**
 * {@code range.get(handle, sheet, range, ...)} — return a rectangular region as {@link
 * com.portfolex.excelmcp.shape.RangeShape}.
 */
public final class RangeGetTool implements ToolDefinition {

  private static final int DEFAULT_MAX_CELLS = 10_000;

  private final WorkbookEngine engine;

  public RangeGetTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "range.get";
  }

  @Override
  public String description() {
    return "Read a rectangular range. Each cell carries type, value, and (if any) the typed formula. Set include_formatting=true to include styling.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set("handle", stringProp("Workbook handle."));
    props.set("sheet", stringProp("Sheet name."));
    props.set("range", stringProp("A1 range, e.g. \"A1:C10\" or \"C5\"; OR set start+end."));
    props.set("start", stringProp("Optional — alternative to range. Paired with end."));
    props.set("end", stringProp("Optional — alternative to range. Paired with start."));
    props.set(
        "include_formatting", boolProp("Include font/fill/alignment where available.", false));
    props.set(
        "max_cells",
        intProp(
            "Truncate output beyond this count; default " + DEFAULT_MAX_CELLS + ".",
            DEFAULT_MAX_CELLS));
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
    boolean includeFormatting = boolOrDefault(input, "include_formatting", false);
    int maxCells = intOrDefault(input, "max_cells", DEFAULT_MAX_CELLS);

    Optional<String> rangeStr = optionalString(input, "range");
    Optional<String> start = optionalString(input, "start");
    Optional<String> end = optionalString(input, "end");
    String a1;
    if (rangeStr.isPresent()) {
      a1 = rangeStr.get();
    } else if (start.isPresent() && end.isPresent()) {
      a1 = RangeAddress.of(start.get(), end.get()).toA1();
    } else {
      throw new McpException(
          ErrorCode.RANGE_INVALID,
          "either 'range' or both 'start' and 'end' must be provided",
          Map.of());
    }
    return engine.readRange(id, sheet, a1, includeFormatting, maxCells);
  }
}
