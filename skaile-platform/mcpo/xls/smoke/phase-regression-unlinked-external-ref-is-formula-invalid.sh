#!/usr/bin/env bash
# Regression: range.set with a formula that references an unlinked external workbook, e.g.
# "=[NichtExistent.xlsx]Sheet1!A1", must surface as FORMULA_INVALID — NOT INTERNAL_ERROR.
#
# POI throws IllegalStateException ("Book not linked for filename Foo.xlsx") from
# setCellFormula in this case, which is a parser-stage rejection semantically equivalent to
# FormulaParseException. Plan §8.2: FORMULA_INVALID = "cannot be parsed by POI's formula
# parser". The prior narrow catch in PoiCellWriter missed this exception type and let it
# bubble up as INTERNAL_ERROR, misleading the agent about whether the error is recoverable.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

python3 - "$JAR" <<'PY'
import os, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call, call_expect_error

jar = sys.argv[1]
p = start(jar)
try:
    handshake(p, "unlinked-ref")
    h = call(p, 10, "workbook.create", {})["handle"]

    # Unlinked external reference — POI throws IllegalStateException from setCellFormula.
    body = call_expect_error(
        p, 11, "range.set",
        {"handle": h, "sheet": "Sheet1", "start": "A1",
         "values": [[None]],
         "formulas": [["=[NichtExistent.xlsx]Sheet1!A1"]]},
        "FORMULA_INVALID")
    assert "Book not linked" in body.get("message", ""), body
    assert body.get("details", {}).get("exception") == "IllegalStateException", body

    # And the regular syntax error path still maps to FORMULA_INVALID (FormulaParseException).
    call_expect_error(
        p, 12, "range.set",
        {"handle": h, "sheet": "Sheet1", "start": "A1",
         "values": [[None]],
         "formulas": [["=SUM("]]},  # truncated — syntactically invalid
        "FORMULA_INVALID")

    call(p, 13, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()

print("Regression smoke OK: unlinked external ref surfaces as FORMULA_INVALID, not INTERNAL_ERROR.")
PY
