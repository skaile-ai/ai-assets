#!/usr/bin/env bash
# Regression: deleting the last remaining sheet must fail with SHEET_LAST_REMAINING.
#
# Prior to review fix-up Batch B2, sheet.delete on the only remaining sheet would silently
# succeed and the next workbook.save would produce a zero-sheet workbook that Excel refuses
# to open. The tool now pre-checks getNumberOfSheets() == 1 and rejects the call up front.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

python3 - "$JAR" <<'PY'
import os, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call, call_expect_error

jar = sys.argv[1]
p = start(jar)
try:
    handshake(p, "reg")
    opened = call(p, 10, "workbook.create", {})
    h = opened["handle"]
    sheets = call(p, 11, "workbook.list_sheets", {"handle": h})
    assert [s["name"] for s in sheets["sheets"]] == ["Sheet1"], sheets

    # Deleting the only sheet must fail with SHEET_LAST_REMAINING.
    call_expect_error(p, 12, "sheet.delete", {"handle": h, "name": "Sheet1"}, "SHEET_LAST_REMAINING")

    # And the workbook still has its sheet.
    sheets = call(p, 13, "workbook.list_sheets", {"handle": h})
    assert [s["name"] for s in sheets["sheets"]] == ["Sheet1"], sheets

    # Adding a second sheet then deleting the original works as before.
    call(p, 14, "sheet.create", {"handle": h, "name": "Other"})
    call(p, 15, "sheet.delete", {"handle": h, "name": "Sheet1"})
    sheets = call(p, 16, "workbook.list_sheets", {"handle": h})
    assert [s["name"] for s in sheets["sheets"]] == ["Other"], sheets

    # And now deleting the only remaining sheet fails again.
    call_expect_error(p, 17, "sheet.delete", {"handle": h, "name": "Other"}, "SHEET_LAST_REMAINING")

    call(p, 18, "workbook.close", {"handle": h})
    print("OK")
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()
PY
