package com.skaile.excelmcp.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Checked exception carrying an {@link ErrorCode} and a structured details map. Tools throw this;
 * the dispatch layer translates it into the MCP error envelope (§8).
 */
public final class McpException extends Exception {

  private final ErrorCode code;
  private final Map<String, Object> details;

  public McpException(ErrorCode code, String message) {
    this(code, message, Map.of(), null);
  }

  public McpException(ErrorCode code, String message, Map<String, Object> details) {
    this(code, message, details, null);
  }

  public McpException(
      ErrorCode code, String message, Map<String, Object> details, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.details = Map.copyOf(details);
  }

  public ErrorCode code() {
    return code;
  }

  public Map<String, Object> details() {
    return details;
  }

  public Map<String, Object> detailsWith(String key, Object value) {
    Map<String, Object> merged = new HashMap<>(details);
    merged.put(key, value);
    return Map.copyOf(merged);
  }
}
