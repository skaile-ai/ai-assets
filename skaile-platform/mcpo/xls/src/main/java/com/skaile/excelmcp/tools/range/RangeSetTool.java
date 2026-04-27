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
    return "Writes a 2D block of values and (optionally) formulas into a sheet starting at a"
        + " given A1 position; cells outside the block are untouched. Requires an open handle"
        + " and an existing sheet; the change is in-memory until workbook.save and does NOT"
        + " auto-recalculate — call workbook.recalculate after setting formulas to refresh"
        + " cached results. If formulas is supplied it must be the same 2D shape as values"
        + " (mismatched shapes fail with RANGE_INVALID); non-null formula entries override the"
        + " matching value, and unlinked external references are rejected at write time with"
        + " FORMULA_INVALID. Sheet-prefixed range/start strings (\"Sheet1!A1\") are accepted"
        + " but if the prefix and the sheet argument disagree the call fails with"
        + " RANGE_INVALID.";
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
        "sheet",
        stringProp(
            "Sheet name matched case-insensitively (e.g. \"Sheet1\"); fails with SHEET_NOT_FOUND"
                + " if no match."));
    props.set(
        "range",
        stringProp(
            "Optional A1 range that explicitly bounds the write, e.g. \"A1:B2\". When omitted,"
                + " start is required and the extent is inferred from the values array's"
                + " dimensions. If provided, its shape must match the values dimensions."
                + " Sheet-prefixed forms (\"Sheet1!A1:B2\") are accepted but conflict with the"
                + " sheet argument if they disagree."));
    props.set(
        "start",
        stringProp(
            "Alternative to range — A1 address of the top-left cell where writing begins (e.g."
                + " \"A1\"). Extent is inferred from the values array's shape. Sheet-prefixed"
                + " forms (\"Sheet1!A1\") are accepted but conflict with the sheet argument if"
                + " they disagree."));

    ObjectNode values = object();
    values.put("type", "array");
    values.put(
        "description",
        "Required 2D row-major array of cell values. Each inner array is one row; entries may"
            + " be JSON strings, numbers (integer or float), booleans, ISO-8601 date strings"
            + " (\"2026-04-17T00:00:00\", naive — no timezone), or null. Null entries paired"
            + " with a non-null formulas entry at the same position write a formula-only cell;"
            + " null entries with no formula leave that cell unchanged.");
    ObjectNode inner = object();
    inner.put("type", "array");
    values.set("items", inner);
    props.set("values", values);

    ObjectNode formulas = object();
    formulas.put("type", "array");
    formulas.put(
        "description",
        "Optional 2D array with the same shape as values. Non-null entries (e.g."
            + " \"=B2*1.5\" — the leading equals sign is optional, both \"=B2*1.5\" and"
            + " \"B2*1.5\" are accepted) are stored as cell formulas and override the"
            + " corresponding value at that position. Omit entirely for pure-value writes."
            + " Formulas referencing an unopened external workbook are rejected up front with"
            + " FORMULA_INVALID.");
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
    if (formulasNode != null && formulasNode.isArray() && !formulasNode.isEmpty()) {
      formulas = parse2DStrings(formulasNode);
      assertSameShape(values, formulas);
    }

    Optional<String> rangeStr = optionalString(input, "range");
    Optional<String> startStr = optionalString(input, "start");
    int startRow;
    int startCol;
    if (startStr.isPresent()) {
      RangeAddress.ParsedRange ps = RangeAddress.parseWithSheet(startStr.get());
      RangeReferences.assertSheetMatches(ps.sheet(), sheet, startStr.get());
      startRow = ps.address().startRow();
      startCol = ps.address().startCol();
    } else if (rangeStr.isPresent()) {
      RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet(rangeStr.get());
      RangeReferences.assertSheetMatches(pr.sheet(), sheet, rangeStr.get());
      startRow = pr.address().startRow();
      startCol = pr.address().startCol();
    } else {
      throw new McpException(
          ErrorCode.RANGE_INVALID, "either 'range' or 'start' must be provided", Map.of());
    }
    int written = engine.writeRange(id, sheet, startRow, startCol, values, formulas);
    return Map.of("written_cells", written);
  }

  private static void assertSameShape(List<List<Object>> values, List<List<String>> formulas)
      throws McpException {
    int vRows = values.size();
    int fRows = formulas.size();
    int vCols = values.isEmpty() ? 0 : values.get(0).size();
    int fCols = formulas.isEmpty() ? 0 : formulas.get(0).size();
    if (vRows != fRows) {
      throw new McpException(
          ErrorCode.RANGE_INVALID,
          "values and formulas must have the same shape: values="
              + vRows
              + "x"
              + vCols
              + ", formulas="
              + fRows
              + "x"
              + fCols,
          Map.of(
              "values_rows", vRows,
              "values_cols", vCols,
              "formulas_rows", fRows,
              "formulas_cols", fCols));
    }
    for (int i = 0; i < vRows; i++) {
      int vc = values.get(i).size();
      int fc = formulas.get(i).size();
      if (vc != fc) {
        throw new McpException(
            ErrorCode.RANGE_INVALID,
            "values and formulas row " + i + " differ in length: values=" + vc + ", formulas=" + fc,
            Map.of("row", i, "values_cols", vc, "formulas_cols", fc));
      }
    }
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
