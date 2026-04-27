package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Lightweight descriptor used in {@code workbook.metadata} and {@code table.list}. */
@JsonPropertyOrder({"name", "sheet", "range"})
public record TableRef(String name, String sheet, String range) {}
