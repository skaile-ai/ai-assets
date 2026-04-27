package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Rectangular range wire shape (§7.2). {@code cells} is row-major and always rectangular. {@code
 * truncated} / {@code total_cells} are populated only when the requested range exceeded the caller
 * cap; otherwise they are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"sheet", "range", "rows", "cols", "cells", "truncated", "total_cells"})
public record RangeShape(
    String sheet,
    String range,
    int rows,
    int cols,
    List<List<CellShape>> cells,
    Boolean truncated,
    Integer totalCells) {

  public static RangeShape of(String sheet, String range, List<List<CellShape>> cells) {
    int r = cells.size();
    int c = cells.isEmpty() ? 0 : cells.get(0).size();
    return new RangeShape(sheet, range, r, c, cells, null, null);
  }
}
