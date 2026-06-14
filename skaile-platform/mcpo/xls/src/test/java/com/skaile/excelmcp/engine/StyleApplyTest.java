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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
