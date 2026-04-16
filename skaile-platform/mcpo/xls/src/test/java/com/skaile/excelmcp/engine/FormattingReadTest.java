package com.skaile.excelmcp.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.skaile.excelmcp.config.ServerConfig;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.HandleRegistry;
import com.skaile.excelmcp.shape.CellShape;
import com.skaile.excelmcp.shape.RangeShape;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * include_formatting=true should populate the {@code formatting} map with the §7.1 fields;
 * include_formatting=false (default) should leave it absent.
 */
class FormattingReadTest {

  private static final ServerConfig CFG =
      new ServerConfig(
          Optional.empty(), ServerConfig.DEFAULT_MAX_FILE_BYTES, ServerConfig.DEFAULT_MAX_CELLS);

  @Test
  void extractsFontFillAlignmentAndNumberFormat(@TempDir Path tmp) throws Exception {
    Path fixture = writeStyledFixture(tmp);
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.open(fixture);

      RangeShape withoutFmt = engine.readRange(id, "Data", "A1:A1", false, 10);
      assertThat(withoutFmt.cells().get(0).get(0).formatting()).isNull();

      RangeShape withFmt = engine.readRange(id, "Data", "A1:A1", true, 10);
      CellShape a1 = withFmt.cells().get(0).get(0);
      Map<String, Object> fmt = a1.formatting();
      assertThat(fmt).isNotNull();

      assertThat(fmt.get("number_format")).isEqualTo("0.00%");
      assertThat(fmt.get("horizontal_alignment")).isEqualTo("center");
      assertThat(fmt.get("wrap_text")).isEqualTo(true);
      assertThat(fmt.get("fill_color")).isEqualTo("#FFFF00");

      @SuppressWarnings("unchecked")
      Map<String, Object> font = (Map<String, Object>) fmt.get("font");
      assertThat(font.get("bold")).isEqualTo(true);
      assertThat(font.get("italic")).isEqualTo(false);
      assertThat(font.get("size")).isEqualTo(14);
      assertThat(font.get("name")).isEqualTo("Calibri");
      assertThat(font.get("color")).isEqualTo("#FF0000");

      engine.close(id);
    }
  }

  private static Path writeStyledFixture(Path tmp) throws Exception {
    Path out = tmp.resolve("styled.xlsx");
    try (XSSFWorkbook wb = new XSSFWorkbook();
        OutputStream os = Files.newOutputStream(out)) {
      XSSFCellStyle style = wb.createCellStyle();
      style.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
      style.setAlignment(HorizontalAlignment.CENTER);
      style.setWrapText(true);
      style.setFillForegroundColor(
          new XSSFColor(new byte[] {(byte) 0xFF, (byte) 0xFF, 0x00}, null));
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      XSSFFont font = wb.createFont();
      font.setBold(true);
      font.setFontHeightInPoints((short) 14);
      font.setFontName("Calibri");
      font.setColor(new XSSFColor(new byte[] {(byte) 0xFF, 0x00, 0x00}, null));
      style.setFont(font);

      var sheet = wb.createSheet("Data");
      Cell a1 = sheet.createRow(0).createCell(0);
      a1.setCellStyle(style);
      a1.setCellValue(0.1234);
      wb.write(os);
    }
    return out;
  }
}
