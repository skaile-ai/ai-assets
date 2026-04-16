package com.portfolex.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Lightweight descriptor used in {@code workbook.metadata} and {@code named_range.list}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "sheet", "range", "scope"})
public record NamedRangeRef(String name, String sheet, String range, String scope) {}
