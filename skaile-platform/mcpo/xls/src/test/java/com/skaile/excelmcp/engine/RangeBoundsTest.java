package com.skaile.excelmcp.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skaile.excelmcp.config.ServerConfig;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.HandleRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Regression for the missing format-bounds check: A1:XFE1 and similar should be rejected as {@code
 * RANGE_OUT_OF_BOUNDS} instead of silently returning blanks (xlsx maxes out at XFD x 1,048,576).
 * Same check must fire on reads, writes, and clears.
 */
class RangeBoundsTest {

  private static final ServerConfig CFG =
      new ServerConfig(
          Optional.empty(),
          true,
          ServerConfig.DEFAULT_MAX_FILE_BYTES,
          ServerConfig.DEFAULT_MAX_CELLS);

  @Test
  void readPastLastColumnIsRejected() throws Exception {
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, new HandleRegistry())) {
      HandleId id = engine.create(Optional.empty());
      assertThatThrownBy(() -> engine.readRange(id, "Sheet1", "A1:XFE1", false, 10_000))
          .isInstanceOf(McpException.class)
          .satisfies(
              ex -> {
                McpException mx = (McpException) ex;
                assertThat(mx.code()).isEqualTo(ErrorCode.RANGE_OUT_OF_BOUNDS);
                assertThat(mx.getMessage()).contains("XFD").contains("1048576");
              });
    }
  }

  @Test
  void readPastLastRowIsRejected() throws Exception {
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, new HandleRegistry())) {
      HandleId id = engine.create(Optional.empty());
      assertThatThrownBy(() -> engine.readRange(id, "Sheet1", "A1:A1048577", false, 10))
          .isInstanceOf(McpException.class)
          .extracting(ex -> ((McpException) ex).code())
          .isEqualTo(ErrorCode.RANGE_OUT_OF_BOUNDS);
    }
  }

  @Test
  void writePastLastColumnIsRejected() throws Exception {
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, new HandleRegistry())) {
      HandleId id = engine.create(Optional.empty());
      // Start column is XFE (0-based 16384 — one past XFD).
      int xfeCol = com.skaile.excelmcp.shape.RangeAddress.colIndex("XFE");
      assertThatThrownBy(
              () -> engine.writeRange(id, "Sheet1", 0, xfeCol, List.of(List.of("x")), null))
          .isInstanceOf(McpException.class)
          .extracting(ex -> ((McpException) ex).code())
          .isEqualTo(ErrorCode.RANGE_OUT_OF_BOUNDS);
    }
  }

  @Test
  void clearPastLastColumnIsRejected() throws Exception {
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, new HandleRegistry())) {
      HandleId id = engine.create(Optional.empty());
      assertThatThrownBy(() -> engine.clearRange(id, "Sheet1", "A1:XFE1"))
          .isInstanceOf(McpException.class)
          .extracting(ex -> ((McpException) ex).code())
          .isEqualTo(ErrorCode.RANGE_OUT_OF_BOUNDS);
    }
  }

  @Test
  void readAtLastValidCellIsAccepted() throws Exception {
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, new HandleRegistry())) {
      HandleId id = engine.create(Optional.empty());
      // XFD is column 16383 (0-based). A1:XFD1 exactly hits the last column — must succeed.
      assertThat(engine.readRange(id, "Sheet1", "XFD1:XFD1", false, 10).cols()).isEqualTo(1);
    }
  }

  @Test
  void fullColumnReadOnXlsxClampsToFormatRowBound() throws Exception {
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, new HandleRegistry())) {
      HandleId id = engine.create(Optional.empty());
      // A:A is treated as the entire column; on xlsx the engine truncates at max_cells but never
      // fails the format-bound check.
      var range = engine.readRange(id, "Sheet1", "A:A", false, 5);
      assertThat(range.cols()).isEqualTo(1);
      assertThat(range.truncated()).isTrue();
    }
  }
}
