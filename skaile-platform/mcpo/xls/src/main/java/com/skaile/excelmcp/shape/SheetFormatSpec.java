package com.skaile.excelmcp.shape;

import java.util.List;

/**
 * POI-free description of sheet-level presentation (the {@code sheet.set_format} tool): column
 * widths, row heights, a frozen header pane, and the worksheet tab color. Every group is
 * independently optional; a null/empty group is left unchanged.
 */
public record SheetFormatSpec(
    List<ColumnWidth> columnWidths,
    List<RowHeight> rowHeights,
    FreezePane freeze,
    String tabColor) {

  /** Width for one column, addressed by letter ("A", "AB"). Width is in Excel character units. */
  public record ColumnWidth(String column, double width) {}

  /** Height for one 1-based row. Height is in points (the unit Excel shows in the UI). */
  public record RowHeight(int row, double height) {}

  /**
   * Frozen leading {@code rows} and {@code cols} (a classic header freeze is {@code rows=1,
   * cols=0}). Both 0 removes any existing freeze.
   */
  public record FreezePane(int rows, int cols) {}

  public boolean isEmpty() {
    return (columnWidths == null || columnWidths.isEmpty())
        && (rowHeights == null || rowHeights.isEmpty())
        && freeze == null
        && tabColor == null;
  }
}
