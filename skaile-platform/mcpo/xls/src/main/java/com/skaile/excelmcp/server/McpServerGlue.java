package com.skaile.excelmcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skaile.excelmcp.log.LoggingSetup;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin façade around the MCP SDK. Builds the stdio transport against the <em>real</em> stdout
 * captured by {@link LoggingSetup} (not {@code System.out}, which has been redirected to stderr),
 * then wires in the tool registry.
 */
public final class McpServerGlue {

  private static final Logger log = LoggerFactory.getLogger(McpServerGlue.class);

  public static final String SERVER_NAME = "excel-mcp";
  public static final String SERVER_VERSION = "0.2.1";

  private final ObjectMapper jsonMapper;
  private final ToolRegistry registry;

  public McpServerGlue(ObjectMapper jsonMapper, ToolRegistry registry) {
    this.jsonMapper = jsonMapper;
    this.registry = registry;
  }

  public McpSyncServer start() {
    StdioServerTransportProvider transport =
        new StdioServerTransportProvider(
            new JacksonMcpJsonMapper(jsonMapper), System.in, LoggingSetup.realStdout());
    McpSyncServer server =
        McpServer.sync(transport)
            .serverInfo(SERVER_NAME, SERVER_VERSION)
            .capabilities(ServerCapabilities.builder().tools(true).build())
            .tools(registry.asSyncSpecifications())
            .build();
    log.info(
        "mcp server started name={} version={} tools={}",
        SERVER_NAME,
        SERVER_VERSION,
        server.listTools().size());
    return server;
  }
}
