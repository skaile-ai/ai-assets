package com.skaile.excelmcp.engine;

import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.OpenWorkbook;
import com.skaile.excelmcp.shape.CapabilitiesReportShape;
import com.skaile.excelmcp.shape.CellStyleSpec;
import com.skaile.excelmcp.shape.NamedRangeGetShape;
import com.skaile.excelmcp.shape.NamedRangeRef;
import com.skaile.excelmcp.shape.RangeShape;
import com.skaile.excelmcp.shape.SheetFormatSpec;
import com.skaile.excelmcp.shape.SheetShape;
import com.skaile.excelmcp.shape.TableGetShape;
import com.skaile.excelmcp.shape.TableRef;
import com.skaile.excelmcp.shape.VbaModuleShape;
import com.skaile.excelmcp.shape.VbaModuleSourceShape;
import com.skaile.excelmcp.shape.WorkbookMetadataShape;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Contract between {@code tools/} and POI. Tools never import POI directly; they go through this
 * seam. v1 ships a single implementation ({@code XssfInMemoryEngine}); a streaming or
 * LibreOffice-backed engine can slot in later as a sibling impl without rewriting tool code.
 *
 * <p>The interface grows as phases add tool coverage; it is deliberately kept narrow to what v1
 * actually needs.
 */
public interface WorkbookEngine extends AutoCloseable {

  /**
   * Load a workbook from disk and return a fresh handle. Performs size + format guards along the
   * way; on any failure the file is not kept open and no handle is registered.
   */
  HandleId open(Path path) throws McpException;

  /**
   * Create a new empty workbook. The optional source path is remembered for later {@code
   * workbook.save(handle)} with no explicit destination.
   */
  HandleId create(Optional<Path> sourcePath) throws McpException;

  /** Save to an explicit destination (or the source path if omitted). Returns the bytes written. */
  long save(HandleId id, Optional<Path> destination) throws McpException;

  /** Close a handle: POI workbook is released and the registry entry removed. */
  void close(HandleId id) throws McpException;

  /** Lookup the public-facing metadata record for a handle. */
  OpenWorkbook describe(HandleId id) throws McpException;

  /** Best-effort source-path peek for logging; returns null when unknown/closed. */
  Path peekSourcePath(HandleId id);

  /** List all sheets (name, index, hidden flag). */
  List<SheetShape> listSheets(HandleId id) throws McpException;

  /** List workbook-wide named ranges (and sheet-scoped ones). */
  List<NamedRangeRef> listNamedRanges(HandleId id) throws McpException;

  /** List tables ({@code ListObject}s) across every sheet. */
  List<TableRef> listTables(HandleId id) throws McpException;

  /**
   * Read the cell contents of the named table (ListObject), augmented with the resolved table name.
   * Delegates to {@link #readRange(HandleId, String, String, boolean, int)} after resolving the
   * table's area, so the same pagination, bounds, and cell-shape rules apply.
   */
  TableGetShape readTable(HandleId id, String name, boolean includeFormatting, int maxCells)
      throws McpException;

  /**
   * Read the cell contents of the named range (defined name), augmented with the resolved name.
   * Delegates to {@link #readRange(HandleId, String, String, boolean, int)} after resolving the
   * name's reference.
   */
  NamedRangeGetShape readNamedRange(
      HandleId id, String name, boolean includeFormatting, int maxCells) throws McpException;

  /** Aggregate workbook metadata for the {@code workbook.metadata} tool. */
  WorkbookMetadataShape describeMetadata(
      HandleId id, boolean includeNamedRanges, boolean includeTables) throws McpException;

  /**
   * Read a rectangular range. {@code maxCells} caps the number of cells returned; when the
   * requested range exceeds the cap, the returned shape has {@code truncated=true} and {@code
   * total_cells} set.
   */
  RangeShape readRange(
      HandleId id, String sheet, String rangeA1, boolean includeFormatting, int maxCells)
      throws McpException;

  /**
   * Write a 2D block of values (and optionally formulas) starting at {@code (startRow, startCol)}.
   * {@code formulas} may be {@code null}, or a same-shape 2D array where non-null entries override
   * the corresponding value with a formula. Returns the number of cells actually touched.
   *
   * <p>Setting a formula does not auto-recalc — callers must follow up with {@code
   * workbook.recalculate}.
   */
  int writeRange(
      HandleId id,
      String sheet,
      int startRow,
      int startCol,
      List<List<Object>> values,
      List<List<String>> formulas)
      throws McpException;

  /** Remove every non-null cell in the given range. Styling and merged regions are untouched. */
  int clearRange(HandleId id, String sheet, String rangeA1) throws McpException;

  /**
   * Apply a {@link CellStyleSpec} to every cell in the A1 range, merging onto each cell's existing
   * style so unspecified attributes are preserved. Empty cells in the range are materialised so the
   * style applies. XSSF (.xlsx/.xlsm) only — HSSF (.xls) fails with {@code STYLE_INVALID}. Returns
   * the number of cells styled. In-memory until {@code workbook.save}.
   */
  int applyStyle(HandleId id, String sheet, String rangeA1, CellStyleSpec spec) throws McpException;

  /**
   * Apply sheet-level presentation ({@link SheetFormatSpec}: column widths, row heights, freeze
   * pane, tab color). Only the supplied groups are changed. XSSF only. In-memory until {@code
   * workbook.save}.
   */
  void applySheetFormat(HandleId id, String sheet, SheetFormatSpec spec) throws McpException;

  /**
   * Walk every formula cell and refresh its cached result via POI's evaluator. Returns the number
   * of cells whose cache was successfully refreshed. Cells that hit a POI {@code
   * NotImplementedException} (FILTER, LAMBDA, etc.) are skipped without failure — use {@link
   * #capabilitiesReport(HandleId)} to see the coverage gaps ahead of time.
   */
  int recalculate(HandleId id) throws McpException;

  /**
   * Inventory of which Excel features the POI engine can and cannot recalculate in the loaded
   * workbook. Read-only — does not touch cell state.
   */
  CapabilitiesReportShape capabilitiesReport(HandleId id) throws McpException;

  /**
   * Add a new empty sheet. {@code index} (0-based) positions the sheet; empty = end of the
   * workbook. Returns the created sheet's {@link SheetShape} with the resolved final index.
   */
  SheetShape createSheet(HandleId id, String name, OptionalInt index) throws McpException;

  /** Remove the named sheet and all of its cell data. */
  void deleteSheet(HandleId id, String name) throws McpException;

  /** Rename a sheet in place; POI rewrites references to the old name in formulas. */
  void renameSheet(HandleId id, String oldName, String newName) throws McpException;

  /** Return the merged-cell regions on the given sheet as A1 range strings. */
  List<String> mergedRegions(HandleId id, String sheet) throws McpException;

  /**
   * Insert {@code count} empty rows at {@code startRow1Based}. Rows at or below that index are
   * pushed down by {@code count}. No-op (apart from index validation) when the sheet has no data at
   * or below the insertion point.
   */
  void insertRows(HandleId id, String sheet, int startRow1Based, int count) throws McpException;

  /**
   * Delete {@code count} rows starting at {@code startRow1Based}. Rows below are pulled up by
   * {@code count}.
   */
  void deleteRows(HandleId id, String sheet, int startRow1Based, int count) throws McpException;

  /**
   * Insert {@code count} empty columns at {@code startCol1Based} (A=1). Columns at or to the right
   * are pushed right by {@code count}. XSSF only — HSSF (.xls) does not implement column shifting
   * and the call will surface as INTERNAL_ERROR.
   */
  void insertCols(HandleId id, String sheet, int startCol1Based, int count) throws McpException;

  /**
   * Delete {@code count} columns starting at {@code startCol1Based}. Columns to the right are
   * pulled left by {@code count}. XSSF only.
   */
  void deleteCols(HandleId id, String sheet, int startCol1Based, int count) throws McpException;

  /**
   * List every VBA module (Document / Module / Class) in the workbook. Raises {@code
   * VBA_NOT_PRESENT} when the workbook has no macros or no source file on disk.
   */
  List<VbaModuleShape> listVbaModules(HandleId id) throws McpException;

  /**
   * Return the named VBA module's source text along with its type. Case-insensitive name lookup.
   * Raises {@code VBA_MODULE_NOT_FOUND} when no module matches.
   */
  VbaModuleSourceShape getVbaModule(HandleId id, String name) throws McpException;

  @Override
  void close();
}
