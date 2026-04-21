package com.skaile.excelmcp.engine.poi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Coverage for the file-size and cell-count guards used at workbook.open. */
class PoiSizeGuardTest {

  @Test
  void fileSizeAtLimitPasses(@TempDir Path dir) throws Exception {
    Path f = writeWorkbook(dir.resolve("ok.xlsx"), 1);
    long size = Files.size(f);
    PoiSizeGuard.assertFileSize(f, size);
  }

  @Test
  void fileSizeUnderLimitPasses(@TempDir Path dir) throws Exception {
    Path f = writeWorkbook(dir.resolve("ok.xlsx"), 1);
    long size = Files.size(f);
    PoiSizeGuard.assertFileSize(f, size + 1);
  }

  @Test
  void fileSizeOneOverLimitFailsWithWorkbookTooLarge(@TempDir Path dir) throws Exception {
    Path f = writeWorkbook(dir.resolve("big.xlsx"), 1);
    long size = Files.size(f);
    assertThatThrownBy(() -> PoiSizeGuard.assertFileSize(f, size - 1))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.WORKBOOK_TOO_LARGE);
  }

  @Test
  void cellCountAtLimitPasses() throws Exception {
    try (XSSFWorkbook wb = workbookWithCells(10)) {
      PoiSizeGuard.assertCellCount(wb, 10);
    }
  }

  @Test
  void cellCountOneOverLimitFailsWithWorkbookTooManyCells() throws Exception {
    try (XSSFWorkbook wb = workbookWithCells(11)) {
      assertThatThrownBy(() -> PoiSizeGuard.assertCellCount(wb, 10))
          .isInstanceOf(McpException.class)
          .extracting(ex -> ((McpException) ex).code())
          .isEqualTo(ErrorCode.WORKBOOK_TOO_MANY_CELLS);
    }
  }

  @Test
  void countPopulatedCellsCountsOnlyPhysicalCells() throws Exception {
    try (XSSFWorkbook wb = workbookWithCells(7)) {
      assertThat(PoiSizeGuard.countPopulatedCells(wb)).isEqualTo(7);
    }
  }

  private static XSSFWorkbook workbookWithCells(int n) {
    XSSFWorkbook wb = new XSSFWorkbook();
    Sheet s = wb.createSheet("Sheet1");
    var row = s.createRow(0);
    for (int i = 0; i < n; i++) {
      row.createCell(i).setCellValue(i);
    }
    return wb;
  }

  private static Path writeWorkbook(Path dest, int cells) throws Exception {
    try (XSSFWorkbook wb = workbookWithCells(cells);
        OutputStream os = Files.newOutputStream(dest)) {
      wb.write(os);
    }
    return dest;
  }
}
