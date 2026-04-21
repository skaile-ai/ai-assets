package com.skaile.excelmcp.tools.vba;

import com.skaile.excelmcp.handles.OpenWorkbook;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;

/**
 * Cross-tool helper for the {@code source_disk_mtime_changed_since_open} flag exposed by the VBA
 * tools (review nit C3). Returns {@code false} whenever we don't have a baseline mtime (workbook
 * was created without a source path) or the stat fails — agents should treat the flag as a
 * best-effort signal, not a guarantee.
 */
final class VbaSourceFreshness {

  private VbaSourceFreshness() {}

  static boolean changedSinceOpen(OpenWorkbook meta) {
    Optional<Instant> baseline = meta.openedSourceMtime();
    if (baseline.isEmpty() || meta.sourcePath() == null) {
      return false;
    }
    try {
      Instant current = Files.getLastModifiedTime(meta.sourcePath()).toInstant();
      return !current.equals(baseline.get());
    } catch (IOException ignored) {
      // Best effort: if we can't stat the file (deleted, permission revoked, etc.), surface
      // false rather than crashing the call. The VBA extractor itself will raise the right
      // VBA_NOT_PRESENT envelope if the file is genuinely gone.
      return false;
    }
  }
}
