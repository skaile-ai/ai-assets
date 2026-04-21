#!/usr/bin/env bash
# Regression: workbook.recalculate after a structural mutation (here: sheet.insert_rows)
# must return FRESH formula results, not values cached by POI's FormulaEvaluator at the
# pre-shift addresses.
#
# Root cause (fixed in PoiFormulaEvaluation.evaluateAll): POI's FormulaEvaluator keeps an
# internal address-keyed result cache that survives shiftRows / shiftColumns and in-place
# setCellFormula rewrites; wb.setForceFormulaRecalculation(true) only affects Excel-side
# recalc on next open, not POI's own cache. Without evaluator.clearAllCachedResultValues()
# as the first line of evaluateAll, the shifted =SUM(...) cell serves the stale pre-shift
# sum.
#
# Reproduced sequence:
#   1. workbook.create → range.set A1:B3 with numeric values and =SUM(B1:B3) in B4
#   2. workbook.recalculate → assert B4 equals sum of B1:B3 (fresh eval)
#   3. sheet.insert_rows start_row=1 count=1 → header row inserted above; B4 shifts to B5,
#      formula text rewritten by POI to =SUM(B2:B4)
#   4. workbook.recalculate → assert B5 equals sum of the NEW B2:B4 (i.e. fresh eval),
#      NOT the pre-shift B1:B3 sum that the evaluator had cached at the old address.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

python3 - "$JAR" <<'PY'
import os, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call

jar = sys.argv[1]
p = start(jar)
try:
    handshake(p, "recalc-regression")

    created = call(p, 10, "workbook.create", {})
    h = created["handle"]

    # Grid:
    #   A1..B1 = "x", 1000
    #   A2..B2 = "y", 1300
    #   A3..B3 = "z", 1600
    #   B4     = =SUM(B1:B3)   → 3900
    call(p, 11, "range.set", {"handle": h, "sheet": "Sheet1", "start": "A1",
                              "values": [["x", 1000],
                                         ["y", 1300],
                                         ["z", 1600]]})
    call(p, 12, "range.set", {"handle": h, "sheet": "Sheet1", "start": "B4",
                              "values": [[None]], "formulas": [["=SUM(B1:B3)"]]})

    # --- Step 2: first recalc computes B4 from B1:B3. ---
    call(p, 13, "workbook.recalculate", {"handle": h})
    b4 = call(p, 14, "range.get", {"handle": h, "sheet": "Sheet1", "range": "B4"})
    assert b4["cells"][0][0]["value"] == 3900, f"pre-shift B4 wrong: {b4}"

    # --- Step 3: insert a header row at row 1. B4 shifts to B5; POI rewrites the formula
    # text to =SUM(B2:B4). The new B1 row is blank, so the new sum of B2:B4 equals the old
    # sum of B1:B3 — that would mask the bug. Put fresh values in the shifted range so the
    # correct recalc result is DIFFERENT from the pre-shift cached value.
    call(p, 15, "sheet.insert_rows", {"handle": h, "sheet": "Sheet1", "start_row": 1, "count": 1})

    # Overwrite the shifted numeric values (previously B2=1000, B3=1300, B4=1600 after the
    # shift) with new ones. Post-shift values: B2=1, B3=2, B4=3 → expected sum 6.
    call(p, 16, "range.set", {"handle": h, "sheet": "Sheet1", "start": "A1",
                              "values": [["header_a", "header_b"]]})
    call(p, 17, "range.set", {"handle": h, "sheet": "Sheet1", "start": "B2",
                              "values": [[1], [2], [3]]})

    # Sanity: POI rewrote the formula text to reference the new addresses.
    b5_pre_recalc = call(p, 18, "range.get", {"handle": h, "sheet": "Sheet1", "range": "B5"})
    assert b5_pre_recalc["cells"][0][0].get("formula") == "SUM(B2:B4)", \
        f"expected formula text rewritten to =SUM(B2:B4), got {b5_pre_recalc}"

    # --- Step 4: recalc MUST return the fresh sum (6), not the stale pre-shift value (3900). ---
    call(p, 19, "workbook.recalculate", {"handle": h})
    b5 = call(p, 20, "range.get", {"handle": h, "sheet": "Sheet1", "range": "B5"})
    got = b5["cells"][0][0]["value"]
    assert got == 6, (
        f"STALE EVALUATOR CACHE: post-shift B5 should be 6 (=SUM of new B2:B4 = 1+2+3), "
        f"got {got}. If this is 3900 the evaluator served the pre-shift cached sum at the "
        f"old B4 address; if anything else, investigate shift semantics.")

    call(p, 21, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()

print("Regression smoke OK: workbook.recalculate returns fresh values after sheet.insert_rows.")
PY
