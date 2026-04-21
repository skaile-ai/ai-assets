package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a POI {@link Workbook} to a sibling temp file and atomically renames it into place (§1.3).
 * Mid-write crashes cannot corrupt the original file. Returns the size written.
 */
public final class PoiAtomicSaver {

  private static final Logger log = LoggerFactory.getLogger(PoiAtomicSaver.class);

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
        // Operators need to know the atomicity guarantee was lost: if the JVM dies between the
        // delete and the rename of a non-atomic move, the destination can end up missing.
        log.warn(
            "atomic rename not supported on target filesystem; falling back to non-atomic replace"
                + " (a crash during rename may leave the destination missing). destination={}",
            destination.getFileName(),
            atomicFailure);
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
          Map.of("destination", destination.getFileName().toString()),
          ex);
    }
  }
}
