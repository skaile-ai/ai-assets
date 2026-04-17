#!/usr/bin/env bash
# Regression: range.set with a formula that references an unlinked external workbook, e.g.
# "=[NichtExistent.xlsx]Sheet1!A1", must surface as FORMULA_INVALID — NOT INTERNAL_ERROR.
#
# POI throws IllegalStateException ("Book not linked for filename Foo.xlsx") from
# setCellFormula in this case, which is a parser-stage rejection semantically equivalent to
# FormulaParseException. Plan §8.2: FORMULA_INVALID = "cannot be parsed by POI's formula
# parser". The prior narrow catch in PoiCellWriter missed this exception type and let it
# bubble up as INTERNAL_ERROR, misleading the agent about whether the error is recoverable.
set -euo pipefail

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
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"unlinked-ref","version":"0.0.1"}}})
    init = recv(p); assert init.get("id") == 1, init
    send(p, {"jsonrpc":"2.0","method":"notifications/initialized"})

def call_err(p, i, name, args, expected_code):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":name,"arguments":args}})
    r = recv(p); assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    assert r["result"].get("isError"), f"expected error, got success: {body}"
    assert body.get("code") == expected_code, (
        f"expected {expected_code}, got {body}")
    return body

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
    h = call(p, 10, "workbook.create", {})["handle"]

    # Unlinked external reference — POI throws IllegalStateException from setCellFormula.
    body = call_err(
        p, 11, "range.set",
        {"handle": h, "sheet": "Sheet1", "start": "A1",
         "values": [[None]],
         "formulas": [["=[NichtExistent.xlsx]Sheet1!A1"]]},
        "FORMULA_INVALID")
    assert "Book not linked" in body.get("message", ""), body
    assert body.get("details", {}).get("exception") == "IllegalStateException", body

    # And the regular syntax error path still maps to FORMULA_INVALID (FormulaParseException).
    call_err(
        p, 12, "range.set",
        {"handle": h, "sheet": "Sheet1", "start": "A1",
         "values": [[None]],
         "formulas": [["=SUM("]]},  # truncated — syntactically invalid
        "FORMULA_INVALID")

    call(p, 13, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()

print("Regression smoke OK: unlinked external ref surfaces as FORMULA_INVALID, not INTERNAL_ERROR.")
PY
