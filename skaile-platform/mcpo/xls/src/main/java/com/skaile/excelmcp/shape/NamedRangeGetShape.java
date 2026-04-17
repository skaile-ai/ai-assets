package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * {@code named_range.get} wire shape (§9.6). A {@link RangeShape} augmented with the resolved
 * {@code named_range}. Fields mirror {@link RangeShape} verbatim so the JSON stays flat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "named_range",
  "sheet",
  "range",
  "rows",
  "cols",
  "cells",
  "truncated",
  "total_cells"
})
public record NamedRangeGetShape(
    String namedRange,
    String sheet,
    String range,
    int rows,
    int cols,
    List<List<CellShape>> cells,
    Boolean truncated,
    Integer totalCells) {

  public static NamedRangeGetShape of(String namedRange, RangeShape rs) {
    return new NamedRangeGetShape(
        namedRange,
        rs.sheet(),
        rs.range(),
        rs.rows(),
        rs.cols(),
        rs.cells(),
        rs.truncated(),
        rs.totalCells());
  }
}
