---
name: xls
description: "Use when an agent needs to read, modify, or inspect .xlsx / .xlsm /
  .xls workbooks. Wraps the Excel MCP stdio server (Java / Apache POI) exposing 28
  tools across workbook lifecycle, range I/O (incl. cell styling), sheet management
  and presentation, row/column mutation, tables, named ranges, and read-only VBA
  module extraction. Every cell read surfaces
  type, value, and (if present) typed formula — so the agent can distinguish a stale
  cached value from a real one."
metadata:
  tags:
  - excel
  - xlsx
  - xlsm
  - xls
  - spreadsheet
  - workbook
  - mcp
  - poi
  - range
  - formula
  - table
  - named-range
  - vba
  stage: alpha
---

# Excel MCP

An MCP server that gives an agent first-class ability to create, modify, review, and summarize Excel files with real fidelity. Built on Apache POI, shipped as a Docker image, stdio transport.

## When to reach for this skill

- The user asks to read, inspect, summarize, modify, or create an Excel workbook (.xlsx / .xlsm / .xls).
- The agent needs to evaluate formulas, edit cells, insert or delete rows/columns, rename or reorder sheets, manage named ranges, or extract VBA source.
- The task involves structured spreadsheet data where formula correctness and cell-type fidelity matter — not quick-and-dirty CSV work (use plain file tools for that).

## Core flow

A typical session looks like:

```
workbook.open (or workbook.create)
  → workbook.capabilities_report       # check for unsupported modern functions before editing
  → workbook.metadata / list_sheets    # orient
  → range.get / named_range.get        # read
  → range.set / sheet.insert_rows …    # write values & formulas
  → range.set_style / sheet.set_format # style cells & sheet presentation
  → workbook.recalculate               # refresh cached formula results
  → workbook.save                      # flush to disk (atomic temp-file + rename)
  → workbook.close                     # release in-memory state
```

The process is **session-scoped**: one container per agent session, workbooks stay in memory across many tool calls so reads and edits don't pay a reload cost between turns. Closing without saving discards in-memory edits silently.

## Non-obvious gotchas the agent must respect

- **Formula vs cached value.** `range.get` returns `{type: "formula", formula: "=SUM(A1:A3)", value: <cached>}`. Don't trust the cached value after a write — call `workbook.recalculate` first. Values of `type: "formula_uncomputed"` mean the cache is empty; always recalc before reading.
- **Modern functions are partially supported.** Post-2019 Excel functions (FILTER, SORT, LAMBDA family, dynamic-array spill, linked data types, IMAGE, PY, …) are not implemented by the recalc engine. Call `workbook.capabilities_report` first to see which cells `workbook.recalculate` will leave stale; plan writes accordingly. Excel / LibreOffice fix these on next open, but headless consumers won't.
- **Dates are timezone-naive.** Excel stores date serials as wall-clock values. Reads return `2024-03-05T10:00:00` (no `Z`, no offset) — do not interpret as a UTC instant.
- **Handles are session-local.** `workbook.open` / `workbook.create` return `wb-<hex>` handles that live for the process lifetime. A second `docker run` starts fresh.
- **Paths are container-local.** If the image mounts the host at `/data`, all `path` arguments must be `/data/...`, not host paths.
- **Sandbox is fail-closed.** The server refuses to start without `EXCEL_MCP_ROOT` unless `EXCEL_MCP_ALLOW_UNSANDBOXED=true` is explicitly set.
- **Style through the MCP, never a second library.** Use `range.set_style` (fills, fonts, borders, number formats, alignment, wrap) and `sheet.set_format` (column widths, row heights, freeze panes, tab color) for all presentation. Do **not** post-process the saved file with openpyxl / exceljs / a second writer — two serializers over one file is exactly what produces the "Excel repaired records" corruption. `range.set_style` merges onto existing styles, so layer it in any order; styling is `.xlsx`/`.xlsm` only and rejects full-column/row ranges (pass a bounded range like `A1:N1`).

## Styling recipe

`range.set_style` + `sheet.set_format` cover the same ground the original broken session reached for `exceljs` to do. Apply styling in **whole-range calls — one call per visual group**, never cell-by-cell: it is clearer and cheaper (styles dedupe per call, so a styled row costs one style, not one-per-cell), and it keeps every edit inside the single POI writer.

A typical pass over a finished sheet:

1. **Header bar** — one call over the header row:
   `range.set_style(range:"A1:N1", style:{fill_color:"#13151A", font:{name:"Inter", size:10, bold:true, color:"#F4F4F5"}, horizontal_alignment:"center", border:{bottom:{style:"medium", color:"#7300FF"}}})`
2. **Number formats** — one call per numeric block: `style:{number_format:"#,##0"}` (or `"0%"` for ratios stored 0–1, `"#,##0.00"` for currency). Styling merges, so this does not disturb fonts/fills set earlier.
3. **Emphasis rows** (totals, section headers) — bold font plus a light fill over the row's range.
4. **Sheet frame** — one `sheet.set_format` per sheet: `column_widths`, optional `row_heights`, `freeze:{rows:1, cols:0}` to pin the header, and a `tab_color`.

Constraints worth remembering:
- Colors are `#RRGGBB`. Borders take `{style, color?}` per edge (`thin`/`medium`/`thick`/`dashed`/`dotted`/`double`/`hair`/`none`).
- `range.set_style` styles a **bounded** range only — full-column (`A:A`) / full-row (`1:1`) are rejected; pass `A1:A200`.
- Styling is `.xlsx`/`.xlsm` only and never triggers recalculation — it is orthogonal to `workbook.recalculate`.

## Reference documents (in this folder)

- `README.md` — full tool inventory with parameters and descriptions, env-var reference, run / deploy instructions, MCP Inspector walkthrough.
- `excel-mcp-server-implementation-plan.md` — contract: every tool shape, error code, and architectural decision.
- `excel-mcp-server-future-work.md` — everything deferred past v1, plus authoring conventions for new tools.
- `excel-mcp-server-skill.md` — design notes on why an MCP over a skill, engine survey, original intent.

## Authoring guidance for agents writing spreadsheets

*(Placeholder — the Excel "best practices" rules for agents building workbooks from scratch are pending a briefing with the product owners. Until that lands, agents should apply general good hygiene: don't mix hardcoded values into formulas, represent percentages as `0–1` with `%` number format, avoid merged cells, prefer grouped rows over hidden rows. Treat these as defaults, not hard contracts.)* For visual formatting, see the **Styling recipe** above.
