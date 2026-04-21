#!/usr/bin/env bash
# Regression: vba.list_modules and vba.get_module surface
# source_disk_mtime_changed_since_open=true after the on-disk source has been touched
# between workbook.open and the call.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

FIXTURE_SRC="$ROOT/src/test/resources/fixtures/vba-hello.xlsm"
[[ -f "$FIXTURE_SRC" ]] || { echo "fixture not found: $FIXTURE_SRC" >&2; exit 2; }

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
FIXTURE="$TMP/vba.xlsm"
cp "$FIXTURE_SRC" "$FIXTURE"

python3 - "$JAR" "$FIXTURE" <<'PY'
import os, sys, time
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call

jar, fixture = sys.argv[1], sys.argv[2]
p = start(jar)
try:
    handshake(p, "reg")
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
    except Exception: p.kill()
PY
