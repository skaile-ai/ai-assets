#!/usr/bin/env bash
# Regression: range.set with mismatched values / formulas dimensions must fail with
# RANGE_INVALID instead of silently truncating the smaller array (plan §7.1).
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

    # Mismatched row count: 2 rows of values, 1 row of formulas → RANGE_INVALID.
    call_expect_error(p, 11, "range.set", {
        "handle": h, "sheet": "Sheet1", "start": "A1",
        "values": [[1, 2], [3, 4]],
        "formulas": [["=A1+1", None]]
    }, "RANGE_INVALID")

    # Mismatched col count on a single row → RANGE_INVALID.
    call_expect_error(p, 12, "range.set", {
        "handle": h, "sheet": "Sheet1", "start": "A1",
        "values": [[1, 2, 3]],
        "formulas": [["=A1+1", None]]
    }, "RANGE_INVALID")

    # Matching shapes still succeed.
    call(p, 13, "range.set", {
        "handle": h, "sheet": "Sheet1", "start": "A1",
        "values": [[1, 2], [3, 4]],
        "formulas": [["=A1+1", None], [None, "=B1*2"]]
    })

    call(p, 14, "workbook.close", {"handle": h})
    print("OK")
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()
PY
