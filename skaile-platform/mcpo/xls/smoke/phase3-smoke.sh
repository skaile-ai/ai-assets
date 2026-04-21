#!/usr/bin/env bash
# Phase 3 verify: open → list_sheets → metadata → close over the MCP stdio transport.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
FIXTURE="$TMP/simple.xlsx"

cat > "$TMP/MakeFixture.java" <<'EOF'
import java.nio.file.*; import java.io.*;
import org.apache.poi.xssf.usermodel.*; import org.apache.poi.ss.usermodel.*;
public class MakeFixture {
  public static void main(String[] a) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(Path.of(a[0]))) {
      Sheet s1 = wb.createSheet("Sheet1");
      s1.createRow(0).createCell(0).setCellValue("Name");
      s1.createRow(1).createCell(0).setCellValue("Acme");
      wb.createSheet("Hidden");
      wb.setSheetHidden(1, true);
      wb.write(os);
    }
  }
}
EOF
( cd "$TMP" && javac -cp "$JAR" MakeFixture.java && java -cp "$JAR:$TMP" MakeFixture "$FIXTURE" )

python3 - "$JAR" "$FIXTURE" <<'PY'
import os, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call

jar, fixture = sys.argv[1], sys.argv[2]
p = start(jar)
try:
    handshake(p, "phase3")

    opened = call(p, 2, "workbook.open", {"path": fixture})
    handle = opened["handle"]
    assert opened["format"] == "xlsx" and opened["sheet_count"] == 2, opened

    ls = call(p, 3, "workbook.list_sheets", {"handle": handle})
    names = [s["name"] for s in ls["sheets"]]
    assert names == ["Sheet1", "Hidden"], ls
    assert ls["sheets"][1]["is_hidden"] is True, ls

    meta = call(p, 4, "workbook.metadata", {"handle": handle})
    assert meta["filename"].endswith("simple.xlsx") and meta["format"] == "xlsx", meta

    closed = call(p, 5, "workbook.close", {"handle": handle})
    assert closed.get("closed") is True, closed

    print("Phase 3 smoke OK.")
    print(f"handle={handle} sheets={names} filename={meta['filename']}")
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()
PY
