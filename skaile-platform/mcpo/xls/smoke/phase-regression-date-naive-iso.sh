#!/usr/bin/env bash
# Regression: range.get on a date-formatted cell must emit a naive local timestamp
# (no trailing "Z", no timezone offset). Excel date serials are wall-clock values.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
FIXTURE="$TMP/dates.xlsx"

cat > "$TMP/MakeDated.java" <<'EOF'
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class MakeDated {
  public static void main(String[] args) throws Exception {
    Path out = Paths.get(args[0]);
    try (XSSFWorkbook wb = new XSSFWorkbook();
         OutputStream os = Files.newOutputStream(out)) {
      Sheet s = wb.createSheet("Sheet1");
      CellStyle ds = wb.createCellStyle();
      ds.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
      Calendar c = Calendar.getInstance();
      c.clear();
      c.set(2024, Calendar.MARCH, 5, 10, 0, 0);
      Cell cell = s.createRow(0).createCell(0);
      cell.setCellValue(c.getTime());
      cell.setCellStyle(ds);
      wb.write(os);
    }
  }
}
EOF

( cd "$TMP" && javac -cp "$JAR" MakeDated.java && java -cp "$JAR:$TMP" MakeDated "$FIXTURE" )

python3 - "$JAR" "$FIXTURE" <<'PY'
import os, re, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call

jar, fixture = sys.argv[1], sys.argv[2]
p = start(jar)
try:
    handshake(p, "reg")
    opened = call(p, 10, "workbook.open", {"path": fixture})
    h = opened["handle"]
    got = call(p, 11, "range.get", {"handle": h, "sheet": "Sheet1", "range": "A1"})
    cell = got["cells"][0][0]
    assert cell["type"] == "date", cell
    val = cell["value"]
    assert isinstance(val, str), cell
    assert not val.endswith("Z"), cell
    assert "+" not in val[10:] and "-" not in val[10:], cell  # no offset
    assert re.match(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$", val), cell
    print(f"OK ({val})")
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except Exception: p.kill()
PY
