package com.skaile.excelmcp.tools.range;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.optionalString;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.requireString;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
import com.skaile.excelmcp.shape.RangeAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code range.set(handle, sheet, range|start, values, formulas?)} — write a 2D block; recalc is
 * separate.
 */
public final class RangeSetTool implements ToolDefinition {

  private final WorkbookEngine engine;

  public RangeSetTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "range.set";
  }

  @Override
  public String description() {
    return "Write a 2D block of values and/or formulas. Does NOT auto-recalc — call workbook.recalculate to refresh cached formula results.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set("handle", stringProp("Workbook handle."));
    props.set("sheet", stringProp("Sheet name."));
    props.set(
        "range",
        stringProp(
            "Optional A1 range. If omitted, 'start' is required and the extent is inferred from values."));
    props.set("start", stringProp("Optional A1 start cell. If omitted, the range start is used."));

    ObjectNode values = object();
    values.put("type", "array");
    values.put(
        "description",
        "2D row-major array of cell values. Strings, numbers, booleans, or ISO date strings.");
    ObjectNode inner = object();
    inner.put("type", "array");
    values.set("items", inner);
    props.set("values", values);

    ObjectNode formulas = object();
    formulas.put("type", "array");
    formulas.put(
        "description",
        "Optional 2D array of formulas (same shape as values). Non-null entries override the value.");
    ObjectNode innerF = object();
    innerF.put("type", "array");
    formulas.set("items", innerF);
    props.set("formulas", formulas);

    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("sheet").add("values");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String sheet = requireString(input, "sheet");
    JsonNode valuesNode = input.get("values");
    if (valuesNode == null || !valuesNode.isArray() || valuesNode.isEmpty()) {
      throw new McpException(
          ErrorCode.RANGE_INVALID, "'values' must be a non-empty 2D array", Map.of());
    }
    List<List<Object>> values = parse2D(valuesNode);
    JsonNode formulasNode = input.get("formulas");
    List<List<String>> formulas = null;
    if (formulasNode != null && formulasNode.isArray()) {
      formulas = parse2DStrings(formulasNode);
    }

    Optional<String> rangeStr = optionalString(input, "range");
    Optional<String> startStr = optionalString(input, "start");
    int startRow;
    int startCol;
    if (startStr.isPresent()) {
      RangeAddress s = RangeAddress.parse(startStr.get());
      startRow = s.startRow();
      startCol = s.startCol();
    } else if (rangeStr.isPresent()) {
      RangeAddress r = RangeAddress.parse(rangeStr.get());
      startRow = r.startRow();
      startCol = r.startCol();
    } else {
      throw new McpException(
          ErrorCode.RANGE_INVALID, "either 'range' or 'start' must be provided", Map.of());
    }
    int written = engine.writeRange(id, sheet, startRow, startCol, values, formulas);
    return Map.of("written_cells", written);
  }

  private static List<List<Object>> parse2D(JsonNode node) {
    List<List<Object>> rows = new ArrayList<>(node.size());
    for (JsonNode rowNode : node) {
      List<Object> row = new ArrayList<>(rowNode.size());
      for (JsonNode v : rowNode) {
        row.add(jsonToJava(v));
      }
      rows.add(row);
    }
    return rows;
  }

  private static List<List<String>> parse2DStrings(JsonNode node) {
    List<List<String>> rows = new ArrayList<>(node.size());
    for (JsonNode rowNode : node) {
      List<String> row = new ArrayList<>(rowNode.size());
      for (JsonNode v : rowNode) {
        row.add(v == null || v.isNull() ? null : v.textValue());
      }
      rows.add(row);
    }
    return rows;
  }

  private static Object jsonToJava(JsonNode v) {
    if (v == null || v.isNull()) return null;
    if (v.isBoolean()) return v.booleanValue();
    if (v.isInt() || v.isLong()) return v.longValue();
    if (v.isNumber()) return v.doubleValue();
    return v.textValue();
  }
}
