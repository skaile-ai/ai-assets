package com.skaile.excelmcp.engine.poi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Atomic-saver behaviour. The atomic-rename fallback is filesystem-specific and not portable to a
 * unit test, but we cover the happy-path and the IOException cleanup branch (which guards against
 * leaving a stranded ".tmp-…" sibling if the underlying write fails).
 */
class PoiAtomicSaverTest {

  @Test
  void happyPathWritesDestinationAndLeavesNoTempSibling(@TempDir Path dir) throws Exception {
    Path dest = dir.resolve("out.xlsx");
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      wb.createSheet("Sheet1");
      long size = PoiAtomicSaver.save(wb, dest);
      assertThat(size).isPositive();
    }
    assertThat(Files.exists(dest)).isTrue();
    assertThat(siblingTempCount(dir, dest)).isZero();
  }

  @Test
  void writeFailureDeletesTempAndRaisesSaveFailed(@TempDir Path dir) throws Exception {
    // Pointing the destination at a non-existent parent directory makes Files.newOutputStream on
    // the sibling temp file throw NoSuchFileException — exercising the IOException cleanup path
    // without depending on filesystem permissions or platform-specific quirks.
    Path missingDir = dir.resolve("does-not-exist");
    Path dest = missingDir.resolve("out.xlsx");
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      wb.createSheet("Sheet1");
      assertThatThrownBy(() -> PoiAtomicSaver.save(wb, dest))
          .isInstanceOf(McpException.class)
          .extracting(ex -> ((McpException) ex).code())
          .isEqualTo(ErrorCode.SAVE_FAILED);
    }
    assertThat(Files.exists(dest)).isFalse();
    // No temp sibling could have been created either, since the parent dir doesn't exist.
    assertThat(Files.exists(missingDir)).isFalse();
  }

  @Test
  void saveFailedDetailsExposeOnlyDestinationBasename(@TempDir Path dir) throws Exception {
    Path missingDir = dir.resolve("nope");
    Path dest = missingDir.resolve("secret-name.xlsx");
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      wb.createSheet("Sheet1");
      McpException ex =
          org.junit.jupiter.api.Assertions.assertThrows(
              McpException.class, () -> PoiAtomicSaver.save(wb, dest));
      assertThat(ex.details()).containsEntry("destination", "secret-name.xlsx");
      assertThat(ex.details().get("destination").toString()).doesNotContain(missingDir.toString());
    }
  }

  private static long siblingTempCount(Path dir, Path dest) throws IOException {
    String prefix = dest.getFileName().toString() + ".tmp-";
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      return StreamSupport.stream(stream.spliterator(), false)
          .filter(p -> p.getFileName().toString().startsWith(prefix))
          .count();
    }
  }
}
