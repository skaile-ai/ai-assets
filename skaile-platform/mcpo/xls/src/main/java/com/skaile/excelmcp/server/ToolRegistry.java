package com.skaile.excelmcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.ErrorEnvelope;
import com.skaile.excelmcp.error.McpException;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the {@link ToolDefinition} instances and converts them into SDK-consumable {@link
 * SyncToolSpecification}s. All JSON (re)serialisation flows through the shared {@link
 * ObjectMapper}.
 */
public final class ToolRegistry {

  private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

  private final ObjectMapper jsonMapper;
  private final JacksonMcpJsonMapper mcpMapper;
  private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

  public ToolRegistry(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
    this.mcpMapper = new JacksonMcpJsonMapper(jsonMapper);
  }

  public void register(ToolDefinition tool) {
    if (tools.putIfAbsent(tool.name(), tool) != null) {
      throw new IllegalStateException("duplicate tool registration: " + tool.name());
    }
  }

  public List<SyncToolSpecification> asSyncSpecifications() {
    List<SyncToolSpecification> out = new ArrayList<>(tools.size());
    for (ToolDefinition def : tools.values()) {
      out.add(toSpecification(def));
    }
    return out;
  }

  private SyncToolSpecification toSpecification(ToolDefinition def) {
    Tool tool =
        Tool.builder()
            .name(def.name())
            .description(def.description())
            .inputSchema(mcpMapper, def.inputSchema().toString())
            .build();
    return SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> dispatch(def, request))
        .build();
  }

  private CallToolResult dispatch(ToolDefinition def, CallToolRequest request) {
    JsonNode input =
        jsonMapper.valueToTree(request.arguments() == null ? Map.of() : request.arguments());
    try {
      Object result = def.execute(input);
      String body = jsonMapper.writeValueAsString(result);
      return CallToolResult.builder()
          .addContent(new TextContent(body))
          .structuredContent(jsonMapper.convertValue(result, Map.class))
          .isError(false)
          .build();
    } catch (McpException ex) {
      log.warn("tool={} code={} message={}", def.name(), ex.code(), ex.getMessage());
      return errorResult(ErrorEnvelope.of(ex));
    } catch (RuntimeException ex) {
      log.error("tool={} unexpected failure", def.name(), ex);
      ErrorEnvelope env =
          new ErrorEnvelope(
              ErrorCode.INTERNAL_ERROR.name(),
              ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()),
              Map.of("exception", ex.getClass().getName()));
      return errorResult(env);
    } catch (Exception ex) {
      log.error("tool={} checked failure", def.name(), ex);
      ErrorEnvelope env =
          new ErrorEnvelope(
              ErrorCode.INTERNAL_ERROR.name(),
              ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()),
              Map.of("exception", ex.getClass().getName()));
      return errorResult(env);
    }
  }

  private CallToolResult errorResult(ErrorEnvelope env) {
    try {
      String body = jsonMapper.writeValueAsString(env);
      return CallToolResult.builder()
          .addContent(new TextContent(body))
          .structuredContent(jsonMapper.convertValue(env, Map.class))
          .isError(true)
          .build();
    } catch (Exception serializationFailure) {
      // Keep the stdio transport alive even when the error envelope itself is unserialisable:
      // a minimal well-formed response beats letting the exception bubble past the SDK. The
      // fallback envelope still includes details={} and echoes the same fields into
      // structuredContent so clients that key off either channel see a consistent shape.
      log.error("error-envelope serialisation failed", serializationFailure);
      return CallToolResult.builder()
          .addContent(
              new TextContent(
                  "{\"code\":\"INTERNAL_ERROR\","
                      + "\"message\":\"error-response serialization failed\","
                      + "\"details\":{}}"))
          .structuredContent(
              Map.of(
                  "code",
                  "INTERNAL_ERROR",
                  "message",
                  "error-response serialization failed",
                  "details",
                  Map.of()))
          .isError(true)
          .build();
    }
  }
}
