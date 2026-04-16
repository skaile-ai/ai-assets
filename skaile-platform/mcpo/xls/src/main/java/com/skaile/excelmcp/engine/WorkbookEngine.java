package com.skaile.excelmcp.engine;

import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.handles.OpenWorkbook;
import com.skaile.excelmcp.shape.NamedRangeRef;
import com.skaile.excelmcp.shape.RangeShape;
import com.skaile.excelmcp.shape.SheetShape;
import com.skaile.excelmcp.shape.TableRef;
import com.skaile.excelmcp.shape.WorkbookMetadataShape;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

  @Override
  void close();
}
