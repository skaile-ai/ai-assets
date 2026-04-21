package com.skaile.excelmcp.engine;

import com.skaile.excelmcp.config.ServerConfig;
import com.skaile.excelmcp.engine.poi.PoiAtomicSaver;
import com.skaile.excelmcp.engine.poi.PoiCapabilityScanner;
import com.skaile.excelmcp.engine.poi.PoiCellReader;
import com.skaile.excelmcp.engine.poi.PoiCellWriter;
import com.skaile.excelmcp.engine.poi.PoiFormulaEvaluation;
import com.skaile.excelmcp.engine.poi.PoiSizeGuard;
import com.skaile.excelmcp.engine.poi.PoiVbaExtractor;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.HandleRegistry;
import com.skaile.excelmcp.handles.OpenWorkbook;
import com.skaile.excelmcp.path.FormatWhitelist;
import com.skaile.excelmcp.shape.CapabilitiesReportShape;
import com.skaile.excelmcp.shape.CellShape;
import com.skaile.excelmcp.shape.NamedRangeGetShape;
import com.skaile.excelmcp.shape.NamedRangeRef;
import com.skaile.excelmcp.shape.RangeAddress;
import com.skaile.excelmcp.shape.RangeShape;
import com.skaile.excelmcp.shape.SheetShape;
import com.skaile.excelmcp.shape.TableGetShape;
import com.skaile.excelmcp.shape.TableRef;
import com.skaile.excelmcp.shape.VbaModuleShape;
import com.skaile.excelmcp.shape.VbaModuleSourceShape;
import com.skaile.excelmcp.shape.WorkbookMetadataShape;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
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
  private final Map<HandleId, FormulaEvaluator> evaluators = new HashMap<>();

  public XssfInMemoryEngine(ServerConfig config, HandleRegistry registry) {
    this.config = config;
    this.registry = registry;
  }

  @Override
  public HandleId open(Path path) throws McpException {
    String format = FormatWhitelist.format(path);
    PoiSizeGuard.assertFileSize(path, config.maxFileBytes());
    Workbook wb;
    // Load via InputStream rather than File to disconnect POI from the on-disk file. When POI
    // holds a read/write OPCPackage handle on the source file, closing the workbook silently
    // flushes in-memory state back to disk — overwriting any external edits made between open
    // and close (data-loss bug fixed post-Phase-7). The file-based constructor with
    // readOnly=true would avoid the writeback but then blocks wb.write(OutputStream) via
    // OPCPackage.save(OutputStream) too, so save can't work. Loading from InputStream reads the
    // bytes into memory at open time; POI has no file to flush back to on close; PoiAtomicSaver
    // still saves via wb.write(OutputStream) + temp/rename. Trade-off: peak memory is roughly
    // 2x file size during load; acceptable within the 100 MB file cap.
    try (InputStream in = Files.newInputStream(path)) {
      wb = WorkbookFactory.create(in);
    } catch (java.nio.file.NoSuchFileException | java.io.FileNotFoundException ex) {
      // Race: PathValidator confirmed the file existed, but it was deleted between the check
      // and Files.newInputStream. Surface the same FILE_NOT_FOUND code rather than the generic
      // INTERNAL_ERROR fallback so the agent's retry-or-give-up logic stays simple.
      throw new McpException(
          ErrorCode.FILE_NOT_FOUND,
          "no such file: " + path.getFileName(),
          Map.of("path", path.getFileName().toString()),
          ex);
    } catch (IOException ex) {
      throw new McpException(
          ErrorCode.INTERNAL_ERROR,
          "POI could not open workbook: " + ex.getMessage(),
          Map.of("path", path.getFileName().toString(), "exception", ex.getClass().getName()),
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
    Optional<Instant> sourceMtime = readMtime(path);
    registry.register(new OpenWorkbook(id, path, format, Instant.now(), sourceMtime));
    return id;
  }

  /** Best-effort source-file mtime read. Returns empty if the stat fails for any reason. */
  private static Optional<Instant> readMtime(Path path) {
    try {
      return Optional.of(Files.getLastModifiedTime(path).toInstant());
    } catch (IOException ignored) {
      return Optional.empty();
    }
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
    registry.register(
        new OpenWorkbook(id, sourcePath.orElse(null), format, Instant.now(), Optional.empty()));
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
    evaluators.remove(id);
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
      String scope = n.getSheetIndex() < 0 ? "workbook" : "sheet";
      String sheet = null;
      String range = null;
      String refersTo = null;
      try {
        refersTo = n.getRefersToFormula();
      } catch (RuntimeException ignored) {
        // Some malformed names throw on refersTo access; leave sheet+range null.
      }
      if (refersTo != null && !refersTo.isBlank()) {
        try {
          AreaReference area = new AreaReference(refersTo, wb.getSpreadsheetVersion());
          sheet = area.getFirstCell().getSheetName();
          range = formatAreaA1(area);
        } catch (RuntimeException ignored) {
          // Non-rectangular reference (formula expression, 3D ref, bare A1 without sheet). Per
          // plan §7.3 the wire shape is {name, sheet, range, scope} with clean fields — there's
          // no slot for a raw refers-to expression, and callers that need it can open the file
          // in Excel. Leave sheet+range null so the entry still surfaces in the inventory.
        }
      }
      out.add(new NamedRangeRef(n.getNameName(), sheet, range, scope));
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
        // AreaReference.formatAsString() includes the sheet prefix and absolute markers
        // (e.g. "Sheet1!$A$1:$C$4"), but the wire shape keeps sheet + range separate per
        // plan §7.3. Use formatAreaA1 so the range field is a bare A1 body ("A1:C4") and the
        // sheet name comes through the dedicated field.
        String range;
        try {
          AreaReference area = t.getArea();
          range = area == null ? null : formatAreaA1(area);
        } catch (RuntimeException ignored) {
          range = null;
        }
        out.add(new TableRef(t.getName(), sheet.getSheetName(), range));
      }
    }
    return out;
  }

  @Override
  public TableGetShape readTable(HandleId id, String name, boolean includeFormatting, int maxCells)
      throws McpException {
    Workbook wb = requireOpen(id);
    if (!(wb instanceof XSSFWorkbook xwb)) {
      // HSSF (.xls) has no ListObject API — report the lookup miss rather than crashing on a cast.
      throw new McpException(
          ErrorCode.TABLE_NOT_FOUND,
          "tables are not available on .xls (HSSF) workbooks; open an .xlsx/.xlsm instead",
          Map.of("name", name));
    }
    XSSFTable target = null;
    String lc = name.toLowerCase(Locale.ROOT);
    for (int i = 0; i < xwb.getNumberOfSheets() && target == null; i++) {
      for (XSSFTable t : xwb.getSheetAt(i).getTables()) {
        if (t.getName() != null && t.getName().toLowerCase(Locale.ROOT).equals(lc)) {
          target = t;
          break;
        }
      }
    }
    if (target == null) {
      throw new McpException(
          ErrorCode.TABLE_NOT_FOUND, "table not found: " + name, Map.of("name", name));
    }
    AreaReference area;
    try {
      area = target.getArea();
    } catch (RuntimeException ex) {
      throw new McpException(
          ErrorCode.TABLE_NOT_FOUND,
          "table area could not be resolved: " + name + " (" + ex.getMessage() + ")",
          Map.of("name", name, "exception", ex.getClass().getSimpleName()),
          ex);
    }
    if (area == null) {
      throw new McpException(
          ErrorCode.TABLE_NOT_FOUND, "table has no resolvable area: " + name, Map.of("name", name));
    }
    String sheetName = target.getSheetName();
    String a1 = formatAreaA1(area);
    RangeShape rs = readRange(id, sheetName, a1, includeFormatting, maxCells);
    return TableGetShape.of(target.getName(), rs);
  }

  @Override
  public NamedRangeGetShape readNamedRange(
      HandleId id, String name, boolean includeFormatting, int maxCells) throws McpException {
    Workbook wb = requireOpen(id);
    Name defined = wb.getName(name);
    if (defined == null) {
      throw new McpException(
          ErrorCode.NAMED_RANGE_NOT_FOUND, "named range not found: " + name, Map.of("name", name));
    }
    String refersTo;
    try {
      refersTo = defined.getRefersToFormula();
    } catch (RuntimeException ex) {
      // PLAN-DEFER: named ranges whose refersTo expression POI can't stringify surface as
      // NAMED_RANGE_NOT_FOUND with a reason — the v1 error enum has no dedicated
      // NAMED_RANGE_INVALID code. Appended to excel-mcp-server-future-work.md.
      throw new McpException(
          ErrorCode.NAMED_RANGE_NOT_FOUND,
          "named range has an unreadable reference: " + name + " (" + ex.getMessage() + ")",
          Map.of("name", name, "exception", ex.getClass().getSimpleName()),
          ex);
    }
    if (refersTo == null || refersTo.isBlank()) {
      throw new McpException(
          ErrorCode.NAMED_RANGE_NOT_FOUND,
          "named range has an empty reference: " + name,
          Map.of("name", name));
    }
    AreaReference area;
    try {
      area = new AreaReference(refersTo, wb.getSpreadsheetVersion());
    } catch (RuntimeException ex) {
      // PLAN-DEFER: defined names that resolve to a formula expression (=SUM(A1:A10)), a
      // multi-sheet 3D reference, or anything more complex than a simple rectangular area are
      // surfaced as NAMED_RANGE_NOT_FOUND with the original refersTo in details. Full expression
      // evaluation is out of v1 scope. Appended to excel-mcp-server-future-work.md.
      throw new McpException(
          ErrorCode.NAMED_RANGE_NOT_FOUND,
          "named range does not resolve to a simple rectangular range: " + name + " -> " + refersTo,
          Map.of("name", name, "refers_to", refersTo, "exception", ex.getClass().getSimpleName()),
          ex);
    }
    String sheetName = area.getFirstCell().getSheetName();
    if (sheetName == null) {
      throw new McpException(
          ErrorCode.NAMED_RANGE_NOT_FOUND,
          "named range reference has no sheet qualifier: " + name + " -> " + refersTo,
          Map.of("name", name, "refers_to", refersTo));
    }
    String a1 = formatAreaA1(area);
    RangeShape rs = readRange(id, sheetName, a1, includeFormatting, maxCells);
    return NamedRangeGetShape.of(defined.getNameName(), rs);
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
    RangeAddress addr = clampToFormat(sheet, RangeAddress.parse(rangeA1));
    assertWithinFormatBounds(sheet, addr);
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
    int endRow = startRow + Math.max(0, values.size() - 1);
    int maxCols = values.stream().mapToInt(List::size).max().orElse(0);
    int endCol = startCol + Math.max(0, maxCols - 1);
    assertWithinFormatBounds(sheet, new RangeAddress(startRow, startCol, endRow, endCol));
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
  public int recalculate(HandleId id) throws McpException {
    Workbook wb = requireOpen(id);
    FormulaEvaluator evaluator =
        evaluators.computeIfAbsent(id, k -> PoiFormulaEvaluation.newEvaluator(wb));
    return PoiFormulaEvaluation.evaluateAll(wb, evaluator);
  }

  @Override
  public CapabilitiesReportShape capabilitiesReport(HandleId id) throws McpException {
    Workbook wb = requireOpen(id);
    return PoiCapabilityScanner.scan(wb);
  }

  @Override
  public SheetShape createSheet(HandleId id, String name, OptionalInt index) throws McpException {
    Workbook wb = requireOpen(id);
    assertSheetNameAvailable(wb, name);
    Sheet created;
    try {
      created = wb.createSheet(name);
    } catch (IllegalArgumentException ex) {
      // POI validates length (<=31), forbidden chars (: \ / ? * [ ]), leading/trailing apostrophes.
      // PLAN-DEFER: no dedicated SHEET_NAME_INVALID code in the v1 enum yet — surface as
      // INTERNAL_ERROR with POI's message until the enum is extended.
      throw new McpException(
          ErrorCode.INTERNAL_ERROR,
          "invalid sheet name: " + ex.getMessage(),
          Map.of("name", name, "exception", ex.getClass().getSimpleName()),
          ex);
    }
    int finalIndex = wb.getSheetIndex(created);
    if (index.isPresent()) {
      int target = Math.max(0, Math.min(index.getAsInt(), wb.getNumberOfSheets() - 1));
      wb.setSheetOrder(name, target);
      finalIndex = target;
    }
    return new SheetShape(name, finalIndex, wb.isSheetHidden(finalIndex));
  }

  @Override
  public void deleteSheet(HandleId id, String name) throws McpException {
    Workbook wb = requireOpen(id);
    int idx = resolveSheetIndex(wb, name);
    wb.removeSheetAt(idx);
  }

  @Override
  public void renameSheet(HandleId id, String oldName, String newName) throws McpException {
    Workbook wb = requireOpen(id);
    int idx = resolveSheetIndex(wb, oldName);
    String canonicalOld = wb.getSheetName(idx);
    if (!canonicalOld.equalsIgnoreCase(newName)) {
      assertSheetNameAvailable(wb, newName);
    }
    try {
      wb.setSheetName(idx, newName);
    } catch (IllegalArgumentException ex) {
      // Same rationale as createSheet's catch — invalid sheet name.
      throw new McpException(
          ErrorCode.INTERNAL_ERROR,
          "invalid sheet name: " + ex.getMessage(),
          Map.of("name", newName, "exception", ex.getClass().getSimpleName()),
          ex);
    }
  }

  @Override
  public void insertRows(HandleId id, String sheetName, int startRow1Based, int count)
      throws McpException {
    assertPositive(count, "count", ErrorCode.ROW_INDEX_INVALID);
    assertPositive(startRow1Based, "start_row", ErrorCode.ROW_INDEX_INVALID);
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    int startRow = startRow1Based - 1;
    int maxRow = wb.getSpreadsheetVersion().getLastRowIndex();
    if (startRow > maxRow) {
      throw rowIndexInvalid(
          startRow1Based, "start_row " + startRow1Based + " exceeds format bounds");
    }
    if (startRow + count - 1 > maxRow) {
      throw rowIndexInvalid(
          startRow1Based,
          "inserting " + count + " rows at " + startRow1Based + " overflows format bounds");
    }
    int lastRow = sheet.getLastRowNum();
    if (lastRow >= startRow) {
      sheet.shiftRows(startRow, lastRow, count);
    }
  }

  @Override
  public void deleteRows(HandleId id, String sheetName, int startRow1Based, int count)
      throws McpException {
    assertPositive(count, "count", ErrorCode.ROW_INDEX_INVALID);
    assertPositive(startRow1Based, "start_row", ErrorCode.ROW_INDEX_INVALID);
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    int startRow = startRow1Based - 1;
    int maxRow = wb.getSpreadsheetVersion().getLastRowIndex();
    if (startRow > maxRow) {
      throw rowIndexInvalid(
          startRow1Based, "start_row " + startRow1Based + " exceeds format bounds");
    }
    int lastRow = sheet.getLastRowNum();
    int endRowExcl = startRow + count;
    for (int r = startRow; r < endRowExcl && r <= lastRow; r++) {
      Row row = sheet.getRow(r);
      if (row != null) {
        sheet.removeRow(row);
      }
    }
    // Only shift if there are rows below the deleted range. Avoids the §9.4 footgun pattern of
    // asking POI to shift an empty range.
    if (endRowExcl <= lastRow) {
      sheet.shiftRows(endRowExcl, lastRow, -count);
    }
  }

  @Override
  public void insertCols(HandleId id, String sheetName, int startCol1Based, int count)
      throws McpException {
    assertPositive(count, "count", ErrorCode.COLUMN_INDEX_INVALID);
    assertPositive(startCol1Based, "start_col", ErrorCode.COLUMN_INDEX_INVALID);
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    int startCol = startCol1Based - 1;
    int maxCol = wb.getSpreadsheetVersion().getLastColumnIndex();
    if (startCol > maxCol) {
      throw colIndexInvalid(
          startCol1Based, "start_col " + startCol1Based + " exceeds format bounds");
    }
    if (startCol + count - 1 > maxCol) {
      throw colIndexInvalid(
          startCol1Based,
          "inserting " + count + " cols at " + startCol1Based + " overflows format bounds");
    }
    int lastCol = computeLastFilledColumn(sheet);
    // §9.4 footgun: XSSFSheet.shiftColumns throws "firstMovedIndex, lastMovedIndex out of order"
    // when asked to shift from a position that has nothing to shift. Only call shiftColumns when
    // the sheet has data at or to the right of startCol.
    if (lastCol >= startCol) {
      sheet.shiftColumns(startCol, lastCol, count);
    }
  }

  @Override
  public void deleteCols(HandleId id, String sheetName, int startCol1Based, int count)
      throws McpException {
    assertPositive(count, "count", ErrorCode.COLUMN_INDEX_INVALID);
    assertPositive(startCol1Based, "start_col", ErrorCode.COLUMN_INDEX_INVALID);
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    int startCol = startCol1Based - 1;
    int maxCol = wb.getSpreadsheetVersion().getLastColumnIndex();
    if (startCol > maxCol) {
      throw colIndexInvalid(
          startCol1Based, "start_col " + startCol1Based + " exceeds format bounds");
    }
    int endColExcl = startCol + count;
    for (Row row : sheet) {
      for (int c = startCol; c < endColExcl; c++) {
        Cell cell = row.getCell(c);
        if (cell != null) {
          row.removeCell(cell);
        }
      }
    }
    int lastCol = computeLastFilledColumn(sheet);
    // Same §9.4 footgun guard as insertCols: skip the shift when there's nothing to the right.
    if (endColExcl <= lastCol) {
      sheet.shiftColumns(endColExcl, lastCol, -count);
    }
  }

  @Override
  public List<String> mergedRegions(HandleId id, String sheetName) throws McpException {
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    List<CellRangeAddress> regions = sheet.getMergedRegions();
    List<String> out = new ArrayList<>(regions.size());
    for (CellRangeAddress r : regions) {
      out.add(r.formatAsString());
    }
    return out;
  }

  @Override
  public int clearRange(HandleId id, String sheetName, String rangeA1) throws McpException {
    Workbook wb = requireOpen(id);
    Sheet sheet = requireSheet(wb, sheetName);
    RangeAddress addr = clampToFormat(sheet, RangeAddress.parse(rangeA1));
    assertWithinFormatBounds(sheet, addr);
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
  public List<VbaModuleShape> listVbaModules(HandleId id) throws McpException {
    requireOpen(id);
    OpenWorkbook meta = registry.require(id);
    return PoiVbaExtractor.listModules(meta.sourcePath());
  }

  @Override
  public VbaModuleSourceShape getVbaModule(HandleId id, String name) throws McpException {
    requireOpen(id);
    OpenWorkbook meta = registry.require(id);
    return PoiVbaExtractor.getModule(meta.sourcePath(), name);
  }

  @Override
  public void close() {
    for (Workbook wb : workbooks.values()) {
      safeClose(wb);
    }
    workbooks.clear();
    evaluators.clear();
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

  private int resolveSheetIndex(Workbook wb, String name) throws McpException {
    int exact = wb.getSheetIndex(name);
    if (exact >= 0) {
      return exact;
    }
    String lc = name.toLowerCase(Locale.ROOT);
    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
      if (wb.getSheetName(i).toLowerCase(Locale.ROOT).equals(lc)) {
        return i;
      }
    }
    throw new McpException(
        ErrorCode.SHEET_NOT_FOUND, "sheet not found: " + name, Map.of("sheet", name));
  }

  private static void assertSheetNameAvailable(Workbook wb, String name) throws McpException {
    String lc = name.toLowerCase(Locale.ROOT);
    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
      if (wb.getSheetName(i).toLowerCase(Locale.ROOT).equals(lc)) {
        throw new McpException(
            ErrorCode.SHEET_ALREADY_EXISTS,
            "sheet already exists (case-insensitive): " + name,
            Map.of("name", name, "existing", wb.getSheetName(i)));
      }
    }
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

  /**
   * Reject ranges that exceed the workbook's hard format limits — xlsx/xlsm is capped at column XFD
   * × 1,048,576 rows, xls at IV × 65,536. Data-extent bounds (rejecting a read that asks for rows
   * past the last populated row) is intentionally NOT enforced in v1 — it's noisy for legitimate
   * "show me A1:Z100 on this small sheet" calls and interacts poorly with cleared cells. See
   * excel-mcp-server-future-work.md for the data-extent bound deferral.
   */
  /**
   * Clamp full-column / full-row sentinels (encoded by {@link RangeAddress#parse} as EXCEL2007's
   * last index) to the actual workbook format's limit. xlsx is identity; .xls binds full-column
   * reads to ~64K rows instead of failing the bounds check below. Explicit out-of-bound inputs that
   * happen to land exactly on the EXCEL2007 sentinel get silently clamped on .xls too — acceptable
   * for v1, since the agent's intent there is unambiguous.
   */
  private static RangeAddress clampToFormat(Sheet sheet, RangeAddress addr) {
    SpreadsheetVersion v = sheet.getWorkbook().getSpreadsheetVersion();
    int maxRow = v.getLastRowIndex();
    int maxCol = v.getLastColumnIndex();
    int xlsxMaxRow = SpreadsheetVersion.EXCEL2007.getLastRowIndex();
    int xlsxMaxCol = SpreadsheetVersion.EXCEL2007.getLastColumnIndex();
    int endRow = addr.endRow();
    int endCol = addr.endCol();
    if (endRow == xlsxMaxRow && endRow > maxRow) {
      endRow = maxRow;
    }
    if (endCol == xlsxMaxCol && endCol > maxCol) {
      endCol = maxCol;
    }
    if (endRow == addr.endRow() && endCol == addr.endCol()) {
      return addr;
    }
    return new RangeAddress(addr.startRow(), addr.startCol(), endRow, endCol);
  }

  private static void assertWithinFormatBounds(Sheet sheet, RangeAddress addr) throws McpException {
    SpreadsheetVersion v = sheet.getWorkbook().getSpreadsheetVersion();
    int maxRow = v.getLastRowIndex();
    int maxCol = v.getLastColumnIndex();
    if (addr.startRow() < 0 || addr.startCol() < 0) {
      throw new McpException(
          ErrorCode.RANGE_OUT_OF_BOUNDS,
          "Range " + addr.toA1() + " has negative start coordinate",
          Map.of("range", addr.toA1()));
    }
    if (addr.endRow() > maxRow || addr.endCol() > maxCol) {
      String maxColLetter = RangeAddress.colLetter(maxCol);
      throw new McpException(
          ErrorCode.RANGE_OUT_OF_BOUNDS,
          "Range "
              + addr.toA1()
              + " exceeds workbook format limits (max column "
              + maxColLetter
              + ", max row "
              + (maxRow + 1)
              + ")",
          Map.of(
              "range",
              addr.toA1(),
              "sheet",
              sheet.getSheetName(),
              "format_max_col",
              maxColLetter,
              "format_max_row",
              maxRow + 1));
    }
  }

  /**
   * Strip sheet prefix and absolute markers from a POI {@link AreaReference} so it can be handed to
   * {@link #readRange(HandleId, String, String, boolean, int)}, which expects a bare A1 range on a
   * known sheet. The sheet name is carried separately by callers.
   */
  private static String formatAreaA1(AreaReference area) {
    CellReference first = area.getFirstCell();
    CellReference last = area.getLastCell();
    String start = RangeAddress.colLetter(first.getCol()) + (first.getRow() + 1);
    if (first.getRow() == last.getRow() && first.getCol() == last.getCol()) {
      return start;
    }
    return start + ":" + RangeAddress.colLetter(last.getCol()) + (last.getRow() + 1);
  }

  private static int computeLastFilledColumn(Sheet sheet) {
    int max = -1;
    for (Row row : sheet) {
      short last = row.getLastCellNum();
      if (last > 0 && (last - 1) > max) {
        max = last - 1;
      }
    }
    return max;
  }

  private static void assertPositive(int value, String field, ErrorCode code) throws McpException {
    if (value < 1) {
      throw new McpException(
          code, field + " must be >= 1 (got " + value + ")", Map.of(field, value));
    }
  }

  private static McpException rowIndexInvalid(int startRow1Based, String message) {
    return new McpException(
        ErrorCode.ROW_INDEX_INVALID, message, Map.of("start_row", startRow1Based));
  }

  private static McpException colIndexInvalid(int startCol1Based, String message) {
    return new McpException(
        ErrorCode.COLUMN_INDEX_INVALID, message, Map.of("start_col", startCol1Based));
  }

  private static void safeClose(Workbook wb) {
    try {
      wb.close();
    } catch (IOException ignored) {
      // best effort
    }
  }
}
