package com.skaile.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Wire shape for {@code workbook.capabilities_report} (§9.1). Tells the agent which Excel features
 * the POI engine can and cannot recalculate for the loaded workbook — so editing decisions can be
 * made before values go stale.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "poi_version",
  "supported_function_count",
  "functions_used",
  "unsupported_functions_used",
  "has_linked_data_types",
  "has_dynamic_array_formulas",
  "has_legacy_array_formulas",
  "has_vba",
  "warnings"
})
public record CapabilitiesReportShape(
    String poiVersion,
    int supportedFunctionCount,
    List<String> functionsUsed,
    List<UnsupportedFunctionUse> unsupportedFunctionsUsed,
    boolean hasLinkedDataTypes,
    boolean hasDynamicArrayFormulas,
    boolean hasLegacyArrayFormulas,
    boolean hasVba,
    List<String> warnings) {

  @JsonPropertyOrder({"name", "count", "sample_cells"})
  public record UnsupportedFunctionUse(String name, int count, List<String> sampleCells) {}
}
