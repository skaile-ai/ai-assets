package com.skaile.excelmcp.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.skaile.excelmcp.config.ServerConfig;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.HandleRegistry;
import com.skaile.excelmcp.shape.CellShape;
import com.skaile.excelmcp.shape.CellStyleSpec;
import com.skaile.excelmcp.shape.CellStyleSpec.BorderEdge;
import com.skaile.excelmcp.shape.CellStyleSpec.BorderSpec;
import com.skaile.excelmcp.shape.CellStyleSpec.FontSpec;
import com.skaile.excelmcp.shape.RangeShape;
import com.skaile.excelmcp.shape.SheetFormatSpec;
import com.skaile.excelmcp.shape.SheetFormatSpec.ColumnWidth;
import com.skaile.excelmcp.shape.SheetFormatSpec.FreezePane;
import com.skaile.excelmcp.shape.SheetFormatSpec.RowHeight;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** range.set_style / sheet.set_format engine coverage: round-trip fidelity, dedupe, merge. */
class StyleApplyTest {

  private static final ServerConfig CFG =
      new ServerConfig(
          Optional.empty(),
          true,
          ServerConfig.DEFAULT_MAX_FILE_BYTES,
          ServerConfig.DEFAULT_MAX_CELLS);

  @Test
  void appliesAndRoundTripsCellStyle(@TempDir Path tmp) throws Exception {
    Path dest = tmp.resolve("styled.xlsx");
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.of(dest));
      engine.writeRange(id, "Sheet1", 0, 0, List.of(List.of("Revenue", 1234.0)), null);

      CellStyleSpec spec =
          new CellStyleSpec(
              "#7300FF",
              new FontSpec("Inter", 12, true, null, "#FFFFFF"),
              new BorderSpec(null, new BorderEdge("medium", "#7300FF"), null, null),
              "#,##0",
              "center",
              "middle",
              true);
      int styled = engine.applyStyle(id, "Sheet1", "A1:B1", spec);
      assertThat(styled).isEqualTo(2);

      engine.save(id, Optional.empty());
      engine.close(id);

      HandleId re = engine.open(dest);
      RangeShape rs = engine.readRange(re, "Sheet1", "A1:B1", true, 10);
      CellShape a1 = rs.cells().get(0).get(0);
      Map<String, Object> fmt = a1.formatting();
      assertThat(fmt.get("fill_color")).isEqualTo("#7300FF");
      assertThat(fmt.get("number_format")).isEqualTo("#,##0");
      assertThat(fmt.get("horizontal_alignment")).isEqualTo("center");
      assertThat(fmt.get("wrap_text")).isEqualTo(true);

      @SuppressWarnings("unchecked")
      Map<String, Object> font = (Map<String, Object>) fmt.get("font");
      assertThat(font.get("bold")).isEqualTo(true);
      assertThat(font.get("name")).isEqualTo("Inter");
      assertThat(font.get("size")).isEqualTo(12);
      assertThat(font.get("color")).isEqualTo("#FFFFFF");
      engine.close(re);
    }
  }

  @Test
  void mergeKeepsExistingAttributesWhenSettingOthers(@TempDir Path tmp) throws Exception {
    Path dest = tmp.resolve("merge.xlsx");
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.of(dest));
      engine.writeRange(id, "Sheet1", 0, 0, List.of(List.of(0.25)), null);

      // First set only a number format, then only a fill — the fill call must not wipe the format.
      engine.applyStyle(
          id, "Sheet1", "A1", new CellStyleSpec(null, null, null, "0%", null, null, null));
      engine.applyStyle(
          id, "Sheet1", "A1", new CellStyleSpec("#F3EAFF", null, null, null, null, null, null));

      RangeShape rs = engine.readRange(id, "Sheet1", "A1", true, 10);
      Map<String, Object> fmt = rs.cells().get(0).get(0).formatting();
      assertThat(fmt.get("number_format")).isEqualTo("0%");
      assertThat(fmt.get("fill_color")).isEqualTo("#F3EAFF");
      engine.close(id);
    }
  }

  @Test
  void dedupesStylesAcrossAUniformRange(@TempDir Path tmp) throws Exception {
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.empty());
      // Reach the live workbook to count styles before/after.
      XSSFWorkbook wb = openWorkbookField(engine, id);
      int before = wb.getNumCellStyles();

      engine.applyStyle(
          id, "Sheet1", "A1:J1", new CellStyleSpec("#7300FF", null, null, null, null, null, null));

      // 10 cells, identical base style + identical spec => exactly one new cell style.
      assertThat(wb.getNumCellStyles()).isEqualTo(before + 1);
      engine.close(id);
    }
  }

  @Test
  void appliesSheetFormatAndRoundTrips(@TempDir Path tmp) throws Exception {
    Path dest = tmp.resolve("sheetfmt.xlsx");
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.of(dest));
      engine.applySheetFormat(
          id,
          "Sheet1",
          new SheetFormatSpec(
              List.of(new ColumnWidth("A", 28)),
              List.of(new RowHeight(1, 28)),
              new FreezePane(1, 0),
              "#7300FF"));
      engine.save(id, Optional.empty());
      engine.close(id);

      try (XSSFWorkbook wb = new XSSFWorkbook(dest.toString())) {
        XSSFSheet sheet = wb.getSheetAt(0);
        assertThat(sheet.getColumnWidth(0)).isEqualTo(28 * 256);
        assertThat(sheet.getRow(0).getHeightInPoints()).isEqualTo(28f);
        assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
        assertThat(sheet.getTabColor().getARGBHex()).endsWith("7300FF");
      }
    }
  }

  @Test
  void fontMergePreservesUnderlineAndStrikeoutWhenChangingColor(@TempDir Path tmp)
      throws Exception {
    Path dest = tmp.resolve("underline.xlsx");
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.of(dest));

      // Pre-style A1 with an underlined, struck-through, bold font.
      XSSFWorkbook wb = openWorkbookField(engine, id);
      XSSFFont uf = wb.createFont();
      uf.setUnderline(Font.U_SINGLE);
      uf.setStrikeout(true);
      uf.setBold(true);
      XSSFCellStyle base = wb.createCellStyle();
      base.setFont(uf);
      var cell = wb.getSheetAt(0).createRow(0).createCell(0);
      cell.setCellValue("x");
      cell.setCellStyle(base);

      // Change ONLY the font color — underline/strikeout/bold must survive the merge.
      engine.applyStyle(
          id,
          "Sheet1",
          "A1",
          new CellStyleSpec(
              null, new FontSpec(null, null, null, null, "#FF0000"), null, null, null, null, null));

      engine.save(id, Optional.empty());
      engine.close(id);

      try (XSSFWorkbook re = new XSSFWorkbook(dest.toString())) {
        XSSFFont font = (XSSFFont) re.getSheetAt(0).getRow(0).getCell(0).getCellStyle().getFont();
        assertThat(font.getUnderline()).isEqualTo(Font.U_SINGLE);
        assertThat(font.getStrikeout()).isTrue();
        assertThat(font.getBold()).isTrue();
        assertThat(font.getXSSFColor().getARGBHex()).endsWith("FF0000");
      }
    }
  }

  @Test
  void appliesBordersThatRoundTrip(@TempDir Path tmp) throws Exception {
    Path dest = tmp.resolve("borders.xlsx");
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.of(dest));
      engine.writeRange(id, "Sheet1", 0, 0, List.of(List.of("x")), null);
      engine.applyStyle(
          id,
          "Sheet1",
          "A1",
          new CellStyleSpec(
              null,
              null,
              new BorderSpec(
                  new BorderEdge("thin", "#000000"),
                  new BorderEdge("medium", "#7300FF"),
                  null,
                  null),
              null,
              null,
              null,
              null));
      engine.save(id, Optional.empty());
      engine.close(id);

      try (XSSFWorkbook re = new XSSFWorkbook(dest.toString())) {
        XSSFCellStyle st = re.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
        assertThat(st.getBorderTop()).isEqualTo(BorderStyle.THIN);
        assertThat(st.getBorderBottom()).isEqualTo(BorderStyle.MEDIUM);
      }
    }
  }

  @Test
  void rejectsStylingOnHssfWorkbook(@TempDir Path tmp) throws Exception {
    Path xls = tmp.resolve("legacy.xls");
    try (HSSFWorkbook wb = new HSSFWorkbook();
        OutputStream os = Files.newOutputStream(xls)) {
      wb.createSheet("Sheet1").createRow(0).createCell(0).setCellValue("x");
      wb.write(os);
    }
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.open(xls);
      McpException ex =
          assertThrows(
              McpException.class,
              () ->
                  engine.applyStyle(
                      id,
                      "Sheet1",
                      "A1",
                      new CellStyleSpec("#FFFFFF", null, null, null, null, null, null)));
      assertThat(ex.code().name()).isEqualTo("STYLE_INVALID");
      engine.close(id);
    }
  }

  @Test
  void rejectsExcessiveColumnWidth(@TempDir Path tmp) throws Exception {
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.empty());
      McpException ex =
          assertThrows(
              McpException.class,
              () ->
                  engine.applySheetFormat(
                      id,
                      "Sheet1",
                      new SheetFormatSpec(List.of(new ColumnWidth("A", 300)), null, null, null)));
      assertThat(ex.code().name()).isEqualTo("STYLE_INVALID");
      engine.close(id);
    }
  }

  @Test
  void rejectsFullColumnStyling(@TempDir Path tmp) throws Exception {
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.empty());
      McpException ex =
          assertThrows(
              McpException.class,
              () ->
                  engine.applyStyle(
                      id,
                      "Sheet1",
                      "A:A",
                      new CellStyleSpec("#FFFFFF", null, null, null, null, null, null)));
      assertThat(ex.code().name()).isEqualTo("STYLE_INVALID");
      assertThat(ex.getMessage()).contains("full-column/full-row");
      engine.close(id);
    }
  }

  @Test
  void rejectsMalformedColor(@TempDir Path tmp) throws Exception {
    HandleRegistry registry = new HandleRegistry();
    try (WorkbookEngine engine = new XssfInMemoryEngine(CFG, registry)) {
      HandleId id = engine.create(Optional.empty());
      McpException ex =
          assertThrows(
              McpException.class,
              () ->
                  engine.applyStyle(
                      id,
                      "Sheet1",
                      "A1",
                      new CellStyleSpec("purple", null, null, null, null, null, null)));
      assertThat(ex.code().name()).isEqualTo("STYLE_INVALID");
      engine.close(id);
    }
  }

  /** Reflectively reach the engine's in-memory workbook for white-box style-count assertions. */
  private static XSSFWorkbook openWorkbookField(WorkbookEngine engine, HandleId id)
      throws Exception {
    var field = XssfInMemoryEngine.class.getDeclaredField("workbooks");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<HandleId, org.apache.poi.ss.usermodel.Workbook> map =
        (Map<HandleId, org.apache.poi.ss.usermodel.Workbook>) field.get(engine);
    return (XSSFWorkbook) map.get(id);
  }
}
