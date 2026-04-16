package com.skaile.excelmcp.engine;

import com.skaile.excelmcp.config.ServerConfig;
import com.skaile.excelmcp.engine.poi.PoiAtomicSaver;
import com.skaile.excelmcp.engine.poi.PoiCellReader;
import com.skaile.excelmcp.engine.poi.PoiCellWriter;
import com.skaile.excelmcp.engine.poi.PoiSizeGuard;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.HandleRegistry;
import com.skaile.excelmcp.handles.OpenWorkbook;
import com.skaile.excelmcp.path.FormatWhitelist;
import com.skaile.excelmcp.shape.CellShape;
import com.skaile.excelmcp.shape.NamedRangeRef;
import com.skaile.excelmcp.shape.RangeAddress;
import com.skaile.excelmcp.shape.RangeShape;
import com.skaile.excelmcp.shape.SheetShape;
import com.skaile.excelmcp.shape.TableRef;
import com.skaile.excelmcp.shape.WorkbookMetadataShape;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * v1 engine implementation: POI workbooks held entirely in JVM heap. Single-threaded usage by
 * contract — see §1.1.
 */
public final class XssfInMemoryEngine implements WorkbookEngine {

  private final ServerConfig config;
  private final HandleRegistry registry;
  private final Map<HandleId, Workbook> workbooks = new HashMap<>();

  public XssfInMemoryEngine(ServerConfig config, HandleRegistry registry) {
    this.config = config;
    this.registry = registry;
  }

  @Override
  public HandleId open(Path path) throws McpException {
    String format = FormatWhitelist.format(path);
    PoiSizeGuard.assertFileSize(path, config.maxFileBytes());
    Workbook wb;
    try {
      wb = WorkbookFactory.create(path.toFile(), null, /* readOnly= */ false);
    } catch (IOException ex) {
      throw new McpException(
          ErrorCode.INTERNAL_ERROR,
          "POI could not open workbook: " + ex.getMessage(),
          Map.of("path", path.toString(), "exception", ex.getClass().getName()),
          ex);
    }
    try {
      PoiSizeGuard.assertCellCount(wb, config.maxCells());
    } catch (McpException guarded) {
      safeClose(wb);
      throw guarded;
    }
    HandleId id = HandleId.newRandom();
    workbooks.put(id, wb);
    registry.register(new OpenWorkbook(id, path, format, Instant.now()));
    return id;
  }

  @Override
  public HandleId create(Optional<Path> sourcePath) throws McpException {
    String format = "xlsx";
    if (sourcePath.isPresent()) {
      format = FormatWhitelist.format(sourcePath.get());
    }
    Workbook wb = new XSSFWorkbook();
    // A brand-new XSSFWorkbook with zero sheets is invalid OOXML and Excel refuses to open the
    // saved file. Match Excel's own "New Workbook" default and seed "Sheet1" so workbook.create +
    // workbook.save produces a valid file without the caller needing sheet.create (which isn't
    // available until Phase 6 anyway).
    wb.createSheet("Sheet1");
    HandleId id = HandleId.newRandom();
    workbooks.put(id, wb);
    registry.register(new OpenWorkbook(id, sourcePath.orElse(null), format, Instant.now()));
    return id;
  }

  @Override
  public long save(HandleId id, Optional<Path> destination) throws McpException {
    Workbook wb = requireOpen(id);
    OpenWorkbook meta = registry.require(id);
    Path dest = destination.orElseGet(() -> meta.sourcePath() == null ? null : meta.sourcePath());
    if (dest == null) {
      throw new McpException(
          ErrorCode.SAVE_REQUIRES_PATH,
          "workbook has no source path; supply one explicitly",
          Map.of("handle", id.value()));
    }
    return PoiAtomicSaver.save(wb, dest);
  }

  @Override
  public void close(HandleId id) throws McpException {
    Workbook wb = workbooks.remove(id);
    if (wb == null) {
      throw new McpException(
          ErrorCode.HANDLE_CLOSED,
          "handle already closed or unknown: " + id,
          Map.of("handle", id.value()));
    }
    registry.remove(id);
    safeClose(wb);
  }

  @Override
  public OpenWorkbook describe(HandleId id) throws McpException {
    return registry.require(id);
  }

  @Override
  public Path peekSourcePath(HandleId id) {
    return registry.lookup(id).map(OpenWorkbook::sourcePath).orElse(null);
  }

  @Override
  public List<SheetShape> listSheets(HandleId id) throws McpException {
    Workbook wb = requireOpen(id);
    List<SheetShape> out = new ArrayList<>(wb.getNumberOfSheets());
    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
      out.add(new SheetShape(wb.getSheetName(i), i, wb.isSheetHidden(i)));
    }
    return out;
  }

  @Override
  public List<NamedRangeRef> listNamedRanges(HandleId id) throws McpException {
    Workbook wb = requireOpen(id);
    List<NamedRangeRef> out = new ArrayList<>();
    for (Name n : wb.getAllNames()) {
      String sheet = n.getSheetIndex() < 0 ? null : wb.getSheetName(n.getSheetIndex());
      String scope = n.getSheetIndex() < 0 ? "workbook" : "sheet";
      String refers = null;
      try {
        refers = n.getRefersToFormula();
      } catch (RuntimeException ignored) {
        // some malformed names throw; skip the range info rather than failing the whole list
      }
      out.add(new NamedRangeRef(n.getNameName(), sheet, refers, scope));
    }
    return out;
  }

  @Override
  public List<TableRef> listTables(HandleId id) throws McpException {
    Workbook wb = requireOpen(id);
    List<TableRef> out = new ArrayList<>();
    if (!(wb instanceof XSSFWorkbook xwb)) {
      return out; // .xls (HSSF) has no equivalent ListObject API
    }
    for (int i = 0; i < xwb.getNumberOfSheets(); i++) {
      XSSFSheet sheet = xwb.getSheetAt(i);
      for (XSSFTable t : sheet.getTables()) {
        String area;
        try {
          area = t.getArea() == null ? null : t.getArea().formatAsString();
        } catch (RuntimeException ignored) {
          area = null;
        }
        out.add(new TableRef(t.getName(), sheet.getSheetName(), area));
      }
    }
    return out;
  }

  @Override
  public WorkbookMetadataShape describeMetadata(
      HandleId id, boolean includeNamedRanges, boolean includeTables) throws McpException {
    OpenWorkbook meta = registry.require(id);
    requireOpen(id); // ensure workbook is still live
    String filename = meta.sourcePath() == null ? null : meta.sourcePath().getFileName().toString();
    Long size = null;
    Instant modified = null;
    if (meta.sourcePath() != null && Files.exists(meta.sourcePath())) {
      try {
        size = Files.size(meta.sourcePath());
        modified = Files.getLastModifiedTime(meta.sourcePath()).toInstant();
      } catch (IOException ignored) {
        // metadata fields remain null
      }
    }
    List<SheetShape> sheets = listSheets(id);
    List<NamedRangeRef> names = includeNamedRanges ? listNamedRanges(id) : List.of();
    List<TableRef> tables = includeTables ? listTables(id) : List.of();
    return new WorkbookMetadataShape(
        filename, size, modified, meta.format(), sheets, names, tables);
  }

  @Override
  public RangeShape readRange(
      HandleId id, String sheetName, String rangeA1, boolean includeFormatting, int maxCells)
      throws McpException {
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    RangeAddress addr = RangeAddress.parse(rangeA1);
    int total = addr.cellCount();
    int rows = addr.rowCount();
    int cols = addr.colCount();
    boolean truncated = false;
    int effectiveRows = rows;
    if (total > maxCells) {
      truncated = true;
      // Row-major truncation: keep whole rows where possible.
      effectiveRows = Math.max(1, maxCells / Math.max(1, cols));
    }
    List<List<CellShape>> cells = new ArrayList<>(effectiveRows);
    for (int r = 0; r < effectiveRows; r++) {
      List<CellShape> rowOut = new ArrayList<>(cols);
      Row row = sheet.getRow(addr.startRow() + r);
      for (int c = 0; c < cols; c++) {
        int row0 = addr.startRow() + r;
        int col0 = addr.startCol() + c;
        rowOut.add(
            PoiCellReader.read(
                row == null ? null : row.getCell(col0), row0, col0, includeFormatting));
      }
      cells.add(rowOut);
    }
    if (truncated) {
      return new RangeShape(sheetName, addr.toA1(), effectiveRows, cols, cells, true, total);
    }
    return RangeShape.of(sheetName, addr.toA1(), cells);
  }

  @Override
  public int writeRange(
      HandleId id,
      String sheetName,
      int startRow,
      int startCol,
      List<List<Object>> values,
      List<List<String>> formulas)
      throws McpException {
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    int written = 0;
    for (int r = 0; r < values.size(); r++) {
      List<Object> row = values.get(r);
      List<String> fRow = formulas == null || r >= formulas.size() ? null : formulas.get(r);
      Row poiRow = sheet.getRow(startRow + r);
      if (poiRow == null) {
        poiRow = sheet.createRow(startRow + r);
      }
      for (int c = 0; c < row.size(); c++) {
        int col = startCol + c;
        Cell cell = poiRow.getCell(col);
        if (cell == null) {
          cell = poiRow.createCell(col);
        }
        String formula = fRow == null || c >= fRow.size() ? null : fRow.get(c);
        Object value = row.get(c);
        if (formula == null && value == null) {
          continue; // sparse: nothing to write
        }
        PoiCellWriter.write(cell, value, formula);
        written++;
      }
    }
    return written;
  }

  @Override
  public int clearRange(HandleId id, String sheetName, String rangeA1) throws McpException {
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    RangeAddress addr = RangeAddress.parse(rangeA1);
    int cleared = 0;
    for (int r = addr.startRow(); r <= addr.endRow(); r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;
      for (int c = addr.startCol(); c <= addr.endCol(); c++) {
        Cell cell = row.getCell(c);
        if (cell != null) {
          row.removeCell(cell);
          cleared++;
        }
      }
    }
    return cleared;
  }

  @Override
  public void close() {
    for (Workbook wb : workbooks.values()) {
      safeClose(wb);
    }
    workbooks.clear();
  }

  private Workbook requireOpen(HandleId id) throws McpException {
    Workbook wb = workbooks.get(id);
    if (wb == null) {
      // Either the handle was never registered, or it was closed. Prefer the specific code.
      if (registry.lookup(id).isEmpty()) {
        throw new McpException(
            ErrorCode.HANDLE_UNKNOWN, "handle not found: " + id, Map.of("handle", id.value()));
      }
      throw new McpException(
          ErrorCode.HANDLE_CLOSED, "handle is closed: " + id, Map.of("handle", id.value()));
    }
    return wb;
  }

  private Sheet requireSheet(Workbook wb, String name) throws McpException {
    Sheet sheet = wb.getSheet(name);
    if (sheet != null) {
      return sheet;
    }
    // Case-insensitive fallback — agents routinely get case wrong. PLAN-DEFER: accept
    // case-insensitive.
    String lc = name.toLowerCase(Locale.ROOT);
    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
      if (wb.getSheetName(i).toLowerCase(Locale.ROOT).equals(lc)) {
        return wb.getSheetAt(i);
      }
    }
    throw new McpException(
        ErrorCode.SHEET_NOT_FOUND, "sheet not found: " + name, Map.of("sheet", name));
  }

  private static void safeClose(Workbook wb) {
    try {
      wb.close();
    } catch (IOException ignored) {
      // best effort
    }
  }
}
