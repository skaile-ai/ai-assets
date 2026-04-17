#!/usr/bin/env bash
# Phase 1 verify (post-Phase-10 update): initialize + tools/list over stdio MCP transport.
#
# Originally this smoke only confirmed an empty registry (no tools yet). After Phase 10, the
# registry holds all 26 v1 tools, so we assert the tool count instead. The log-leak check is
# still the critical assertion: if Logback or System.out sends anything to stdout, the stdio
# transport corrupts and the agent disconnects with an opaque error. The server does not
# self-exit on stdin EOF; we run it under `timeout` and parse the captured stdout.
set -uo pipefail

JAR="$(dirname "$0")/../target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

EXPECTED_TOOL_COUNT=26

REQUESTS=$'{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"phase1-smoke","version":"0.0.1"}}}\n{"jsonrpc":"2.0","method":"notifications/initialized"}\n{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}\n'

OUT_FILE=/tmp/phase1-server.stdout
ERR_FILE=/tmp/phase1-server.stderr
: > "$OUT_FILE"; : > "$ERR_FILE"

# Feed requests, then keep stdin open long enough for the server to reply, then close.
( printf '%s' "$REQUESTS"; sleep 2 ) \
  | timeout --signal=TERM --kill-after=2 5s java -jar "$JAR" >"$OUT_FILE" 2>"$ERR_FILE"
rc=$?

echo "--- server stdout ---"
cat "$OUT_FILE"
echo "--- server stderr ---"
cat "$ERR_FILE"
echo "---------------------"
echo "exit=$rc (124=timeout TERM, 137=timeout KILL — both acceptable here)"

grep -q '"id":2' "$OUT_FILE" || { echo "FAIL: no tools/list response"; exit 3; }

# Count tool entries: each tool has a "name" field inside the "tools" array. Using python3 keeps
# us robust against JSON reordering across SDK versions.
python3 - "$OUT_FILE" "$EXPECTED_TOOL_COUNT" <<'PY'
import json, sys
out_file, expected = sys.argv[1], int(sys.argv[2])
with open(out_file, "r") as f:
    for line in f:
        if '"id":2' in line or '"id": 2' in line:
            resp = json.loads(line)
            tools = resp.get("result", {}).get("tools", [])
            names = [t.get("name") for t in tools]
            if len(tools) != expected:
                print(f"FAIL: expected {expected} tools, got {len(tools)}: {names}")
                sys.exit(4)
            # Minimum-bar sanity: the Phase 10 registry addition is present.
            if "workbook.list_handles" not in names:
                print(f"FAIL: workbook.list_handles missing from tools/list: {names}")
                sys.exit(4)
            print(f"tools/list returned {len(tools)} tools including workbook.list_handles")
            sys.exit(0)
print("FAIL: no id:2 tools/list response parsed")
sys.exit(4)
PY
rc_py=$?
[[ $rc_py -eq 0 ]] || exit $rc_py

# Verify nothing log-shaped leaked to stdout (no "INFO"/"WARN"/"ERROR" markers). This is the
# assertion that actually matters in production — the tool-count check above is a bonus.
if grep -qE '^\S+ (INFO|WARN|ERROR)' "$OUT_FILE"; then
  echo "FAIL: log output leaked to stdout"; exit 5
fi
echo "Phase 1 smoke OK: tools/list returned $EXPECTED_TOOL_COUNT tools; no log leakage on stdout."
