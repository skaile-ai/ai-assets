package com.portfolex.excelmcp.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolex.excelmcp.config.ServerConfig;
import com.portfolex.excelmcp.handles.HandleId;
import com.portfolex.excelmcp.handles.HandleRegistry;
import com.portfolex.excelmcp.shape.CellShape;
import com.portfolex.excelmcp.shape.RangeShape;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Phase 2 verify gate: engine loads a fixture xlsx and reads a known cell. */
class XssfInMemoryEngineTest {

  private static final ServerConfig CFG =
      new ServerConfig(
          Optional.empty(), ServerConfig.DEFAULT_MAX_FILE_BYTES, ServerConfig.DEFAULT_MAX_CELLS);

  @Test
  void opensFixtureAndReadsKnownCell(@TempDir Path tmp) throws Exception {
    Path fixture = writeSimpleFixture(tmp);
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.open(fixture);

      RangeShape range = engine.readRange(id, "Sheet1", "A1:B2", false, 100);
      assertThat(range.sheet()).isEqualTo("Sheet1");
      assertThat(range.rows()).isEqualTo(2);
      assertThat(range.cols()).isEqualTo(2);

      CellShape a1 = range.cells().get(0).get(0);
      assertThat(a1.a1()).isEqualTo("A1");
      assertThat(a1.type()).isEqualTo("string");
      assertThat(a1.value()).isEqualTo("Name");

      CellShape b2 = range.cells().get(1).get(1);
      assertThat(b2.a1()).isEqualTo("B2");
      assertThat(b2.type()).isEqualTo("number");
      assertThat(b2.value()).isEqualTo(42L);

      engine.close(id);
    }
  }

  @Test
  void rejectsXlsb(@TempDir Path tmp) throws Exception {
    Path xlsb = tmp.resolve("bad.xlsb");
    Files.write(xlsb, new byte[] {0x50, 0x4B}); // fake ZIP magic, won't be parsed
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      var thrown =
          org.junit.jupiter.api.Assertions.assertThrows(
              com.portfolex.excelmcp.error.McpException.class, () -> engine.open(xlsb));
      assertThat(thrown.code().name()).isEqualTo("FORMAT_UNSUPPORTED");
      assertThat(thrown.getMessage()).contains(".xlsb is not supported in v1");
    }
  }

  private static Path writeSimpleFixture(Path tmp) throws Exception {
    Path out = tmp.resolve("simple.xlsx");
    try (XSSFWorkbook wb = new XSSFWorkbook();
        OutputStream os = Files.newOutputStream(out)) {
      Sheet s = wb.createSheet("Sheet1");
      Row r1 = s.createRow(0);
      r1.createCell(0).setCellValue("Name");
      r1.createCell(1).setCellValue("Q1");
      Row r2 = s.createRow(1);
      r2.createCell(0).setCellValue("Acme");
      r2.createCell(1).setCellValue(42);
      wb.write(os);
    }
    return out;
  }
}
