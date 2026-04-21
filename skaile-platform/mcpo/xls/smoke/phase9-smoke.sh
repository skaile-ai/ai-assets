#!/usr/bin/env bash
# Phase 9 verify: vba.list_modules + vba.get_module.
#
# Uses the committed fixture at src/test/resources/fixtures/vba-hello.xlsm. POI cannot author
# vbaProject.bin from scratch; the fixture was built once via BuildVbaFixture (jshell-style copy
# of an existing .xlsm's vbaProject.bin into a fresh XSSFWorkbook). See the fixtures README for
# provenance and regeneration notes.
#
# Two tiers:
#   A. VBA_NOT_PRESENT on a freshly-created workbook with no source file on disk, and on a plain
#      .xlsx (regression for the post-Phase-9 INTERNAL_ERROR bug).
#   B. Full list_modules / get_module round-trip against the committed .xlsm fixture, plus
#      VBA_MODULE_NOT_FOUND + transport-survives-error regressions.
set -euo pipefail

export EXCEL_MCP_ALLOW_UNSANDBOXED=true

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

FIXTURE="$ROOT/src/test/resources/fixtures/vba-hello.xlsm"
[[ -f "$FIXTURE" ]] || { echo "fixture not found: $FIXTURE" >&2; exit 2; }

python3 - "$JAR" "$FIXTURE" <<'PY'
import json, os, subprocess, sys, tempfile
jar = sys.argv[1]
fixture = sys.argv[2]
tmp = tempfile.mkdtemp()

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
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"p9","version":"0.0.1"}}})
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

def list_tools(p, i=2):
    send(p, {"jsonrpc":"2.0","id":i,"method":"tools/list"})
    r = recv(p); assert r.get("id") == i, r
    return {t["name"] for t in r["result"]["tools"]}

p = start()
try:
    handshake(p)

    # --- Registration: both vba.* tools listed ---
    names = list_tools(p)
    assert "vba.list_modules" in names, names
    assert "vba.get_module" in names, names

    # --- VBA_NOT_PRESENT (tier A): fresh .xlsx with no source on disk ---
    fresh = call(p, 10, "workbook.create", {})
    h = fresh["handle"]
    call_err(p, 11, "vba.list_modules", {"handle": h}, "VBA_NOT_PRESENT")
    call_err(p, 12, "vba.get_module", {"handle": h, "name": "Module1"}, "VBA_NOT_PRESENT")
    call(p, 13, "workbook.close", {"handle": h})

    # --- VBA_NOT_PRESENT (tier A, regression for Bug 3 found post-Phase-9): a plain .xlsx
    # saved to disk and reopened exercises the path where VBAMacroReader.readMacroModules()
    # actually executes (sourcePath is non-null, file exists, but contains no vbaProject.bin).
    # Before the fix, POI's IllegalArgumentException("No VBA project found") bubbled past the
    # IOException-only catch in PoiVbaExtractor and surfaced as INTERNAL_ERROR instead of the
    # documented VBA_NOT_PRESENT. Must return the documented code here.
    plain = os.path.join(tmp, "phase9-plain.xlsx")
    fresh2 = call(p, 30, "workbook.create", {})
    h2 = fresh2["handle"]
    call(p, 31, "workbook.save", {"handle": h2, "path": plain})
    call(p, 32, "workbook.close", {"handle": h2})
    opened_plain = call(p, 33, "workbook.open", {"path": plain})
    h2 = opened_plain["handle"]
    call_err(p, 34, "vba.list_modules", {"handle": h2}, "VBA_NOT_PRESENT")
    call_err(p, 35, "vba.get_module", {"handle": h2, "name": "Module1"}, "VBA_NOT_PRESENT")
    call(p, 36, "workbook.close", {"handle": h2})

    # --- Tier B: real .xlsm with VBA modules ---
    opened = call(p, 20, "workbook.open", {"path": fixture})
    h = opened["handle"]

    modules = call(p, 21, "vba.list_modules", {"handle": h})
    names = [m["name"] for m in modules["modules"]]
    types = {m["name"]: m["type"] for m in modules["modules"]}
    assert names, modules
    # Every module entry must have name + type (wire values).
    for m in modules["modules"]:
        assert isinstance(m["name"], str) and m["name"], m
        assert m["type"] in {"module", "class", "document"}, m

    # Pick one module — prefer a non-document one if present, else whatever is first.
    target = next((n for n, t in types.items() if t == "module"), names[0])
    src_body = call(p, 22, "vba.get_module", {"handle": h, "name": target})
    assert src_body["name"] == target, src_body
    assert src_body["type"] in {"module", "class", "document"}, src_body
    assert isinstance(src_body.get("source"), str), src_body
    assert len(src_body["source"]) > 0, src_body

    # Case-insensitive name lookup.
    src_ci = call(p, 23, "vba.get_module", {"handle": h, "name": target.upper()})
    assert src_ci["name"] == target, src_ci  # canonical casing preserved

    # Unknown module -> VBA_MODULE_NOT_FOUND.
    call_err(p, 24, "vba.get_module",
             {"handle": h, "name": "__does_not_exist__"}, "VBA_MODULE_NOT_FOUND")

    # Regression for Bug 2 (post-Phase-9 manual testing): a VBA_MODULE_NOT_FOUND response
    # must not kill the stdio transport. Before the fix, MCP Inspector's connection dropped
    # right after this error. Prove the transport survived by issuing a follow-up tools/list
    # request — if stdout got corrupted or the Java thread died, this would hang or EOF.
    survived = list_tools(p, i=241)
    assert "vba.get_module" in survived, survived

    call(p, 25, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()
    for f in os.listdir(tmp):
        os.unlink(os.path.join(tmp, f))
    os.rmdir(tmp)

print("Phase 9 smoke OK: registration + VBA_NOT_PRESENT (both tiers) + full module list/get + transport-survives-error regression.")
PY
