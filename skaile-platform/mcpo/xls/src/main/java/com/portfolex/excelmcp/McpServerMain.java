package com.portfolex.excelmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolex.excelmcp.config.ServerConfig;
import com.portfolex.excelmcp.engine.WorkbookEngine;
import com.portfolex.excelmcp.engine.XssfInMemoryEngine;
import com.portfolex.excelmcp.error.McpException;
import com.portfolex.excelmcp.handles.HandleRegistry;
import com.portfolex.excelmcp.log.LoggingSetup;
import com.portfolex.excelmcp.path.ExcelMcpRoot;
import com.portfolex.excelmcp.path.PathValidator;
import com.portfolex.excelmcp.server.McpServerGlue;
import com.portfolex.excelmcp.server.ToolRegistry;
import com.portfolex.excelmcp.shape.ShapeMapper;
import com.portfolex.excelmcp.tools.range.RangeClearTool;
import com.portfolex.excelmcp.tools.range.RangeGetTool;
import com.portfolex.excelmcp.tools.range.RangeSetTool;
import com.portfolex.excelmcp.tools.workbook.WorkbookCloseTool;
import com.portfolex.excelmcp.tools.workbook.WorkbookCreateTool;
import com.portfolex.excelmcp.tools.workbook.WorkbookListSheetsTool;
import com.portfolex.excelmcp.tools.workbook.WorkbookMetadataTool;
import com.portfolex.excelmcp.tools.workbook.WorkbookOpenTool;
import com.portfolex.excelmcp.tools.workbook.WorkbookSaveTool;
import io.modelcontextprotocol.server.McpSyncServer;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point. Locks stdout down, parses config, constructs the engine and tool registry, starts
 * the MCP stdio server, then blocks on a shutdown latch.
 */
public final class McpServerMain {

  private static final Logger log = LoggerFactory.getLogger(McpServerMain.class);

  private McpServerMain() {}

  public static void main(String[] args) {
    LoggingSetup.lockDownStdout();

    ServerConfig config = ServerConfig.fromEnvironment();
    ExcelMcpRoot root;
    try {
      root = ExcelMcpRoot.resolve(config.excelMcpRoot());
    } catch (McpException e) {
      log.error("invalid EXCEL_MCP_ROOT: {}", e.getMessage());
      System.exit(2);
      return;
    }
    if (root.isEnabled()) {
      log.info("EXCEL_MCP_ROOT={} (path sandboxing enabled)", root.canonical().orElseThrow());
    } else {
      log.warn("EXCEL_MCP_ROOT not set; path sandboxing disabled");
    }
    log.info("limits maxFileBytes={} maxCells={}", config.maxFileBytes(), config.maxCells());

    HandleRegistry registry = new HandleRegistry();
    PathValidator pathValidator = new PathValidator(root);
    WorkbookEngine engine = new XssfInMemoryEngine(config, registry);

    ObjectMapper jsonMapper = ShapeMapper.newObjectMapper();
    ToolRegistry tools = new ToolRegistry(jsonMapper);
    tools.register(new WorkbookOpenTool(engine, pathValidator));
    tools.register(new WorkbookCreateTool(engine, pathValidator));
    tools.register(new WorkbookSaveTool(engine, pathValidator));
    tools.register(new WorkbookCloseTool(engine));
    tools.register(new WorkbookListSheetsTool(engine));
    tools.register(new WorkbookMetadataTool(engine));
    tools.register(new RangeGetTool(engine));
    tools.register(new RangeSetTool(engine));
    tools.register(new RangeClearTool(engine));

    McpServerGlue glue = new McpServerGlue(jsonMapper, tools);
    McpSyncServer server = glue.start();

    CountDownLatch shutdown = new CountDownLatch(1);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("shutdown signal received; closing server");
                  try {
                    server.closeGracefully();
                  } catch (RuntimeException ex) {
                    log.warn("graceful close failed: {}", ex.toString());
                  }
                  try {
                    engine.close();
                  } catch (RuntimeException ex) {
                    log.warn("engine close failed: {}", ex.toString());
                  }
                  shutdown.countDown();
                },
                "excel-mcp-shutdown"));

    try {
      shutdown.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    log.info("excel-mcp exited");
  }
}
