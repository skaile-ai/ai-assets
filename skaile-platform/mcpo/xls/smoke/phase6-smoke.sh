#!/usr/bin/env bash
# Phase 6 verify: sheet.create/delete/rename/merged_regions round-trip.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
MERGED="$TMP/merged.xlsx"

# Fixture with a merged region so sheet.merged_regions has something to return.
cat > "$TMP/MakeMerged.java" <<'EOF'
import java.nio.file.*; import java.io.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
public class MakeMerged {
  public static void main(String[] a) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream os = Files.newOutputStream(Path.of(a[0]))) {
      Sheet s = wb.createSheet("Data");
      s.createRow(0).createCell(0).setCellValue("Merged");
      s.addMergedRegion(new CellRangeAddress(0, 0, 0, 2)); // A1:C1
      s.addMergedRegion(new CellRangeAddress(2, 3, 1, 1)); // B3:B4
      wb.write(os);
    }
  }
}
EOF
( cd "$TMP" && javac -cp "$JAR" MakeMerged.java && java -cp "$JAR:$TMP" MakeMerged "$MERGED" )

python3 - "$JAR" "$MERGED" <<'PY'
import json, subprocess, sys
jar, merged = sys.argv[1], sys.argv[2]

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
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"p6","version":"0.0.1"}}})
    init = recv(p); assert init.get("id") == 1, init
    send(p, {"jsonrpc":"2.0","method":"notifications/initialized"})

def call(p, i, name, args):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    if r["result"].get("isError"):
        raise AssertionError(f"tool error id={i}: {body}")
    return body

def call_expect_error(p, i, name, args, expected_code):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    assert r["result"].get("isError"), f"expected error but got success: {body}"
    assert body.get("code") == expected_code, f"expected {expected_code}, got {body}"
    return body

# --- Part 1: create/rename/delete on a brand-new workbook ---
p = start()
try:
    handshake(p)
    opened = call(p, 10, "workbook.create", {})
    h = opened["handle"]

    sheets = call(p, 11, "workbook.list_sheets", {"handle": h})
    assert [s["name"] for s in sheets["sheets"]] == ["Sheet1"], sheets

    summary = call(p, 12, "sheet.create", {"handle": h, "name": "Summary"})
    assert summary["name"] == "Summary" and summary["index"] == 1, summary

    # Insert-at-index: Detail ends up at position 0, pushing everything else right.
    detail = call(p, 13, "sheet.create", {"handle": h, "name": "Detail", "index": 0})
    assert detail["name"] == "Detail" and detail["index"] == 0, detail

    sheets = call(p, 14, "workbook.list_sheets", {"handle": h})
    names = [s["name"] for s in sheets["sheets"]]
    assert names == ["Detail", "Sheet1", "Summary"], sheets

    # Duplicate (case-insensitive) → SHEET_ALREADY_EXISTS
    call_expect_error(p, 15, "sheet.create", {"handle": h, "name": "summary"}, "SHEET_ALREADY_EXISTS")

    # Rename Sheet1 → Q1Data
    renamed = call(p, 16, "sheet.rename", {"handle": h, "old_name": "Sheet1", "new_name": "Q1Data"})
    assert renamed == {"old_name": "Sheet1", "new_name": "Q1Data"}, renamed

    # Delete Detail
    call(p, 17, "sheet.delete", {"handle": h, "name": "Detail"})
    sheets = call(p, 18, "workbook.list_sheets", {"handle": h})
    names = [s["name"] for s in sheets["sheets"]]
    assert names == ["Q1Data", "Summary"], sheets

    # Delete non-existent → SHEET_NOT_FOUND
    call_expect_error(p, 19, "sheet.delete", {"handle": h, "name": "Nope"}, "SHEET_NOT_FOUND")

    call(p, 20, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()

# --- Part 2: merged_regions returns the configured ranges ---
p = start()
try:
    handshake(p)
    opened = call(p, 30, "workbook.open", {"path": merged})
    h = opened["handle"]
    got = call(p, 31, "sheet.merged_regions", {"handle": h, "sheet": "Data"})
    ranges = sorted(r["range"] for r in got["merged_regions"])
    assert ranges == ["A1:C1", "B3:B4"], got
    # Case-insensitive sheet lookup still works
    got2 = call(p, 32, "sheet.merged_regions", {"handle": h, "sheet": "data"})
    assert sorted(r["range"] for r in got2["merged_regions"]) == ["A1:C1", "B3:B4"], got2

    # merged_regions on an unknown sheet → SHEET_NOT_FOUND
    call_expect_error(p, 33, "sheet.merged_regions", {"handle": h, "sheet": "NoSuch"}, "SHEET_NOT_FOUND")

    call(p, 34, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()

print("Phase 6 smoke OK: sheet.create/rename/delete + merged_regions verified.")
PY
