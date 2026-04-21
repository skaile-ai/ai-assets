#!/usr/bin/env bash
# Regression: deleting the last remaining sheet must fail with SHEET_LAST_REMAINING.
#
# Prior to review fix-up Batch B2, sheet.delete on the only remaining sheet would silently
# succeed and the next workbook.save would produce a zero-sheet workbook that Excel refuses
# to open. The tool now pre-checks getNumberOfSheets() == 1 and rejects the call up front.
set -euo pipefail

export EXCEL_MCP_ALLOW_UNSANDBOXED=true

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

python3 - "$JAR" <<'PY'
import json, subprocess, sys
jar = sys.argv[1]

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

def call_expect_error(p, i, name, args, expected_code):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    assert r["result"].get("isError"), f"expected error but got success: {body}"
    assert body.get("code") == expected_code, f"expected {expected_code}, got {body}"
    return body

p = start()
try:
    handshake(p)
    opened = call(p, 10, "workbook.create", {})
    h = opened["handle"]
    sheets = call(p, 11, "workbook.list_sheets", {"handle": h})
    assert [s["name"] for s in sheets["sheets"]] == ["Sheet1"], sheets

    # Deleting the only sheet must fail with SHEET_LAST_REMAINING.
    call_expect_error(p, 12, "sheet.delete", {"handle": h, "name": "Sheet1"}, "SHEET_LAST_REMAINING")

    # And the workbook still has its sheet.
    sheets = call(p, 13, "workbook.list_sheets", {"handle": h})
    assert [s["name"] for s in sheets["sheets"]] == ["Sheet1"], sheets

    # Adding a second sheet then deleting the original works as before.
    call(p, 14, "sheet.create", {"handle": h, "name": "Other"})
    call(p, 15, "sheet.delete", {"handle": h, "name": "Sheet1"})
    sheets = call(p, 16, "workbook.list_sheets", {"handle": h})
    assert [s["name"] for s in sheets["sheets"]] == ["Other"], sheets

    # And now deleting the only remaining sheet fails again.
    call_expect_error(p, 17, "sheet.delete", {"handle": h, "name": "Other"}, "SHEET_LAST_REMAINING")

    call(p, 18, "workbook.close", {"handle": h})
    print("OK")
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()
PY
