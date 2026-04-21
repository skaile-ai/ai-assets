#!/usr/bin/env bash
# Regression: range.get on a date-formatted cell must emit a naive local timestamp
# (no trailing "Z", no timezone offset). Excel date serials are wall-clock values.
set -euo pipefail

export EXCEL_MCP_ALLOW_UNSANDBOXED=true

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

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
import json, re, subprocess, sys
jar, fixture = sys.argv[1], sys.argv[2]

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
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"reg","version":"0.0.1"}}})
    init = recv(p); assert init.get("id") == 1, init
    send(p, {"jsonrpc":"2.0","method":"notifications/initialized"})

def call(p, i, name, args):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    if r["result"].get("isError"):
        raise AssertionError(f"tool error id={i}: {body}")
    return body

p = start()
try:
    handshake(p)
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
    except subprocess.TimeoutExpired: p.kill()
PY
