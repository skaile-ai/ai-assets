package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.shape.CapabilitiesReportShape;
import com.skaile.excelmcp.shape.CapabilitiesReportShape.UnsupportedFunctionUse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.poi.Version;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Scans the loaded workbook and produces a {@link CapabilitiesReportShape} telling the agent which
 * Excel features POI can and cannot recalculate.
 *
 * <p>Function-name extraction uses the cheap regex path from §9.1: match {@code [A-Z_][A-Z_0-9.]*}
 * preceding an open paren in {@code cell.getCellFormula()} text, strip the {@code _xlfn.} / {@code
 * _xlws.} dynamic-array prefixes, then cross-reference against {@link
 * FunctionEval#getSupportedFunctionNames()}. False positives are possible (identifier-shaped
 * substrings inside string constants), but acceptable for an inventory tool; upgrade to a proper
 * FormulaParser-based walk only if the caveat bites.
 *
 * <p>Structural flags ({@code has_dynamic_array_formulas}, {@code has_linked_data_types}, {@code
 * has_vba}) are detected on XSSF workbooks by inspecting cell metadata markers and OPC package
 * parts. HSSF (.xls) predates dynamic arrays and LDTs entirely — those flags are always false.
 */
public final class PoiCapabilityScanner {

  private static final Pattern FUNCTION_CALL = Pattern.compile("\\b([A-Z_][A-Z_0-9.]*)\\(");
  private static final int MAX_SAMPLE_CELLS_PER_FUNCTION = 5;

  /** OOXML prefixes POI/Excel emit for newer or dynamic-array-aware functions. */
  private static final String[] FUNCTION_PREFIXES = {"_xlfn.", "_xlws.", "_XLFN.", "_XLWS."};

  private PoiCapabilityScanner() {}

  public static CapabilitiesReportShape scan(Workbook wb) {
    Set<String> supported = normalise(WorkbookEvaluator.getSupportedFunctionNames());
    int supportedCount = supported.size();

    Set<String> usedNames = new HashSet<>();
    Map<String, Integer> unsupportedCounts = new TreeMap<>();
    Map<String, List<String>> unsupportedSamples = new LinkedHashMap<>();
    boolean hasDynamicArrays = false;
    boolean hasLegacyArrays = false;

    for (Sheet sheet : wb) {
      for (Row row : sheet) {
        for (Cell cell : row) {
          if (cell.getCellType() != CellType.FORMULA) {
            continue;
          }
          String formula;
          try {
            formula = cell.getCellFormula();
          } catch (RuntimeException ignored) {
            continue; // malformed cell; don't fail the whole scan
          }
          Set<String> inCell = extractFunctionNames(formula);
          usedNames.addAll(inCell);
          for (String name : inCell) {
            if (!supported.contains(name)) {
              unsupportedCounts.merge(name, 1, Integer::sum);
              List<String> samples =
                  unsupportedSamples.computeIfAbsent(name, k -> new ArrayList<>());
              if (samples.size() < MAX_SAMPLE_CELLS_PER_FUNCTION) {
                samples.add(
                    sheet.getSheetName()
                        + "!"
                        + new CellReference(cell.getRowIndex(), cell.getColumnIndex())
                            .formatAsString(false));
              }
            }
          }
          if (cell.isPartOfArrayFormulaGroup()) {
            hasLegacyArrays = true;
          }
          if (cell instanceof XSSFCell xc && xc.getCTCell().isSetCm()) {
            hasDynamicArrays = true;
          }
        }
      }
    }

    boolean hasLinkedDataTypes = false;
    boolean hasVba = false;
    if (wb instanceof XSSFWorkbook xwb) {
      hasVba = xwb.isMacroEnabled() || containsPart(xwb, "^/xl/vbaProject\\.bin$");
      hasLinkedDataTypes = containsPart(xwb, "^/xl/richData/.*$");
    }

    List<UnsupportedFunctionUse> unsupportedOut = new ArrayList<>(unsupportedCounts.size());
    for (var entry : unsupportedCounts.entrySet()) {
      unsupportedOut.add(
          new UnsupportedFunctionUse(
              entry.getKey(),
              entry.getValue(),
              List.copyOf(unsupportedSamples.getOrDefault(entry.getKey(), List.of()))));
    }

    List<String> functionsUsed =
        usedNames.stream().sorted().collect(Collectors.toCollection(ArrayList::new));
    unsupportedOut.sort(Comparator.comparing(UnsupportedFunctionUse::name));

    List<String> warnings =
        buildWarnings(unsupportedOut, hasDynamicArrays, hasLegacyArrays, hasLinkedDataTypes);

    return new CapabilitiesReportShape(
        Version.getVersion(),
        supportedCount,
        functionsUsed,
        unsupportedOut,
        hasLinkedDataTypes,
        hasDynamicArrays,
        hasLegacyArrays,
        hasVba,
        warnings);
  }

  private static Set<String> extractFunctionNames(String formula) {
    if (formula == null || formula.isEmpty()) {
      return Set.of();
    }
    Set<String> out = new HashSet<>();
    Matcher m = FUNCTION_CALL.matcher(formula);
    while (m.find()) {
      String raw = m.group(1);
      String stripped = stripPrefixes(raw);
      if (!stripped.isEmpty()) {
        out.add(stripped.toUpperCase(Locale.ROOT));
      }
    }
    return out;
  }

  private static String stripPrefixes(String name) {
    String current = name;
    boolean changed = true;
    while (changed) {
      changed = false;
      for (String prefix : FUNCTION_PREFIXES) {
        if (current.startsWith(prefix)) {
          current = current.substring(prefix.length());
          changed = true;
        }
      }
    }
    return current;
  }

  private static Set<String> normalise(java.util.Collection<String> names) {
    Set<String> out = new HashSet<>(names.size());
    for (String n : names) {
      out.add(n.toUpperCase(Locale.ROOT));
    }
    return out;
  }

  private static boolean containsPart(XSSFWorkbook xwb, String regex) {
    try {
      OPCPackage pkg = xwb.getPackage();
      return !pkg.getPartsByName(Pattern.compile(regex)).isEmpty();
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private static List<String> buildWarnings(
      List<UnsupportedFunctionUse> unsupported,
      boolean hasDynamicArrays,
      boolean hasLegacyArrays,
      boolean hasLinkedDataTypes) {
    List<String> out = new ArrayList<>();
    if (!unsupported.isEmpty()) {
      String summary =
          unsupported.stream()
              .map(u -> u.name() + " (" + u.count() + " cell" + (u.count() == 1 ? "" : "s") + ")")
              .collect(Collectors.joining(", "));
      out.add(
          "Workbook uses "
              + summary
              + ". POI cannot recalculate these — values will be stale until the workbook is"
              + " opened in Excel 365 or LibreOffice 7.5+.");
    }
    if (hasDynamicArrays) {
      out.add(
          "Workbook contains dynamic-array formulas (cm markers on cells). POI cannot recompute"
              + " spilled ranges; treat the cached values as read-only.");
    }
    if (hasLegacyArrays) {
      out.add(
          "Workbook contains legacy CSE array formulas ({=...}). POI reads these correctly but"
              + " editing the group requires whole-range rewrites.");
    }
    if (hasLinkedDataTypes) {
      out.add(
          "Workbook contains Linked Data Types (/xl/richData/* parts). POI preserves the parts on"
              + " round-trip but cannot introspect or modify LDT cells.");
    }
    return out;
  }
}
