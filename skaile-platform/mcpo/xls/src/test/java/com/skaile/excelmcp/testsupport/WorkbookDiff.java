package com.skaile.excelmcp.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.opentest4j.AssertionFailedError;

/**
 * Port of xlport's {@code Utils.diffTwoWorkbooksAndReturnErrors} (xlport-internal
 * src/main/java/com/molnify/xlport/core/Utils.java:320) for excel-mcp's deterministic test-suite
 * regression harness.
 *
 * <p>Scope matches xlport: sheet count + names, one-sided row-count check, per-cell rendered string
 * compare via {@code DataFormatter(Locale.GERMAN)}, and table-by-name presence + row count. Cell
 * styles, merged regions, named ranges, conditional formats, etc. are deliberately out of scope —
 * suites that need deeper checks attach a {@link SuiteExtension} (see {@code TestSuiteRunner}).
 */
public final class WorkbookDiff {

  private static final DataFormatter FORMATTER = new DataFormatter(Locale.GERMAN);

  private WorkbookDiff() {}

  /** Returns an empty list when expected and actual match; one error string per mismatch. */
  public static List<String> diff(Workbook expected, Workbook actual) {
    List<String> errors = new ArrayList<>();
    if (expected.getNumberOfSheets() != actual.getNumberOfSheets()) {
      errors.add(
          "Number of sheets differ, expected ["
              + expected.getNumberOfSheets()
              + "] does not match actual ["
              + actual.getNumberOfSheets()
              + "]");
    }
    int sheetsToCompare = Math.min(expected.getNumberOfSheets(), actual.getNumberOfSheets());
    for (int i = 0; i < sheetsToCompare; i++) {
      XSSFSheet es = (XSSFSheet) expected.getSheetAt(i);
      XSSFSheet as = (XSSFSheet) actual.getSheetAt(i);
      if (!es.getSheetName().equals(as.getSheetName())) {
        errors.add(
            "Sheet name expected ["
                + es.getSheetName()
                + "] does not match actual ["
                + as.getSheetName()
                + "]");
      }
      if (es.getLastRowNum() > as.getLastRowNum()) {
        errors.add(
            "Number of rows in sheet ["
                + es.getSheetName()
                + "] differ, expected ["
                + es.getLastRowNum()
                + "] does not match actual ["
                + as.getLastRowNum()
                + "]");
      }
      for (int r = 0; r <= es.getLastRowNum(); r++) {
        Row er = es.getRow(r);
        Row ar = as.getRow(r);
        if (er == null) continue;
        for (Cell ec : er) {
          if (ar == null) {
            errors.add(
                "Row ["
                    + (r + 1)
                    + "] exists in expected but not in actual sheet ["
                    + es.getSheetName()
                    + "]");
            continue;
          }
          Cell ac = ar.getCell(ec.getColumnIndex());
          String eValue = getCellValueAsString(ec);
          String aValue = getCellValueAsString(ac);
          if (!eValue.equals(aValue)) {
            errors.add(
                "Value in sheet ["
                    + es.getSheetName()
                    + "] for cell ["
                    + ec.getAddress()
                    + "] differ, expected ["
                    + eValue
                    + "] does not match actual ["
                    + aValue
                    + "]");
          }
        }
      }

      for (XSSFTable et : es.getTables()) {
        boolean match = false;
        for (XSSFTable at : as.getTables()) {
          if (et.getName() != null && et.getName().equals(at.getName())) {
            match = true;
            if (et.getRowCount() != at.getRowCount()) {
              errors.add(
                  "Sheet ["
                      + es.getSheetName()
                      + "] has a table ["
                      + et.getName()
                      + "] with row count ["
                      + et.getRowCount()
                      + "] but output has row count ["
                      + at.getRowCount()
                      + "]");
            }
          }
        }
        if (!match) {
          errors.add(
              "Sheet ["
                  + es.getSheetName()
                  + "] has a table ["
                  + et.getName()
                  + "] that was not found in the output");
        }
      }
    }
    return errors;
  }

  /** Fails the current JUnit test with the joined error list if {@link #diff} is non-empty. */
  public static void assertMatches(Workbook expected, Workbook actual) {
    List<String> errors = diff(expected, actual);
    if (!errors.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (String e : errors) sb.append("\n").append(e);
      throw new AssertionFailedError("Workbooks differ:" + sb);
    }
  }

  /**
   * Renders a cell as the string xlport compares on: {@code DataFormatter(Locale.GERMAN)} for value
   * cells, and for formula cells the cached-result branch matching {@code
   * Utils.getCellValueAsString}. Null or blank → empty string.
   */
  static String getCellValueAsString(Cell cell) {
    if (cell == null) return "";
    if (cell.getCellType() == CellType.FORMULA) {
      return switch (cell.getCachedFormulaResultType()) {
        case STRING -> cell.getStringCellValue();
        case NUMERIC -> {
          String formatString = cell.getCellStyle().getDataFormatString();
          int formatIndex = cell.getCellStyle().getDataFormat();
          yield FORMATTER.formatRawCellContents(
              cell.getNumericCellValue(), formatIndex, formatString);
        }
        case BLANK, _NONE -> "";
        case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
        case ERROR -> FormulaError.forInt(cell.getErrorCellValue()).getString();
        default ->
            throw new IllegalStateException(
                "Cell ["
                    + cell.getAddress()
                    + "] has unexpected cached formula result type ["
                    + cell.getCachedFormulaResultType()
                    + "]");
      };
    }
    return FORMATTER.formatCellValue(cell);
  }
}
