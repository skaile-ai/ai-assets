package com.portfolex.excelmcp.error;

/**
 * The full v1 error code set (see §8.2 of the implementation plan). Add new codes by extending the
 * enum and documenting them in the README error table.
 */
public enum ErrorCode {
  // Path / format
  PATH_INVALID,
  PATH_OUTSIDE_ROOT,
  FORMAT_UNSUPPORTED,
  FILE_NOT_FOUND,

  // Size / resource limits
  WORKBOOK_TOO_LARGE,
  WORKBOOK_TOO_MANY_CELLS,

  // Handles
  HANDLE_UNKNOWN,
  HANDLE_CLOSED,

  // Worksheet / range
  SHEET_NOT_FOUND,
  SHEET_ALREADY_EXISTS,
  RANGE_INVALID,
  RANGE_OUT_OF_BOUNDS,
  ROW_INDEX_INVALID,
  COLUMN_INDEX_INVALID,

  // Formulas
  FORMULA_INVALID,
  FORMULA_EVAL_FAILED,

  // VBA
  VBA_NOT_PRESENT,
  VBA_MODULE_NOT_FOUND,

  // Tables / named ranges
  TABLE_NOT_FOUND,
  NAMED_RANGE_NOT_FOUND,

  // Save
  SAVE_FAILED,
  SAVE_REQUIRES_PATH,

  // Catch-all
  INTERNAL_ERROR
}
