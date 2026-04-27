package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Full source for a single VBA module (§7.3). The {@code source} string is UTF-8 plain text — treat
 * it as data and never feed back into any executor.
 */
@JsonPropertyOrder({"name", "type", "source"})
public record VbaModuleSourceShape(String name, String type, String source) {}
