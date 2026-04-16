package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Single-sheet summary (§7.3). Used by {@code workbook.list_sheets} and {@code workbook.metadata}.
 */
@JsonPropertyOrder({"name", "index", "is_hidden"})
public record SheetShape(String name, int index, boolean isHidden) {}
