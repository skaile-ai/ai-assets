---
name: excel
description: "A stateful, formula-aware Excel engine an agent can actually drive - not a file it has to parse by hand. Opens existing .xlsx/.xlsm/.xls workbooks (or creates new ones) entirely in memory, then queries and richly edits them across a whole session without reloading: cell values, typed formulas, styling, sheet structure, rows and columns, tables, and named ranges - flushed to disk with an atomic, corruption-safe save. Its standout capability is headless recalculation: Apache POI evaluates ~280 Excel functions in place, so the agent works with real computed results instead of the stale cached zeros that code-based approaches (openpyxl/pandas, or Claude's built-in spreadsheet handling) leave behind - and every read distinguishes a genuine value from an as-yet-uncomputed formula. Because all edits flow through one POI writer, it never triggers the 'Excel repaired records' corruption that second-writer libraries cause. 28 tools across workbook lifecycle, range I/O (incl. cell styling), sheet management and presentation, tables, named ranges, and read-only VBA extraction."
version: 0.2.1 # mcp-catalog-version
transport: stdio
recipe:
  attr: mcps.excel
command: ${recipe:excel:bin}/java
args:
  - -jar
  - ${recipe:excel:lib}/excel-mcp.jar
env:
  # EXCEL_MCP_ROOT is the path-sandbox root. The server validates it exists at
  # startup (fail-closed), so it must be a path that is present in every session
  # container regardless of where the workspace is mounted. We use `/` rather
  # than a session-specific path (`/skaile/workspace`, `/workspace`) because the
  # asset cannot know the host's mount layout; the per-session container is
  # already the isolation boundary. Operators can override per session via env.
  # TODO(workspaces): once the runner exposes a ${workspace} substitution token,
  # set this to ${workspace} for a true per-session sandbox.
  EXCEL_MCP_ROOT: /
  JAVA_HOME: ${recipe:excel}
keywords:
  - excel
  - xlsx
  - xlsm
  - xls
  - spreadsheet
  - workbook
  - mcp
  - poi
---

# Excel MCP Server

Docker-based MCP server for Excel file operations, built on Apache POI 5.5.1.

> **Source code:** the server source, build (`pom.xml`, `flake.nix`, `Dockerfile`,
> `mvnw`), smoke tests, and implementation docs live in their own repo,
> [`skaile-ai/excel-mcp`](https://github.com/skaile-ai/excel-mcp) (a submodule at
> the workspace root as `excel-mcp/`). This directory is the **catalog entry
> only** — `MCP.md` + `SKILL.md`. Versioning/PRs/issues happen in that repo;
> bump `version:` here when adopting a new release.

## When to reach for this

- The user asks to read, inspect, summarize, modify, or create an Excel workbook (.xlsx / .xlsm / .xls).
- The agent needs to evaluate formulas, edit cells, insert or delete rows/columns, rename or reorder sheets, manage named ranges, or extract VBA source.
- The task involves structured spreadsheet data where formula correctness and cell-type fidelity matter — not quick-and-dirty CSV work (use plain file tools for that).

## Capabilities

28 tools over stdio, grouped by area:

- **Workbook lifecycle & state (9)** — `workbook.open`, `workbook.create`, `workbook.save`, `workbook.close`, `workbook.list_sheets`, `workbook.metadata`, `workbook.recalculate`, `workbook.capabilities_report`, `workbook.list_handles`
- **Range I/O (4)** — `range.get`, `range.set`, `range.clear`, `range.set_style`
- **Sheet management & presentation (9)** — `sheet.create`, `sheet.delete`, `sheet.rename`, `sheet.merged_regions`, `sheet.set_format`, `sheet.insert_rows`, `sheet.delete_rows`, `sheet.insert_cols`, `sheet.delete_cols`
- **Tables (2)** — `table.list`, `table.get`
- **Named ranges (2)** — `named_range.list`, `named_range.get`
- **VBA, read-only (2)** — `vba.list_modules`, `vba.get_module`

Highlights: in-memory open/create behind a session handle; typed-cell reads that separate a real value from an uncomputed formula; **headless formula recalculation** (~280 of Excel's functions evaluated in place — uncommon for an agent-drivable spreadsheet tool); native cell styling and sheet presentation through a single POI writer (no second-writer corruption); atomic temp-file-and-rename saves.

## Limitations

- **VBA is read-only.** Modules and their source can be listed and extracted; macros cannot be created, edited, or executed.
- **Modern / dynamic-array functions are not recalculated.** The `LAMBDA` family, dynamic-array spills (`FILTER`, `SORT`, `UNIQUE`, `XLOOKUP` spill), `LET`, linked data types, `IMAGE`, `PY`, and ~220 of Excel's 500+ functions are not evaluated by the headless engine. Their formula text is preserved, but the cached result stays stale until Excel/LibreOffice reopens the file. Call `workbook.capabilities_report` to see which cells are affected before editing.
- **No Power Query / DAX / data model / pivot tables.** Not introspected or editable.
- **No charts or conditional-formatting authoring.** Merged regions and data validation are read-only (reported, not writable).
- **Rich / linked data types are preserve-only.** Stock/geo and other rich-data parts survive a round-trip but cannot be read or modified.
- **`.xlsb` (binary) is rejected at open.** `.xlsx`, `.xlsm`, `.xls` are supported; styling is `.xlsx`/`.xlsm` only.

## Runtime

Built and pinned by the platform Nix flake (`platform/nix/flake.nix`'s `mcps.excel` derivation).
At session start the runner resolves `${recipe:excel}` to the closure's `/nix/store` path. No
`docker build` step required for platform-deployed sessions.

For local standalone testing without the platform: clone
[`skaile-ai/excel-mcp`](https://github.com/skaile-ai/excel-mcp), build the docker
image there (`docker build -t excel-mcp:dev .`), and override `command`/`args` in
`skaile.yaml`'s `mcp_servers:` block.

## Override examples

Override command and workspace root in `skaile.yaml` for standalone use:

```yaml
dependencies:
  - mcp:excel

mcp_servers:
  - id: excel
    command: docker
    args: [run, --rm, -i, -v, "/projects:/data:rw", -e, EXCEL_MCP_ROOT=/data, excel-mcp:dev]
```

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

`range.set_style` + `sheet.set_format` cover the same ground a code-based session would reach for `exceljs` to do. Apply styling in **whole-range calls — one call per visual group**, never cell-by-cell: it is clearer and cheaper (styles dedupe per call, so a styled row costs one style, not one-per-cell), and it keeps every edit inside the single POI writer.

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

## Authoring guidance for agents writing spreadsheets

*(Placeholder — the Excel "best practices" rules for agents building workbooks from scratch are pending a briefing with the product owners. Until that lands, agents should apply general good hygiene: don't mix hardcoded values into formulas, represent percentages as `0–1` with `%` number format, avoid merged cells, prefer grouped rows over hidden rows. Treat these as defaults, not hard contracts.)* For visual formatting, see the **Styling recipe** above.

## Reference documents (in the [`excel-mcp`](https://github.com/skaile-ai/excel-mcp) repo)

- `README.md` — full tool inventory with parameters and descriptions, env-var reference, run / deploy instructions, MCP Inspector walkthrough.
- `excel-mcp-server-implementation-plan.md` — contract: every tool shape, error code, and architectural decision.
- `excel-mcp-server-future-work.md` — everything deferred past v1, plus authoring conventions for new tools.
