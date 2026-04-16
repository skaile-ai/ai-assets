package com.portfolex.excelmcp.engine.poi;

import com.portfolex.excelmcp.error.ErrorCode;
import com.portfolex.excelmcp.error.McpException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Writes a POI {@link Workbook} to a sibling temp file and atomically renames it into place (§1.3).
 * Mid-write crashes cannot corrupt the original file. Returns the size written.
 */
public final class PoiAtomicSaver {

  private PoiAtomicSaver() {}

  public static long save(Workbook wb, Path destination) throws McpException {
    Path parent = destination.getParent();
    String suffix = ".tmp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Path tmp = (parent == null ? Path.of(".") : parent).resolve(destination.getFileName() + suffix);
    try {
      try (OutputStream out = Files.newOutputStream(tmp)) {
        wb.write(out);
      }
      try {
        Files.move(
            tmp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException atomicFailure) {
        // Filesystem does not support ATOMIC_MOVE (rare — e.g. cross-filesystem). Fall back to a
        // plain replace so the operation still completes; callers see a consistent post-state.
        Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING);
      }
      return Files.size(destination);
    } catch (IOException ex) {
      try {
        Files.deleteIfExists(tmp);
      } catch (IOException ignored) {
        // best effort
      }
      throw new McpException(
          ErrorCode.SAVE_FAILED,
          "atomic save failed: " + ex.getMessage(),
          Map.of("destination", destination.toString()),
          ex);
    }
  }
}
