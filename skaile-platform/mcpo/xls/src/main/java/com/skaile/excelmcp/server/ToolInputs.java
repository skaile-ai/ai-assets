package com.skaile.excelmcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import java.util.Map;
import java.util.Optional;

/** Tiny shared helpers for tool input parsing and schema construction. */
public final class ToolInputs {

  private ToolInputs() {}

  public static String requireString(JsonNode node, String field) throws McpException {
    JsonNode v = node.get(field);
    if (v == null || v.isNull() || !v.isTextual()) {
      throw new McpException(
          ErrorCode.PATH_INVALID,
          "required string field missing: " + field,
          Map.of("field", field));
    }
    return v.textValue();
  }

  public static Optional<String> optionalString(JsonNode node, String field) {
    JsonNode v = node == null ? null : node.get(field);
    if (v == null || v.isNull() || !v.isTextual()) {
      return Optional.empty();
    }
    return Optional.of(v.textValue());
  }

  public static boolean boolOrDefault(JsonNode node, String field, boolean dflt) {
    JsonNode v = node == null ? null : node.get(field);
    if (v == null || v.isNull() || !v.isBoolean()) {
      return dflt;
    }
    return v.booleanValue();
  }

  public static int intOrDefault(JsonNode node, String field, int dflt) {
    JsonNode v = node == null ? null : node.get(field);
    if (v == null || v.isNull() || !v.canConvertToInt()) {
      return dflt;
    }
    return v.intValue();
  }

  public static HandleId requireHandle(JsonNode input) throws McpException {
    String raw = requireString(input, "handle");
    try {
      return new HandleId(raw);
    } catch (IllegalArgumentException ex) {
      throw new McpException(
          ErrorCode.HANDLE_UNKNOWN, "malformed handle: " + raw, Map.of("handle", raw));
    }
  }

  public static ObjectNode object() {
    return JsonNodeFactory.instance.objectNode();
  }

  public static ObjectNode stringProp(String description) {
    ObjectNode n = object();
    n.put("type", "string");
    n.put("description", description);
    return n;
  }

  public static ObjectNode boolProp(String description, boolean defaultValue) {
    ObjectNode n = object();
    n.put("type", "boolean");
    n.put("description", description);
    n.put("default", defaultValue);
    return n;
  }

  public static ObjectNode intProp(String description, int defaultValue) {
    ObjectNode n = object();
    n.put("type", "integer");
    n.put("description", description);
    n.put("default", defaultValue);
    return n;
  }
}
