package com.skaile.excelmcp.testsupport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * On-disk shape of a test-suite {@code request.json}: an ordered list of tool invocations to replay
 * against the suite's template. {@code handle} is injected by the runner — suites never spell it
 * out. {@code workbook.open}/{@code workbook.save}/{@code workbook.close} are implicit. See §5 of
 * {@code deterministic-test-suites-plan.md}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SuiteRequest(String description, List<Operation> operations) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Operation(String tool, JsonNode arguments) {}
}
