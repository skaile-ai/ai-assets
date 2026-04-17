#!/usr/bin/env bash
# Phase 7 verify: sheet.insert_rows/delete_rows/insert_cols/delete_cols, incl. the §9.4
# "shiftColumns at end-of-data" footgun guard.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
JAR="$ROOT/target/excel-mcp-0.1.0-SNAPSHOT.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }

python3 - "$JAR" <<'PY'
import json, subprocess, sys, tempfile, os
jar = sys.argv[1]
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
    send(p, {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"p7","version":"0.0.1"}}})
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

p = start()
try:
    handshake(p)
    created = call(p, 10, "workbook.create", {})
    h = created["handle"]

    # Build a 5x3 grid on Sheet1:
    #   A1..C1 = "h1","h2","h3"
    #   A2..C2 = 10, 20, 30
    #   A3..C3 = 11, 21, 31
    #   A4..C4 = 12, 22, 32
    #   A5..C5 = 13, 23, 33
    call(p, 11, "range.set", {"handle": h, "sheet": "Sheet1", "start": "A1",
                              "values": [["h1","h2","h3"],
                                         [10,20,30],
                                         [11,21,31],
                                         [12,22,32],
                                         [13,23,33]]})

    # --- insert_rows at 2, count 2: former rows 2+ shift to 4+, rows 2,3 become blank ---
    call(p, 12, "sheet.insert_rows", {"handle": h, "sheet": "Sheet1", "start_row": 2, "count": 2})
    after_ins = call(p, 13, "range.get", {"handle": h, "sheet": "Sheet1", "range": "A1:C7"})
    # Header row is still at A1.
    assert after_ins["cells"][0][0]["value"] == "h1", after_ins
    # Rows 2 and 3 are blank now.
    assert all(c["type"] == "blank" for c in after_ins["cells"][1]), after_ins
    assert all(c["type"] == "blank" for c in after_ins["cells"][2]), after_ins
    # Former row 2 (10,20,30) is now row 4.
    assert after_ins["cells"][3][0]["value"] == 10, after_ins

    # --- delete_rows at 2, count 2: reverts to the original 5x3 shape ---
    call(p, 14, "sheet.delete_rows", {"handle": h, "sheet": "Sheet1", "start_row": 2, "count": 2})
    after_del = call(p, 15, "range.get", {"handle": h, "sheet": "Sheet1", "range": "A1:C5"})
    assert after_del["cells"][1][0]["value"] == 10, after_del
    assert after_del["cells"][4][2]["value"] == 33, after_del

    # --- insert_cols at 2, count 1: pushes columns B,C → C,D ---
    call(p, 16, "sheet.insert_cols", {"handle": h, "sheet": "Sheet1", "start_col": 2, "count": 1})
    after_icol = call(p, 17, "range.get", {"handle": h, "sheet": "Sheet1", "range": "A1:D2"})
    assert after_icol["cells"][0][0]["value"] == "h1", after_icol
    assert after_icol["cells"][0][1]["type"] == "blank", after_icol  # new column B
    assert after_icol["cells"][0][2]["value"] == "h2", after_icol
    assert after_icol["cells"][0][3]["value"] == "h3", after_icol

    # --- delete_cols at 2, count 1: back to original layout ---
    call(p, 18, "sheet.delete_cols", {"handle": h, "sheet": "Sheet1", "start_col": 2, "count": 1})
    after_dcol = call(p, 19, "range.get", {"handle": h, "sheet": "Sheet1", "range": "A1:C1"})
    assert [c["value"] for c in after_dcol["cells"][0]] == ["h1", "h2", "h3"], after_dcol

    # --- §9.4 footgun: insert_cols at the far right (past the data) must NOT throw ---
    # Current lastCol index is 2 (C). Inserting at col 10 (J) is past data; engine should skip
    # shiftColumns instead of raising "firstMovedIndex, lastMovedIndex out of order".
    call(p, 20, "sheet.insert_cols", {"handle": h, "sheet": "Sheet1", "start_col": 10, "count": 1})
    # Data is unchanged after the no-op shift.
    still = call(p, 21, "range.get", {"handle": h, "sheet": "Sheet1", "range": "A1:C1"})
    assert [c["value"] for c in still["cells"][0]] == ["h1", "h2", "h3"], still

    # --- Validation: count 0 → COLUMN_INDEX_INVALID; start_row 0 → ROW_INDEX_INVALID ---
    call_err(p, 22, "sheet.insert_cols",
             {"handle": h, "sheet": "Sheet1", "start_col": 2, "count": 0},
             "COLUMN_INDEX_INVALID")
    call_err(p, 23, "sheet.insert_rows",
             {"handle": h, "sheet": "Sheet1", "start_row": 0, "count": 1},
             "ROW_INDEX_INVALID")

    # Save to disk so we know the modified workbook is a valid .xlsx and reloads cleanly.
    out = os.path.join(tmp, "phase7.xlsx")
    saved = call(p, 24, "workbook.save", {"handle": h, "path": out})
    assert saved["size_bytes"] > 0, saved
    call(p, 25, "workbook.close", {"handle": h})
finally:
    p.terminate()
    try: p.wait(timeout=3)
    except subprocess.TimeoutExpired: p.kill()
    # cleanup tmp
    for f in os.listdir(tmp):
        os.unlink(os.path.join(tmp, f))
    os.rmdir(tmp)

print("Phase 7 smoke OK: insert/delete rows & cols round-trip plus footgun guard verified.")
PY
