#!/usr/bin/env bash
# Phase 4 verify: open → range.set → save → reopen → range.get sees the written cells.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

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
import json, subprocess, sys
jar, fixture = sys.argv[1], sys.argv[2]

def start():
    return subprocess.Popen(["java", "-jar", jar],
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

def send(p, obj):
    p.stdin.write((json.dumps(obj) + "\n").encode()); p.stdin.flush()
def recv(p):
    line = p.stdout.readline()
    if not line: raise EOFError("server closed stdout; stderr=" + p.stderr.read(2048).decode("utf-8", "replace"))
    return json.loads(line.decode())

def handshake(p):
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"p4","version":"0.0.1"}}})
    init = recv(p); assert init.get("id") == 1, init
    send(p, {"jsonrpc":"2.0","method":"notifications/initialized"})

def call(p, i, name, args):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    if "error" in r:
        raise AssertionError(f"tool error id={i}: {r['error']}")
    body = json.loads(r["result"]["content"][0]["text"])
    if r["result"].get("isError"):
        raise AssertionError(f"tool error id={i}: {body}")
    return body

# --- Pass 1: open, write, save ---
p = start()
try:
    handshake(p)
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
    except subprocess.TimeoutExpired: p.kill()

# --- Pass 2: reopen in a fresh process, range.get should see the writes ---
p = start()
try:
    handshake(p)
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
    except subprocess.TimeoutExpired: p.kill()

print("Phase 4 smoke OK: write → save → reopen → read → clear round-trip verified.")
PY
