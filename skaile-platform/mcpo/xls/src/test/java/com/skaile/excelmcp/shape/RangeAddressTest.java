package com.skaile.excelmcp.shape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import org.apache.poi.ss.SpreadsheetVersion;
import org.junit.jupiter.api.Test;

/**
 * Parser coverage for the new sheet-prefix and full-row / full-column forms (review fix B1).
 * Existing point-of-truth tests for cell / range parsing live alongside the engine reads in {@link
 * com.skaile.excelmcp.engine.RangeBoundsTest}.
 */
class RangeAddressTest {

  @Test
  void fullColumnReturnsExcel2007RowSentinel() throws Exception {
    RangeAddress a = RangeAddress.parse("A:A");
    assertThat(a.startRow()).isZero();
    assertThat(a.startCol()).isZero();
    assertThat(a.endCol()).isZero();
    assertThat(a.endRow()).isEqualTo(SpreadsheetVersion.EXCEL2007.getLastRowIndex());
  }

  @Test
  void fullColumnRangeAcceptsMultipleColumns() throws Exception {
    RangeAddress a = RangeAddress.parse("A:C");
    assertThat(a.startCol()).isZero();
    assertThat(a.endCol()).isEqualTo(2);
    assertThat(a.endRow()).isEqualTo(SpreadsheetVersion.EXCEL2007.getLastRowIndex());
  }

  @Test
  void fullRowReturnsExcel2007ColumnSentinel() throws Exception {
    RangeAddress a = RangeAddress.parse("1:5");
    assertThat(a.startRow()).isZero();
    assertThat(a.endRow()).isEqualTo(4);
    assertThat(a.startCol()).isZero();
    assertThat(a.endCol()).isEqualTo(SpreadsheetVersion.EXCEL2007.getLastColumnIndex());
  }

  @Test
  void sheetPrefixUnquoted() throws Exception {
    RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet("Sheet1!A1:B2");
    assertThat(pr.sheet()).contains("Sheet1");
    assertThat(pr.address().toA1()).isEqualTo("A1:B2");
  }

  @Test
  void sheetPrefixQuotedWithSpaces() throws Exception {
    RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet("'Sheet Name with Spaces'!A1:B2");
    assertThat(pr.sheet()).contains("Sheet Name with Spaces");
    assertThat(pr.address().toA1()).isEqualTo("A1:B2");
  }

  @Test
  void sheetPrefixQuotedWithEmbeddedApostrophe() throws Exception {
    RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet("'Bob''s sheet'!C5");
    assertThat(pr.sheet()).contains("Bob's sheet");
    assertThat(pr.address().toA1()).isEqualTo("C5");
  }

  @Test
  void sheetPrefixWithFullColumnInner() throws Exception {
    RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet("Sheet1!A:A");
    assertThat(pr.sheet()).contains("Sheet1");
    assertThat(pr.address().endRow()).isEqualTo(SpreadsheetVersion.EXCEL2007.getLastRowIndex());
  }

  @Test
  void threeDimensionalReferenceIsRejected() {
    assertThatThrownBy(() -> RangeAddress.parseWithSheet("Sheet1:Sheet3!A1"))
        .isInstanceOf(McpException.class)
        .satisfies(
            ex -> {
              McpException mx = (McpException) ex;
              assertThat(mx.code()).isEqualTo(ErrorCode.RANGE_INVALID);
              assertThat(mx.getMessage()).contains("3D references");
            });
  }

  @Test
  void noPrefixReturnsEmptyOptional() throws Exception {
    RangeAddress.ParsedRange pr = RangeAddress.parseWithSheet("A1:B2");
    assertThat(pr.sheet()).isEmpty();
    assertThat(pr.address().toA1()).isEqualTo("A1:B2");
  }
}
