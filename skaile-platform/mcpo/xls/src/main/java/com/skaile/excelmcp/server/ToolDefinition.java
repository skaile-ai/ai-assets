package com.skaile.excelmcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.skaile.excelmcp.error.McpException;

/**
 * Contract every tool implements. Kept small on purpose: the tool owns its name, input-schema, and
 * execution; the registry handles the SDK wiring.
 *
 * <p>Tool handlers receive already-parsed JSON input and return an arbitrary object that will be
 * JSON-serialised by the dispatcher. Errors are signalled by throwing {@link McpException}.
 */
public interface ToolDefinition {

  /** Canonical tool name, e.g. {@code "workbook.open"}. */
  String name();

  /** Short human description (surfaced to the agent as tool metadata). */
  String description();

  /**
   * JSON Schema (draft-07 / 2020-12) for the input object. Used by the agent to know what to send.
   */
  JsonNode inputSchema();

  /**
   * Execute the tool. The dispatcher has already resolved input JSON; the result will be
   * JSON-serialised with the shared {@code ShapeMapper}.
   */
  Object execute(JsonNode input) throws McpException;
}
