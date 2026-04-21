package com.skaile.excelmcp.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skaile.excelmcp.config.ServerConfig;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.engine.XssfInMemoryEngine;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.HandleRegistry;
import com.skaile.excelmcp.shape.RangeAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.opentest4j.AssertionFailedError;

/**
 * Replays a suite's {@code request.json} against {@code template.xlsx} via {@link WorkbookEngine}
 * in-process and diffs the result against {@code expected.xlsx} via {@link WorkbookDiff}.
 *
 * <p>Equivalent of xlport's {@code TestSuiteExporter.runSingleTestInSuite}. The engine is driven
 * directly (not through the MCP tool layer) because the tool layer is a thin JSON unwrapper — it
 * adds no logic worth regression-testing here. See §4 of {@code deterministic-test-suites-plan.md}.
 */
public final class TestSuiteRunner {

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final ServerConfig TEST_CONFIG =
      new ServerConfig(
          Optional.empty(),
          true,
          ServerConfig.DEFAULT_MAX_FILE_BYTES,
          ServerConfig.DEFAULT_MAX_CELLS);

  private TestSuiteRunner() {}

  /** Optional per-suite hook for checks beyond what {@link WorkbookDiff} covers. */
  @FunctionalInterface
  public interface SuiteExtension {
    void check(Workbook expected, Workbook actual) throws Exception;
  }

  /**
   * Replay the suite at {@code dir} and assert the saved workbook matches {@code expected.xlsx}.
   *
   * @throws AssertionFailedError if the diff is non-empty, an extension rejects the result, or any
   *     suite fixture is missing / malformed.
   */
  public static void runSuite(Path dir, SuiteExtension... extensions) throws Exception {
    Path template = dir.resolve("template.xlsx");
    Path request = dir.resolve("request.json");
    Path expected = dir.resolve("expected.xlsx");
    requireRegularFile(template, dir);
    requireRegularFile(request, dir);
    requireRegularFile(expected, dir);

    SuiteRequest req;
    try {
      req = JSON.readValue(request.toFile(), SuiteRequest.class);
    } catch (Exception ex) {
      throw new AssertionFailedError(
          "suite [" + dir.getFileName() + "] has a malformed request.json: " + ex.getMessage(), ex);
    }

    Path work = Files.createTempFile("excelmcp-suite-", ".xlsx");
    Files.copy(template, work, StandardCopyOption.REPLACE_EXISTING);
    try {
      HandleRegistry registry = new HandleRegistry();
      try (WorkbookEngine engine = new XssfInMemoryEngine(TEST_CONFIG, registry)) {
        HandleId id = engine.open(work);
        List<SuiteRequest.Operation> ops = req.operations() == null ? List.of() : req.operations();
        for (int i = 0; i < ops.size(); i++) {
          SuiteRequest.Operation op = ops.get(i);
          try {
            dispatch(engine, id, op);
          } catch (Exception ex) {
            throw new AssertionFailedError(
                "suite ["
                    + dir.getFileName()
                    + "] failed at operations["
                    + i
                    + "] ("
                    + op.tool()
                    + "): "
                    + ex.getMessage(),
                ex);
          }
        }
        engine.save(id, Optional.empty());
        engine.close(id);
      }

      try (Workbook expectedWb = WorkbookFactory.create(expected.toFile(), null, true);
          Workbook actualWb = WorkbookFactory.create(work.toFile(), null, true)) {
        List<String> errors = WorkbookDiff.diff(expectedWb, actualWb);
        if (!errors.isEmpty()) {
          StringBuilder sb = new StringBuilder();
          for (String e : errors) sb.append("\n  ").append(e);
          throw new AssertionFailedError("Error in test [" + dir.getFileName() + "]:" + sb);
        }
        for (SuiteExtension ext : extensions) {
          ext.check(expectedWb, actualWb);
        }
      }
    } finally {
      try {
        Files.deleteIfExists(work);
      } catch (Exception ignored) {
        // Best-effort cleanup; the OS temp reaper handles leaks.
      }
    }
  }

  private static void requireRegularFile(Path p, Path dir) {
    if (!Files.isRegularFile(p)) {
      throw new AssertionFailedError(
          "suite [" + dir.getFileName() + "] is missing " + p.getFileName());
    }
  }

  /**
   * Maps one {@link SuiteRequest.Operation} onto a {@link WorkbookEngine} call. Mirrors the
   * argument unwrapping each tool's {@code execute(JsonNode)} does, minus the handle lookup — the
   * runner owns the handle.
   *
   * <p>Read-only tools (range.get, workbook.metadata, …) are rejected: operations describe
   * *changes*, so including a read would suggest a confused suite author. The VBA read tools are
   * rejected for the same reason.
   */
  private static void dispatch(WorkbookEngine engine, HandleId id, SuiteRequest.Operation op)
      throws Exception {
    if (op.tool() == null) {
      throw new IllegalArgumentException("operation is missing 'tool'");
    }
    JsonNode args = op.arguments();
    switch (op.tool()) {
      case "range.set" -> {
        String sheet = requireText(args, "sheet");
        int[] startRC = startRowCol(args, sheet);
        List<List<Object>> values = parseValues2D(args.get("values"));
        List<List<String>> formulas = parseFormulas2D(args.get("formulas"));
        engine.writeRange(id, sheet, startRC[0], startRC[1], values, formulas);
      }
      case "range.clear" -> {
        String sheet = requireText(args, "sheet");
        String rangeStr = requireText(args, "range");
        engine.clearRange(id, sheet, rangeStr);
      }
      case "sheet.create" -> {
        String name = requireText(args, "name");
        OptionalInt idx =
            args.hasNonNull("index")
                ? OptionalInt.of(args.get("index").asInt())
                : OptionalInt.empty();
        engine.createSheet(id, name, idx);
      }
      case "sheet.delete" -> engine.deleteSheet(id, requireText(args, "name"));
      case "sheet.rename" ->
          engine.renameSheet(id, requireText(args, "old_name"), requireText(args, "new_name"));
      case "sheet.insert_rows" ->
          engine.insertRows(
              id,
              requireText(args, "sheet"),
              requireInt(args, "start_row"),
              optionalInt(args, "count", 1));
      case "sheet.delete_rows" ->
          engine.deleteRows(
              id,
              requireText(args, "sheet"),
              requireInt(args, "start_row"),
              optionalInt(args, "count", 1));
      case "sheet.insert_cols" ->
          engine.insertCols(
              id,
              requireText(args, "sheet"),
              requireInt(args, "start_col"),
              optionalInt(args, "count", 1));
      case "sheet.delete_cols" ->
          engine.deleteCols(
              id,
              requireText(args, "sheet"),
              requireInt(args, "start_col"),
              optionalInt(args, "count", 1));
      case "workbook.recalculate" -> engine.recalculate(id);

      case "workbook.open", "workbook.create", "workbook.save", "workbook.close" ->
          throw new IllegalArgumentException(
              op.tool() + " is managed by the runner and must not appear in operations");

      case "range.get",
              "workbook.list_sheets",
              "workbook.metadata",
              "workbook.capabilities_report",
              "workbook.list_handles",
              "sheet.merged_regions",
              "table.list",
              "table.get",
              "named_range.list",
              "named_range.get",
              "vba.list_modules",
              "vba.get_module" ->
          throw new IllegalArgumentException(
              op.tool() + " is read-only and must not appear in operations");

      default -> throw new IllegalArgumentException("unknown tool: " + op.tool());
    }
  }

  private static int[] startRowCol(JsonNode args, String sheet) throws Exception {
    String raw;
    if (args.hasNonNull("start")) {
      raw = args.get("start").asText();
    } else if (args.hasNonNull("range")) {
      raw = args.get("range").asText();
    } else {
      throw new IllegalArgumentException("range.set requires 'start' or 'range'");
    }
    RangeAddress.ParsedRange parsed = RangeAddress.parseWithSheet(raw);
    if (parsed.sheet().isPresent() && !parsed.sheet().get().equalsIgnoreCase(sheet)) {
      throw new IllegalArgumentException(
          "sheet prefix '"
              + parsed.sheet().get()
              + "' in '"
              + raw
              + "' disagrees with sheet '"
              + sheet
              + "'");
    }
    return new int[] {parsed.address().startRow(), parsed.address().startCol()};
  }

  private static List<List<Object>> parseValues2D(JsonNode node) {
    if (node == null || !node.isArray()) {
      throw new IllegalArgumentException("'values' must be a 2D array");
    }
    List<List<Object>> rows = new ArrayList<>(node.size());
    for (JsonNode rowNode : node) {
      if (!rowNode.isArray()) {
        throw new IllegalArgumentException("each row in 'values' must be an array");
      }
      List<Object> row = new ArrayList<>(rowNode.size());
      for (JsonNode v : rowNode) row.add(jsonToJava(v));
      rows.add(row);
    }
    return rows;
  }

  private static List<List<String>> parseFormulas2D(JsonNode node) {
    if (node == null || node.isNull() || !node.isArray() || node.isEmpty()) return null;
    List<List<String>> rows = new ArrayList<>(node.size());
    for (JsonNode rowNode : node) {
      if (!rowNode.isArray()) {
        throw new IllegalArgumentException("each row in 'formulas' must be an array");
      }
      List<String> row = new ArrayList<>(rowNode.size());
      for (JsonNode v : rowNode) row.add(v == null || v.isNull() ? null : v.textValue());
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

  private static String requireText(JsonNode args, String name) {
    if (args == null || !args.hasNonNull(name) || !args.get(name).isTextual()) {
      throw new IllegalArgumentException("missing or non-string argument: " + name);
    }
    return args.get(name).asText();
  }

  private static int requireInt(JsonNode args, String name) {
    if (args == null || !args.hasNonNull(name) || !args.get(name).canConvertToInt()) {
      throw new IllegalArgumentException("missing or non-integer argument: " + name);
    }
    return args.get(name).asInt();
  }

  private static int optionalInt(JsonNode args, String name, int fallback) {
    if (args == null || !args.hasNonNull(name)) return fallback;
    if (!args.get(name).canConvertToInt()) {
      throw new IllegalArgumentException("non-integer argument: " + name);
    }
    return args.get(name).asInt();
  }
}
