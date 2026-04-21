#!/usr/bin/env bash
# Regression: vba.list_modules and vba.get_module surface
# source_disk_mtime_changed_since_open=true after the on-disk source has been touched
# between workbook.open and the call.
set -euo pipefail

export EXCEL_MCP_ALLOW_UNSANDBOXED=true

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

FIXTURE_SRC="$ROOT/src/test/resources/fixtures/vba-hello.xlsm"
[[ -f "$FIXTURE_SRC" ]] || { echo "fixture not found: $FIXTURE_SRC" >&2; exit 2; }

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
FIXTURE="$TMP/vba.xlsm"
cp "$FIXTURE_SRC" "$FIXTURE"

python3 - "$JAR" "$FIXTURE" <<'PY'
import json, os, subprocess, sys, time
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

    # Initial call: source on disk hasn't been touched, flag must be false.
    fresh = call(p, 11, "vba.list_modules", {"handle": h})
    assert fresh.get("source_disk_mtime_changed_since_open") is False, fresh
    assert any(m.get("name") for m in fresh.get("modules", [])), fresh

    # Touch the file so its mtime advances. Sleep ensures the new mtime is strictly greater
    # even on filesystems with second-resolution mtimes.
    time.sleep(1.1)
    now = time.time()
    os.utime(fixture, (now, now))

    stale = call(p, 12, "vba.list_modules", {"handle": h})
    assert stale.get("source_disk_mtime_changed_since_open") is True, stale

    # vba.get_module on the same name must report the flag too.
    name = fresh["modules"][0]["name"]
    got = call(p, 13, "vba.get_module", {"handle": h, "name": name})
    assert got.get("source_disk_mtime_changed_since_open") is True, got
    assert got.get("name") == name, got

    call(p, 14, "workbook.close", {"handle": h})
    print("OK")
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()
PY
