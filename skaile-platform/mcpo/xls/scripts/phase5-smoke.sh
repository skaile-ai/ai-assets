#!/usr/bin/env bash
# Phase 5 verify: workbook.recalculate refreshes cached formula values; workbook.capabilities_report
# detects post-2019 functions (FILTER) and flags them under unsupported_functions_used.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
BASIC="$TMP/basic.xlsx"
FILTERED="$TMP/filtered.xlsx"

# Basic fixture — a plain sheet with a few blank cells the agent will fill via range.set.
cat > "$TMP/MakeBasic.java" <<'EOF'
import java.nio.file.*; import java.io.*;
import org.apache.poi.xssf.usermodel.*;
public class MakeBasic {
  public static void main(String[] a) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(Path.of(a[0]))) {
      wb.createSheet("Data");
      wb.write(os);
    }
  }
}
EOF

# Filtered fixture — embeds a FILTER(...) formula that POI's evaluator does not implement.
# We deliberately bypass cell.setCellFormula's supported-function gate by writing the _xlfn.FILTER
# prefix directly via the XML-level API. capabilities_report should detect it.
cat > "$TMP/MakeFiltered.java" <<'EOF'
import java.nio.file.*; import java.io.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
public class MakeFiltered {
  public static void main(String[] a) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(Path.of(a[0]))) {
      XSSFSheet s = wb.createSheet("Sheet1");
      // Seed A1:A3 so FILTER has an argument.
      s.createRow(0).createCell(0).setCellValue(1);
      s.createRow(1).createCell(0).setCellValue(2);
      s.createRow(2).createCell(0).setCellValue(3);
      // D2 := =_xlfn._xlws.FILTER(A1:A3, A1:A3>1)
      XSSFRow r = s.createRow(3);
      XSSFCell d2 = r.createCell(3);
      d2.setCellFormula("_xlfn._xlws.FILTER(A1:A3,A1:A3>1)");
      wb.write(os);
    }
  }
}
EOF

( cd "$TMP" && javac -cp "$JAR" MakeBasic.java MakeFiltered.java \
    && java -cp "$JAR:$TMP" MakeBasic "$BASIC" \
    && java -cp "$JAR:$TMP" MakeFiltered "$FILTERED" )

python3 - "$JAR" "$BASIC" "$FILTERED" <<'PY'
import json, subprocess, sys
jar, basic, filtered = sys.argv[1], sys.argv[2], sys.argv[3]

def start():
    return subprocess.Popen(["java", "-jar", jar],
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

def send(p, obj):
    p.stdin.write((json.dumps(obj) + "\n").encode()); p.stdin.flush()

def recv(p):
    line = p.stdout.readline()
    if not line:
        err = p.stderr.read(2048).decode("utf-8", "replace")
        raise EOFError("server closed stdout; stderr=" + err)
    return json.loads(line.decode())

def handshake(p):
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"p5","version":"0.0.1"}}})
    init = recv(p); assert init.get("id") == 1, init
    send(p, {"jsonrpc":"2.0","method":"notifications/initialized"})

def call(p, i, name, args):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    if r["result"].get("isError"):
        raise AssertionError(f"tool error id={i}: {body}")
    return body

# --- Part 1: write =SUM(A1:A3), recalc, confirm cached value matches the sum ---
p = start()
try:
    handshake(p)
    opened = call(p, 10, "workbook.open", {"path": basic})
    h = opened["handle"]
    call(p, 11, "range.set", {"handle": h, "sheet": "Data", "start": "A1",
                              "values": [[1], [2], [3]]})
    call(p, 12, "range.set", {"handle": h, "sheet": "Data", "start": "A4",
                              "values": [[None]], "formulas": [["=SUM(A1:A3)"]]})
    # Before recalc, A4 is a formula with no cached value.
    pre = call(p, 13, "range.get", {"handle": h, "sheet": "Data", "range": "A4"})
    pre_cell = pre["cells"][0][0]
    assert pre_cell["formula"] == "SUM(A1:A3)", pre_cell
    assert pre_cell["type"] in ("formula_uncomputed", "number"), pre_cell

    ev = call(p, 14, "workbook.recalculate", {"handle": h})
    assert ev["evaluated_cells"] >= 1, ev

    post = call(p, 15, "range.get", {"handle": h, "sheet": "Data", "range": "A4"})
    post_cell = post["cells"][0][0]
    assert post_cell["type"] == "number", post_cell
    assert post_cell["value"] == 6, post_cell
    assert post_cell["formula"] == "SUM(A1:A3)", post_cell
    call(p, 16, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()

# --- Part 2: capabilities_report flags FILTER as unsupported ---
p = start()
try:
    handshake(p)
    opened = call(p, 20, "workbook.open", {"path": filtered})
    h = opened["handle"]
    report = call(p, 21, "workbook.capabilities_report", {"handle": h})
    assert report["poi_version"].startswith("5.5"), report
    assert report["supported_function_count"] > 100, report
    funcs = [u["name"] for u in report["unsupported_functions_used"]]
    assert "FILTER" in funcs, report
    filter_entry = next(u for u in report["unsupported_functions_used"] if u["name"] == "FILTER")
    assert filter_entry["count"] >= 1, filter_entry
    assert any("Sheet1!D4" in s for s in filter_entry["sample_cells"]), filter_entry
    warnings_str = " ".join(report["warnings"])
    assert "FILTER" in warnings_str, report
    # has_vba should be false on a plain .xlsx
    assert report["has_vba"] is False, report
    call(p, 22, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()

print("Phase 5 smoke OK: recalc updated cached SUM result; capabilities_report flagged FILTER.")
PY
