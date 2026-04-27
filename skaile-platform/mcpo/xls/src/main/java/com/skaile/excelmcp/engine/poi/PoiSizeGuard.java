package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Enforces the two size caps from §5.4:
 *
 * <ul>
 *   <li>pre-load: reject if the file on disk exceeds {@code EXCEL_MCP_MAX_FILE_BYTES};
 *   <li>post-load: reject if the total populated-cell count exceeds {@code EXCEL_MCP_MAX_CELLS}.
 * </ul>
 */
public final class PoiSizeGuard {

  private PoiSizeGuard() {}

  public static void assertFileSize(Path path, long maxBytes) throws McpException {
    long size;
    try {
      size = Files.size(path);
    } catch (IOException e) {
      throw new McpException(
          ErrorCode.PATH_INVALID,
          "cannot stat file: " + e.getMessage(),
          Map.of("path", path.toString()),
          e);
    }
    if (size > maxBytes) {
      throw new McpException(
          ErrorCode.WORKBOOK_TOO_LARGE,
          "file " + size + " bytes exceeds limit " + maxBytes,
          Map.of("path", path.toString(), "size_bytes", size, "limit_bytes", maxBytes));
    }
  }

  public static long countPopulatedCells(Workbook wb) {
    long total = 0;
    for (Sheet sheet : wb) {
      for (var row : sheet) {
        total += row.getPhysicalNumberOfCells();
      }
    }
    return total;
  }

  public static void assertCellCount(Workbook wb, long maxCells) throws McpException {
    long count = countPopulatedCells(wb);
    if (count > maxCells) {
      throw new McpException(
          ErrorCode.WORKBOOK_TOO_MANY_CELLS,
          "workbook has " + count + " cells; limit is " + maxCells,
          Map.of("cell_count", count, "limit_cells", maxCells));
    }
  }
}
