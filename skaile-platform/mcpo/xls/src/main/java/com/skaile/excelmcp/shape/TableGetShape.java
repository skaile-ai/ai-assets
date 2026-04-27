package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * {@code table.get} wire shape (§9.5). A {@link RangeShape} augmented with the resolved {@code
 * table_name}. Fields mirror {@link RangeShape} verbatim so the JSON stays flat — agents consume
 * this exactly like a range read that happens to know its source table.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "table_name",
  "sheet",
  "range",
  "rows",
  "cols",
  "cells",
  "truncated",
  "total_cells"
})
public record TableGetShape(
    String tableName,
    String sheet,
    String range,
    int rows,
    int cols,
    List<List<CellShape>> cells,
    Boolean truncated,
    Integer totalCells) {

  public static TableGetShape of(String tableName, RangeShape rs) {
    return new TableGetShape(
        tableName,
        rs.sheet(),
        rs.range(),
        rs.rows(),
        rs.cols(),
        rs.cells(),
        rs.truncated(),
        rs.totalCells());
  }
}
