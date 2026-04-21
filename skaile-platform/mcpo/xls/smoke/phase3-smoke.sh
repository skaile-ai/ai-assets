#!/usr/bin/env bash
# Phase 3 verify: open → list_sheets → metadata → close over the MCP stdio transport.
set -euo pipefail

export EXCEL_MCP_ALLOW_UNSANDBOXED=true

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

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
import json, subprocess, sys
jar, fixture = sys.argv[1], sys.argv[2]
p = subprocess.Popen(["java", "-jar", jar], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

def send(obj):
    p.stdin.write((json.dumps(obj) + "\n").encode("utf-8"))
    p.stdin.flush()

def recv():
    line = p.stdout.readline()
    if not line:
        raise EOFError("server closed stdout")
    return json.loads(line.decode("utf-8"))

def call(call_id, name, args):
    send({"jsonrpc":"2.0","id":call_id,"method":"tools/call","params":{"name":name,"arguments":args}})
    resp = recv()
    assert resp.get("id") == call_id, resp
    return json.loads(resp["result"]["content"][0]["text"])

send({"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"phase3","version":"0.0.1"}}})
init_resp = recv()
assert init_resp.get("id") == 1 and "result" in init_resp, init_resp
send({"jsonrpc":"2.0","method":"notifications/initialized"})

opened = call(2, "workbook.open", {"path": fixture})
handle = opened["handle"]
assert opened["format"] == "xlsx" and opened["sheet_count"] == 2, opened

ls = call(3, "workbook.list_sheets", {"handle": handle})
names = [s["name"] for s in ls["sheets"]]
assert names == ["Sheet1", "Hidden"], ls
assert ls["sheets"][1]["is_hidden"] is True, ls

meta = call(4, "workbook.metadata", {"handle": handle})
assert meta["filename"].endswith("simple.xlsx") and meta["format"] == "xlsx", meta

closed = call(5, "workbook.close", {"handle": handle})
assert closed.get("closed") is True, closed

p.terminate()
try: p.wait(timeout=3)
except subprocess.TimeoutExpired: p.kill()

print("Phase 3 smoke OK.")
print(f"handle={handle} sheets={names} filename={meta['filename']}")
PY
