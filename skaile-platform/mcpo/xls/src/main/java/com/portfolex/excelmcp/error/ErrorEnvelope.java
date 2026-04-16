package com.portfolex.excelmcp.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;

/** Wire-shape of an error response (§8.1). Serialised into the MCP error channel. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "message", "details"})
public record ErrorEnvelope(String code, String message, Map<String, Object> details) {

  public static ErrorEnvelope of(McpException ex) {
    return new ErrorEnvelope(ex.code().name(), ex.getMessage(), ex.details());
  }
}
