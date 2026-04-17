package com.skaile.excelmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skaile.excelmcp.config.ServerConfig;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.engine.XssfInMemoryEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleRegistry;
import com.skaile.excelmcp.log.LoggingSetup;
import com.skaile.excelmcp.path.ExcelMcpRoot;
import com.skaile.excelmcp.path.PathValidator;
import com.skaile.excelmcp.server.McpServerGlue;
import com.skaile.excelmcp.server.ToolRegistry;
import com.skaile.excelmcp.shape.ShapeMapper;
import com.skaile.excelmcp.tools.range.RangeClearTool;
import com.skaile.excelmcp.tools.range.RangeGetTool;
import com.skaile.excelmcp.tools.range.RangeSetTool;
import com.skaile.excelmcp.tools.sheet.SheetCreateTool;
import com.skaile.excelmcp.tools.sheet.SheetDeleteColsTool;
import com.skaile.excelmcp.tools.sheet.SheetDeleteRowsTool;
import com.skaile.excelmcp.tools.sheet.SheetDeleteTool;
import com.skaile.excelmcp.tools.sheet.SheetInsertColsTool;
import com.skaile.excelmcp.tools.sheet.SheetInsertRowsTool;
import com.skaile.excelmcp.tools.sheet.SheetMergedRegionsTool;
import com.skaile.excelmcp.tools.sheet.SheetRenameTool;
import com.skaile.excelmcp.tools.workbook.WorkbookCapabilitiesReportTool;
import com.skaile.excelmcp.tools.workbook.WorkbookCloseTool;
import com.skaile.excelmcp.tools.workbook.WorkbookCreateTool;
import com.skaile.excelmcp.tools.workbook.WorkbookListSheetsTool;
import com.skaile.excelmcp.tools.workbook.WorkbookMetadataTool;
import com.skaile.excelmcp.tools.workbook.WorkbookOpenTool;
import com.skaile.excelmcp.tools.workbook.WorkbookRecalculateTool;
import com.skaile.excelmcp.tools.workbook.WorkbookSaveTool;
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
    tools.register(new WorkbookRecalculateTool(engine));
    tools.register(new WorkbookCapabilitiesReportTool(engine));
    tools.register(new RangeGetTool(engine));
    tools.register(new RangeSetTool(engine));
    tools.register(new RangeClearTool(engine));
    tools.register(new SheetCreateTool(engine));
    tools.register(new SheetDeleteTool(engine));
    tools.register(new SheetRenameTool(engine));
    tools.register(new SheetMergedRegionsTool(engine));
    tools.register(new SheetInsertRowsTool(engine));
    tools.register(new SheetDeleteRowsTool(engine));
    tools.register(new SheetInsertColsTool(engine));
    tools.register(new SheetDeleteColsTool(engine));

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
