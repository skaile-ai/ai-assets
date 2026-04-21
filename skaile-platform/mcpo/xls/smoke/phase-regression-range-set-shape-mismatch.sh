#!/usr/bin/env bash
# Regression: range.set with mismatched values / formulas dimensions must fail with
# RANGE_INVALID instead of silently truncating the smaller array (plan §7.1).
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

def call_expect_error(p, i, name, args, expected_code):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    assert r["result"].get("isError"), f"expected error but got success: {body}"
    assert body.get("code") == expected_code, f"expected {expected_code}, got {body}"

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
    opened = call(p, 10, "workbook.create", {})
    h = opened["handle"]

    # Mismatched row count: 2 rows of values, 1 row of formulas → RANGE_INVALID.
    call_expect_error(p, 11, "range.set", {
        "handle": h, "sheet": "Sheet1", "start": "A1",
        "values": [[1, 2], [3, 4]],
        "formulas": [["=A1+1", None]]
    }, "RANGE_INVALID")

    # Mismatched col count on a single row → RANGE_INVALID.
    call_expect_error(p, 12, "range.set", {
        "handle": h, "sheet": "Sheet1", "start": "A1",
        "values": [[1, 2, 3]],
        "formulas": [["=A1+1", None]]
    }, "RANGE_INVALID")

    # Matching shapes still succeed.
    call(p, 13, "range.set", {
        "handle": h, "sheet": "Sheet1", "start": "A1",
        "values": [[1, 2], [3, 4]],
        "formulas": [["=A1+1", None], [None, "=B1*2"]]
    })

    call(p, 14, "workbook.close", {"handle": h})
    print("OK")
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()
PY
