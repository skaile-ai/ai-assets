package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;

/** Wire shape for {@code workbook.metadata} (§7.3). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "filename",
  "size_bytes",
  "modified",
  "format",
  "sheets",
  "named_ranges",
  "tables"
})
public record WorkbookMetadataShape(
    String filename,
    Long sizeBytes,
    Instant modified,
    String format,
    List<SheetShape> sheets,
    List<NamedRangeRef> namedRanges,
    List<TableRef> tables) {}
