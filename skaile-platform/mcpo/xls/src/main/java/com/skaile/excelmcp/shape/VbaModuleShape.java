package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Directory entry for a single VBA module (§7.3). {@code type} is one of {@code module | class |
 * document}. UserForms are known-incomplete in every Java/Python extractor and are not surfaced.
 */
@JsonPropertyOrder({"name", "type"})
public record VbaModuleShape(String name, String type) {}
