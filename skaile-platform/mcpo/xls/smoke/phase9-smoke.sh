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
source "$(dirname "$0")/_common.sh"

FIXTURE="$ROOT/src/test/resources/fixtures/vba-hello.xlsm"
[[ -f "$FIXTURE" ]] || { echo "fixture not found: $FIXTURE" >&2; exit 2; }

python3 - "$JAR" "$FIXTURE" <<'PY'
import os, sys, tempfile
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, send, recv, handshake, call, call_expect_error

jar = sys.argv[1]
fixture = sys.argv[2]
tmp = tempfile.mkdtemp()

def list_tools(p, i=2):
    send(p, {"jsonrpc": "2.0", "id": i, "method": "tools/list"})
    r = recv(p)
    assert r.get("id") == i, r
    return {t["name"] for t in r["result"]["tools"]}

p = start(jar)
try:
    handshake(p, "p9")

    # --- Registration: both vba.* tools listed ---
    names = list_tools(p)
    assert "vba.list_modules" in names, names
    assert "vba.get_module" in names, names

    # --- VBA_NOT_PRESENT (tier A): fresh .xlsx with no source on disk ---
    fresh = call(p, 10, "workbook.create", {})
    h = fresh["handle"]
    call_expect_error(p, 11, "vba.list_modules", {"handle": h}, "VBA_NOT_PRESENT")
    call_expect_error(p, 12, "vba.get_module", {"handle": h, "name": "Module1"}, "VBA_NOT_PRESENT")
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
    call_expect_error(p, 34, "vba.list_modules", {"handle": h2}, "VBA_NOT_PRESENT")
    call_expect_error(p, 35, "vba.get_module", {"handle": h2, "name": "Module1"}, "VBA_NOT_PRESENT")
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
    call_expect_error(p, 24, "vba.get_module",
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
    except Exception: p.kill()
    for f in os.listdir(tmp):
        os.unlink(os.path.join(tmp, f))
    os.rmdir(tmp)

print("Phase 9 smoke OK: registration + VBA_NOT_PRESENT (both tiers) + full module list/get + transport-survives-error regression.")
PY
