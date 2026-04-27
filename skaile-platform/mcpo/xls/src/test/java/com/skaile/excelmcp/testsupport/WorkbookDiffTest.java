package com.skaile.excelmcp.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Mirrors xlport's {@code UtilsTest.testDiffTwoWorkbooksAndReturnFirstError}
 * (xlport-internal/src/test/java/com/molnify/xlport/core/UtilsTest.java:78) using an in-memory
 * XSSFWorkbook instead of the xlport-specific {@code TemplateManager.getTemplate("dummy")}.
 */
class WorkbookDiffTest {

  @Test
  void identicalWorkbooksReturnNoErrors() throws Exception {
    try (Workbook a = buildDummy();
        Workbook b = buildDummy()) {
      assertThat(WorkbookDiff.diff(a, a)).isEmpty();
      assertThat(WorkbookDiff.diff(a, b)).isEmpty();
    }
  }

  @Test
  void valueMismatchIsReportedWithSheetAndA1() throws Exception {
    try (Workbook expected = buildDummy();
        Workbook actual = buildDummy()) {
      actual.getSheetAt(1).getRow(0).getCell(1).setCellValue("new!");
      List<String> errors = WorkbookDiff.diff(expected, actual);
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0)).contains("Sheet2").contains("B1").contains("new!");
    }
  }

  @Test
  void revertingMismatchClearsError() throws Exception {
    try (Workbook expected = buildDummy();
        Workbook actual = buildDummy()) {
      String original = expected.getSheetAt(1).getRow(0).getCell(1).getStringCellValue();
      actual.getSheetAt(1).getRow(0).getCell(1).setCellValue("new!");
      assertThat(WorkbookDiff.diff(expected, actual)).isNotEmpty();
      actual.getSheetAt(1).getRow(0).getCell(1).setCellValue(original);
      assertThat(WorkbookDiff.diff(expected, actual)).isEmpty();
    }
  }

  @Test
  void sheetCountMismatchIsReported() throws Exception {
    try (Workbook expected = buildDummy();
        Workbook actual = buildDummy()) {
      actual.createSheet("extra");
      assertThat(WorkbookDiff.diff(expected, actual)).isNotEmpty();
      actual.removeSheetAt(actual.getNumberOfSheets() - 1);
      assertThat(WorkbookDiff.diff(expected, actual)).isEmpty();
    }
  }

  @Test
  void missingRowInActualIsReportedPerExpectedCell() throws Exception {
    try (Workbook expected = buildDummy();
        Workbook actual = buildDummy()) {
      actual.getSheetAt(0).removeRow(actual.getSheetAt(0).getRow(1));
      List<String> errors = WorkbookDiff.diff(expected, actual);
      assertThat(errors)
          .allMatch(e -> e.contains("Row [2] exists in expected but not in actual sheet [Sheet1]"))
          .hasSize(2);
    }
  }

  @Test
  void expectedHavingStrictlyMoreLastRowThanActualIsReported() throws Exception {
    try (Workbook expected = buildDummy();
        Workbook actual = buildDummy()) {
      expected.getSheetAt(0).createRow(10).createCell(0).setCellValue("extra");
      List<String> errors = WorkbookDiff.diff(expected, actual);
      assertThat(errors).anyMatch(e -> e.contains("Number of rows"));
    }
  }

  private static Workbook buildDummy() {
    XSSFWorkbook wb = new XSSFWorkbook();
    for (int s = 1; s <= 3; s++) {
      Sheet sheet = wb.createSheet("Sheet" + s);
      for (int r = 0; r < 4; r++) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue("Sheet" + s + "-r" + r + "-c0");
        row.createCell(1).setCellValue("Sheet" + s + "-r" + r + "-c1");
      }
    }
    return wb;
  }
}
