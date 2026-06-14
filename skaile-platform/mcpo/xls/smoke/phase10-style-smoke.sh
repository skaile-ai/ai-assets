#!/usr/bin/env bash
# Phase 10 verify: range.set_style + sheet.set_format end-to-end over stdio. Builds an empty
# workbook, writes data, styles it, saves, and reopens BOTH through the server and through a raw
# POI parse (jshell) to prove the saved file is valid OOXML — i.e. the styling round-trip does not
# produce the "Excel repaired records" corruption that the exceljs/openpyxl workarounds caused.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

OUT="$TMP/styled.xlsx"

python3 - "$JAR" "$OUT" <<'PY'
import os, sys
sys.path.insert(0, os.environ["SMOKE_DIR"])
from _smoke_common import start, handshake, call, call_expect_error

jar = sys.argv[1]
out = sys.argv[2]

p = start(jar)
try:
    handshake(p, "p10")

    # Both new tools must be advertised.
    import json
    from _smoke_common import send, recv
    send(p, {"jsonrpc": "2.0", "id": 5, "method": "tools/list", "params": {}})
    listed = recv(p)
    names = {t["name"] for t in listed["result"]["tools"]}
    assert "range.set_style" in names, sorted(names)
    assert "sheet.set_format" in names, sorted(names)

    created = call(p, 10, "workbook.create", {"path": out})
    h = created["handle"]

    call(p, 11, "range.set", {
        "handle": h, "sheet": "Sheet1", "start": "A1",
        "values": [["Revenue", "Q1", "Q2"], ["Acme", 1200, 1850]],
    })

    # Header styling: dark fill, white bold font, bottom border, centered.
    styled = call(p, 12, "range.set_style", {
        "handle": h, "sheet": "Sheet1", "range": "A1:C1",
        "style": {
            "fill_color": "#13151A",
            "font": {"name": "Inter", "size": 10, "bold": True, "color": "#F4F4F5"},
            "border": {"bottom": {"style": "medium", "color": "#7300FF"}},
            "horizontal_alignment": "center",
            "vertical_alignment": "middle",
        },
    })
    assert styled["styled_cells"] == 3, styled

    # Number format on the data row (merge must not disturb anything else).
    call(p, 13, "range.set_style", {
        "handle": h, "sheet": "Sheet1", "range": "B2:C2",
        "style": {"number_format": "#,##0"},
    })

    # Sheet-level: column width, frozen header, tab color.
    fmt = call(p, 14, "sheet.set_format", {
        "handle": h, "sheet": "Sheet1",
        "column_widths": [{"column": "A", "width": 28}],
        "row_heights": [{"row": 1, "height": 28}],
        "freeze": {"rows": 1, "cols": 0},
        "tab_color": "#7300FF",
    })
    assert fmt["ok"] is True, fmt

    call(p, 15, "workbook.save", {"handle": h})

    # Read styling back through the server to confirm it persisted.
    got = call(p, 16, "range.get", {
        "handle": h, "sheet": "Sheet1", "range": "A1:A1", "include_formatting": True,
    })
    cell = got["cells"][0][0]
    assert cell["formatting"]["fill_color"] == "#13151A", cell
    assert cell["formatting"]["font"]["bold"] is True, cell

    # Error paths.
    call_expect_error(p, 17, "range.set_style",
        {"handle": h, "sheet": "Sheet1", "range": "A1", "style": {"fill_color": "purple"}},
        "STYLE_INVALID")
    call_expect_error(p, 18, "range.set_style",
        {"handle": h, "sheet": "Sheet1", "range": "A:A", "style": {"fill_color": "#FFFFFF"}},
        "STYLE_INVALID")

    call(p, 19, "workbook.close", {"handle": h})
    print("server round-trip OK")
finally:
    p.stdin.close()
    p.terminate()
PY

[[ -f "$OUT" ]] || { echo "output not saved: $OUT" >&2; exit 2; }

# Independent validity check: a raw POI parse of the saved file must succeed without throwing.
# This is the corruption gate — a malformed styles.xml would blow up here.
jshell --class-path "$JAR" - <<JSHELL | grep -q "POI_PARSE_OK" || { echo "raw POI parse failed" >&2; exit 1; }
import java.nio.file.*;
import org.apache.poi.xssf.usermodel.*;
try (var wb = new XSSFWorkbook("$OUT")) {
    var sheet = wb.getSheetAt(0);
    var tab = sheet.getTabColor();
    if (tab == null || !tab.getARGBHex().endsWith("7300FF")) throw new RuntimeException("tab color lost");
    if (sheet.getColumnWidth(0) != 28 * 256) throw new RuntimeException("col width lost");
    if (sheet.getPaneInformation() == null) throw new RuntimeException("freeze lost");
    System.out.println("POI_PARSE_OK");
}
/exit
JSHELL

echo "phase10-style-smoke: PASS"
