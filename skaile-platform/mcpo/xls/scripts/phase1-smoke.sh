#!/usr/bin/env bash
# Phase 1 verify: initialize + tools/list over stdio MCP transport.
# The server does not self-exit on stdin EOF; we run it under `timeout` and
# parse the captured stdout.
set -uo pipefail

JAR="$(dirname "$0")/../target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

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
grep -qE '"tools"\s*:\s*\[\s*\]' "$OUT_FILE" || { echo "FAIL: tools array not empty"; exit 4; }
# Verify nothing log-shaped leaked to stdout (no "INFO"/"WARN"/"ERROR" markers).
if grep -qE '^\S+ (INFO|WARN|ERROR)' "$OUT_FILE"; then
  echo "FAIL: log output leaked to stdout"; exit 5
fi
echo "Phase 1 smoke OK: tools/list returned empty array; no log leakage on stdout."
