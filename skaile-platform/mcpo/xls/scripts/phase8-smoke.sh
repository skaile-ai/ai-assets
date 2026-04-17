#!/usr/bin/env bash
# Phase 8 verify: table.list, table.get, named_range.list, named_range.get against a fixture
# containing a real ListObject table and workbook-/sheet-scoped defined names. The fixture is
# built on the fly with jshell + POI so the smoke test is self-contained and doesn't require a
# binary .xlsx under version control.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

FIXTURE="$TMP/phase8.xlsx"

# --- Build fixture with POI via jshell (faster than a one-off compile of a helper class) ---
jshell --class-path "$JAR" - <<JSHELL >/dev/null
import java.io.*;
import java.nio.file.*;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

String path = "$FIXTURE";
XSSFWorkbook wb = new XSSFWorkbook();
XSSFSheet data = wb.createSheet("Data");
String[] headers = {"Product", "Region", "Revenue"};
Row h = data.createRow(0);
for (int c = 0; c < headers.length; c++) h.createCell(c).setCellValue(headers[c]);
Object[][] rows = {
    {"Alpha", "EMEA", 1200.0},
    {"Beta",  "AMER",  850.0},
    {"Gamma", "APAC",  600.0}
};
for (int r = 0; r < rows.length; r++) {
    Row row = data.createRow(r + 1);
    row.createCell(0).setCellValue((String) rows[r][0]);
    row.createCell(1).setCellValue((String) rows[r][1]);
    row.createCell(2).setCellValue((Double) rows[r][2]);
}
Row tot = data.createRow(rows.length + 2);
tot.createCell(0).setCellValue("Total");
tot.createCell(2).setCellValue(2650.0);

// Extra sheet so named-range scope is visible.
wb.createSheet("Summary");

// XSSFTable across A1:C4 (headers + 3 data rows).
XSSFTable table = data.createTable(new AreaReference("A1:C4", SpreadsheetVersion.EXCEL2007));
table.setName("tblSales");
table.setDisplayName("tblSales");

// Workbook-scoped named range -> Data!C6 (the Total cell).
Name total = wb.createName();
total.setNameName("Total");
total.setRefersToFormula("Data!\$C\$6");

// Workbook-scoped named range covering the header row.
Name hdr = wb.createName();
hdr.setNameName("HeaderRow");
hdr.setRefersToFormula("Data!\$A\$1:\$C\$1");

// Sheet-scoped named range on the Summary sheet -> Summary!A1:B2.
Name sheetScoped = wb.createName();
sheetScoped.setNameName("SummaryBlock");
sheetScoped.setSheetIndex(wb.getSheetIndex("Summary"));
sheetScoped.setRefersToFormula("Summary!\$A\$1:\$B\$2");

try (OutputStream out = Files.newOutputStream(Paths.get(path))) { wb.write(out); }
wb.close();
/exit
JSHELL

[[ -f "$FIXTURE" ]] || { echo "fixture not built: $FIXTURE" >&2; exit 2; }

python3 - "$JAR" "$FIXTURE" <<'PY'
import json, subprocess, sys
jar = sys.argv[1]
fixture = sys.argv[2]

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
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"p8","version":"0.0.1"}}})
    init = recv(p); assert init.get("id") == 1, init
    send(p, {"jsonrpc":"2.0","method":"notifications/initialized"})

def call(p, i, name, args):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    if r["result"].get("isError"):
        raise AssertionError(f"tool error id={i}: {body}")
    return body

def call_err(p, i, name, args, expected_code):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    assert r["result"].get("isError"), f"expected error, got {body}"
    assert body.get("code") == expected_code, f"expected {expected_code}, got {body}"

p = start()
try:
    handshake(p)

    opened = call(p, 10, "workbook.open", {"path": fixture})
    h = opened["handle"]

    # --- table.list ---
    tables = call(p, 11, "table.list", {"handle": h})
    assert tables["tables"], tables
    t_entry = next(t for t in tables["tables"] if t["name"] == "tblSales")
    assert t_entry["sheet"] == "Data", t_entry
    # POI returns the range with absolute markers? We don't pin the exact string — just confirm
    # it starts with "A1" and ends with "C4".
    assert "A1" in t_entry["range"] and "C4" in t_entry["range"], t_entry

    # --- table.get (case-insensitive) ---
    t_body = call(p, 12, "table.get", {"handle": h, "name": "TBLSALES"})
    assert t_body["table_name"] == "tblSales", t_body
    assert t_body["sheet"] == "Data", t_body
    assert t_body["rows"] == 4 and t_body["cols"] == 3, t_body
    # Header row values.
    header_values = [c["value"] for c in t_body["cells"][0]]
    assert header_values == ["Product", "Region", "Revenue"], header_values
    # First data row.
    first_row = t_body["cells"][1]
    assert first_row[0]["value"] == "Alpha", first_row
    assert first_row[2]["value"] == 1200, first_row  # NUMBER integer-normalised

    # --- table.get error: unknown table ---
    call_err(p, 13, "table.get", {"handle": h, "name": "tblNope"}, "TABLE_NOT_FOUND")

    # --- named_range.list ---
    names = call(p, 14, "named_range.list", {"handle": h})
    names_by_name = {n["name"]: n for n in names["named_ranges"]}
    assert "Total" in names_by_name, names
    assert "HeaderRow" in names_by_name, names
    assert "SummaryBlock" in names_by_name, names
    assert names_by_name["Total"]["scope"] == "workbook", names_by_name["Total"]
    assert names_by_name["SummaryBlock"]["scope"] == "sheet", names_by_name["SummaryBlock"]
    assert names_by_name["SummaryBlock"]["sheet"] == "Summary", names_by_name["SummaryBlock"]

    # --- named_range.get: workbook-scoped single cell ---
    total = call(p, 15, "named_range.get", {"handle": h, "name": "Total"})
    assert total["named_range"] == "Total", total
    assert total["sheet"] == "Data", total
    assert total["rows"] == 1 and total["cols"] == 1, total
    assert total["cells"][0][0]["value"] == 2650, total

    # --- named_range.get: workbook-scoped range (header row) ---
    hdr = call(p, 16, "named_range.get", {"handle": h, "name": "HeaderRow"})
    assert hdr["rows"] == 1 and hdr["cols"] == 3, hdr
    assert [c["value"] for c in hdr["cells"][0]] == ["Product", "Region", "Revenue"], hdr

    # --- named_range.get: sheet-scoped 2x2 block (cells on Summary are blank; shape stays 2x2) ---
    blk = call(p, 17, "named_range.get", {"handle": h, "name": "SummaryBlock"})
    assert blk["sheet"] == "Summary", blk
    assert blk["rows"] == 2 and blk["cols"] == 2, blk
    assert all(c["type"] == "blank" for row in blk["cells"] for c in row), blk

    # --- named_range.get error: unknown name ---
    call_err(p, 18, "named_range.get", {"handle": h, "name": "DoesNotExist"}, "NAMED_RANGE_NOT_FOUND")

    call(p, 19, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()

print("Phase 8 smoke OK: table.list/get + named_range.list/get round-trip on fixture.")
PY
