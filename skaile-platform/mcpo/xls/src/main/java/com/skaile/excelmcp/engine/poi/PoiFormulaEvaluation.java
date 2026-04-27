package com.skaile.excelmcp.engine.poi;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.util.Map;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;

/**
 * POI {@link FormulaEvaluator} wrapper that walks every formula cell and refreshes its cached
 * result. §9.1 recalc contract.
 *
 * <p>{@link NotImplementedException} is tolerated per cell: POI only implements ~280 of Excel's
 * ~500+ functions (FILTER, LAMBDA, dynamic-array shaping, and friends are unimplemented). Failing
 * the whole recalc on a single unsupported function would make this tool unusable on modern
 * workbooks. Unsupported cells keep their existing cached values — {@code
 * workbook.capabilities_report} is the way to see what's being skipped. Any other exception is
 * surfaced as {@link ErrorCode#FORMULA_EVAL_FAILED} with the offending cell address.
 */
public final class PoiFormulaEvaluation {

  private PoiFormulaEvaluation() {}

  /** Build a configured evaluator for {@code wb}. Intended to be cached per workbook. */
  public static FormulaEvaluator newEvaluator(Workbook wb) {
    FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();
    ev.setIgnoreMissingWorkbooks(true);
    return ev;
  }

  /**
   * Evaluate every formula cell in the workbook. Returns the number of cells whose cache was
   * refreshed successfully (cells skipped due to {@link NotImplementedException} are not counted).
   */
  public static int evaluateAll(Workbook wb, FormulaEvaluator evaluator) throws McpException {
    // POI's FormulaEvaluator keeps an internal cell-address-keyed result cache that survives
    // sheet.shiftRows / sheet.shiftColumns and in-place setCellFormula rewrites; without this
    // invalidation, a recalc after any structural mutation returns the pre-mutation cached value
    // at the shifted address. wb.setForceFormulaRecalculation(true) is an Excel-side hint only.
    evaluator.clearAllCachedResultValues();
    wb.setForceFormulaRecalculation(true);
    int count = 0;
    for (Sheet sheet : wb) {
      for (Row row : sheet) {
        for (Cell cell : row) {
          if (cell.getCellType() != CellType.FORMULA) {
            continue;
          }
          try {
            evaluator.evaluateFormulaCell(cell);
            count++;
          } catch (NotImplementedException nie) {
            // Known POI gap — leave the cached value alone; capabilities_report surfaces it.
          } catch (RuntimeException ex) {
            String cellRef =
                sheet.getSheetName()
                    + "!"
                    + new CellReference(cell.getRowIndex(), cell.getColumnIndex())
                        .formatAsString(false);
            throw new McpException(
                ErrorCode.FORMULA_EVAL_FAILED,
                "formula evaluation failed at " + cellRef + ": " + ex.getMessage(),
                Map.of("cell", cellRef, "exception", ex.getClass().getSimpleName()),
                ex);
          }
        }
      }
    }
    return count;
  }
}
