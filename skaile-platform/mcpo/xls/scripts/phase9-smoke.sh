#!/usr/bin/env bash
# Phase 9 verify: vba.list_modules + vba.get_module.
#
# The smoke test runs in two tiers:
#   1. Always: verifies VBA_NOT_PRESENT on a freshly-created workbook (no source file on disk).
#   2. Opportunistic: if PHASE9_VBA_SOURCE (or a known fallback path) resolves to an .xlsm with
#      readable VBA, build a minimal fixture by copying its vbaProject.bin into a fresh
#      XSSFWorkbook, then exercise the full list/get + error paths against the fixture.
#
# A permanent self-contained .xlsm fixture under src/test/resources/fixtures/ is tracked as
# future work — see excel-mcp-server-future-work.md ("Plan deferrals during implementation").
set -euo pipefail

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Resolve a source .xlsm with real VBA (optional). Tiers:
#   - $PHASE9_VBA_SOURCE if set
#   - known local dev paths (host-specific; silently skip if missing)
VBA_SOURCE="${PHASE9_VBA_SOURCE:-}"
if [[ -z "$VBA_SOURCE" ]]; then
  for candidate in \
    "/home/kolja/projects/portfolex/frontend/src/components/ui/spreadsheet/model.xlsm"; do
    if [[ -f "$candidate" ]]; then
      VBA_SOURCE="$candidate"
      break
    fi
  done
fi

FIXTURE=""
if [[ -n "$VBA_SOURCE" && -f "$VBA_SOURCE" ]]; then
  FIXTURE="$TMP/phase9-fixture.xlsm"
  jshell --class-path "$JAR" - <<JSHELL >/dev/null 2>&1 || FIXTURE=""
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import org.apache.poi.xssf.usermodel.*;
String src = "$VBA_SOURCE";
String dst = "$FIXTURE";
ZipFile zf = new ZipFile(src);
ZipEntry e = zf.getEntry("xl/vbaProject.bin");
if (e == null) { zf.close(); System.exit(3); }
byte[] vba;
try (InputStream is = zf.getInputStream(e); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
    is.transferTo(baos); vba = baos.toByteArray();
}
zf.close();
XSSFWorkbook wb = new XSSFWorkbook();
wb.createSheet("Sheet1");
try (ByteArrayInputStream in = new ByteArrayInputStream(vba)) { wb.setVBAProject(in); }
try (OutputStream os = Files.newOutputStream(Paths.get(dst))) { wb.write(os); }
wb.close();
/exit
JSHELL
  if [[ -n "$FIXTURE" && ! -f "$FIXTURE" ]]; then
    FIXTURE=""
  fi
fi

if [[ -n "$FIXTURE" ]]; then
  echo "Phase 9 fixture built from $VBA_SOURCE -> $FIXTURE"
else
  echo "Phase 9: no VBA source available; running VBA_NOT_PRESENT tier only"
fi

python3 - "$JAR" "$FIXTURE" <<'PY'
import json, os, subprocess, sys
jar = sys.argv[1]
fixture = sys.argv[2] or None

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

def list_tools(p):
    send(p, {"jsonrpc":"2.0","id":2,"method":"tools/list"})
    r = recv(p); assert r.get("id") == 2, r
    return {t["name"] for t in r["result"]["tools"]}

p = start()
try:
    handshake(p)

    # --- Registration: both vba.* tools listed ---
    names = list_tools(p)
    assert "vba.list_modules" in names, names
    assert "vba.get_module" in names, names

    # --- VBA_NOT_PRESENT: fresh .xlsx with no source on disk ---
    fresh = call(p, 10, "workbook.create", {})
    h = fresh["handle"]
    call_err(p, 11, "vba.list_modules", {"handle": h}, "VBA_NOT_PRESENT")
    call_err(p, 12, "vba.get_module", {"handle": h, "name": "Module1"}, "VBA_NOT_PRESENT")
    call(p, 13, "workbook.close", {"handle": h})

    if fixture:
        # --- Opportunistic: real .xlsm with actual VBA modules ---
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

        call(p, 25, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()

if fixture:
    print("Phase 9 smoke OK: registration + VBA_NOT_PRESENT + full module list/get against fixture.")
else:
    print("Phase 9 smoke OK (partial): registration + VBA_NOT_PRESENT only (no .xlsm fixture available).")
PY
