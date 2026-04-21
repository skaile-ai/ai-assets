#!/usr/bin/env bash
# Phase 4 verify: open → range.set → save → reopen → range.get sees the written cells.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
FIXTURE="$TMP/blank.xlsx"

cat > "$TMP/MakeBlank.java" <<'EOF'
import java.nio.file.*; import java.io.*;
import org.apache.poi.xssf.usermodel.*; import org.apache.poi.ss.usermodel.*;
public class MakeBlank {
  public static void main(String[] a) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(Path.of(a[0]))) {
      wb.createSheet("Data");
      wb.write(os);
    }
  }
}
EOF
( cd "$TMP" && javac -cp "$JAR" MakeBlank.java && java -cp "$JAR:$TMP" MakeBlank "$FIXTURE" )

python3 - "$JAR" "$FIXTURE" <<'PY'
import os, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call

jar, fixture = sys.argv[1], sys.argv[2]

# --- Pass 1: open, write, save ---
p = start(jar)
try:
    handshake(p, "p4")
    opened = call(p, 10, "workbook.open", {"path": fixture})
    h1 = opened["handle"]
    w = call(p, 11, "range.set", {
        "handle": h1, "sheet": "Data", "start": "A1",
        "values": [["Name", "Q1"], ["Acme", 100]],
    })
    assert w["written_cells"] == 4, w
    saved = call(p, 12, "workbook.save", {"handle": h1})
    assert saved["saved_to"].endswith("blank.xlsx"), saved
    assert saved["size_bytes"] > 0, saved
    call(p, 13, "workbook.close", {"handle": h1})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()

# --- Pass 2: reopen in a fresh process, range.get should see the writes ---
p = start(jar)
try:
    handshake(p, "p4")
    reopened = call(p, 20, "workbook.open", {"path": fixture})
    h2 = reopened["handle"]
    got = call(p, 21, "range.get", {"handle": h2, "sheet": "Data", "range": "A1:B2"})
    cells = got["cells"]
    assert got["rows"] == 2 and got["cols"] == 2, got
    assert cells[0][0]["value"] == "Name" and cells[0][0]["type"] == "string", cells
    assert cells[0][1]["value"] == "Q1"  and cells[0][1]["type"] == "string", cells
    assert cells[1][0]["value"] == "Acme" and cells[1][0]["type"] == "string", cells
    assert cells[1][1]["value"] == 100    and cells[1][1]["type"] == "number", cells

    # range.clear should then remove them
    cleared = call(p, 22, "range.clear", {"handle": h2, "sheet": "Data", "range": "A1:B2"})
    assert cleared["cleared_cells"] == 4, cleared
    after = call(p, 23, "range.get", {"handle": h2, "sheet": "Data", "range": "A1:B2"})
    assert all(c["type"] == "blank" for row in after["cells"] for c in row), after
    call(p, 24, "workbook.close", {"handle": h2})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()

print("Phase 4 smoke OK: write → save → reopen → read → clear round-trip verified.")
PY
