---
name: excel
description: "A stateful, formula-aware Excel engine an agent can actually drive - not a file it has to parse by hand. Opens existing .xlsx/.xlsm/.xls workbooks (or creates new ones) entirely in memory, then queries and richly edits them across a whole session without reloading: cell values, typed formulas, styling, conditional formatting, data validation, charts, sheet structure and outline grouping, rows and columns, tables, and named ranges - flushed to disk with an atomic, corruption-safe save. Its standout capability is headless recalculation: Apache POI evaluates ~280 Excel functions in place, so the agent works with real computed results instead of the stale cached zeros that code-based approaches (openpyxl/pandas, or Claude's built-in spreadsheet handling) leave behind - and every read distinguishes a genuine value from an as-yet-uncomputed formula. It can also review a model, not just write one: workbook.audit scans for hardcoded constants and hidden rows, and cell.trace walks precedents and dependents. Because all edits flow through one POI writer, it never triggers the 'Excel repaired records' corruption that second-writer libraries cause. 41 tools across workbook lifecycle (incl. audit), range I/O (incl. cell styling, conditional formats, data validation, and formula tracing), sheet management, presentation and outlining, line and bar charts, tables, named ranges, and read-only VBA extraction."
version: 0.3.0 # mcp-catalog-version
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
> only** — `MCP.md`. Versioning/PRs/issues happen in that repo;
> bump `version:` here when adopting a new release.

## TRIGGER — reach for this before any other tool

**Whenever the task touches a file ending `.xlsx` / `.xlsm` / `.xls`, or the user says
spreadsheet / workbook / Excel / Tabelle / Arbeitsmappe — `workbook.open` (or
`workbook.create`) is the first call.** Do **not** write a Python or Node script for it,
and do **not** spend a call checking whether `openpyxl`, `pandas.read_excel`, `exceljs` or
`xlsx` is installed. This server is the supported path for two reasons that hold whatever
a given container happens to have on it: every edit flows through one POI writer, which is
what keeps the file out of the "Excel repaired records" state a second serializer puts it
in, and it is the only route that **evaluates formulas headlessly** rather than handing
back the stale cached values a library read returns. (On platform sessions those libraries
are also simply absent — checking costs a call and changes nothing.)

**SKIP** when — and only when — the file is a `.csv` (plain file tools are the right
reach), the file is `.xlsb` or not a spreadsheet at all (see **When NOT to reach for this**
below), or the user has explicitly asked for a script or a library rather than the result.

## When to reach for this

- The user asks to read, inspect, summarize, modify, or create an Excel workbook (.xlsx / .xlsm / .xls).
- The agent needs to evaluate formulas, edit cells, insert or delete rows/columns, rename or reorder sheets, manage named ranges, or extract VBA source.
- The task is to **present** a model, not just populate it: cell styling, number formats, column widths, freeze panes, conditional formatting, dropdown/range data validation, outline grouping, or a line/bar chart.
- The task is to **review** a workbook someone else built: `workbook.audit` flags hardcoded constants, error cells, uncomputed formulas, circular references, and rows hidden outside an outline group; `cell.trace` walks a formula's precedents and dependents.
- The task involves structured spreadsheet data where formula correctness and cell-type fidelity matter — not quick-and-dirty CSV work (use plain file tools for that).

## When NOT to reach for this

- **`.xlsb` (binary workbooks)** — rejected at open; POI has no binary-workbook reader. Convert with `use-anydoc` for read-only extraction.
- **The file isn't a spreadsheet** (`.docx`, `.pdf`, `.odt`, `.rtf`, `.epub`, `.csv`) — `use-anydoc` reads all of these to Markdown; `use-docling` if it needs OCR.

Both are **read-only extraction paths, not a shortcut for editing**. Two traps if you use `use-anydoc` to orient before editing here:

- **Its grid has no A1 addresses**, and it starts at the sheet's *used range*, not at A1 — a sheet whose data begins at D11 yields a table whose first column is D, with nothing in the output saying so. Never translate a position in anydoc output into an A1 address for a `range.set`. Re-locate the cell with `range.get` / `table.list` / `named_range.list` first.
- **It reads cached values only**, so cells this server left as `type: "formula_uncomputed"` come through as *blank*. Call `workbook.recalculate` and `workbook.save` before converting a file this server wrote.

## Capabilities

41 tools over stdio, grouped by area:

- **Workbook lifecycle, state & review (10)** — `workbook.open`, `workbook.create`, `workbook.save`, `workbook.close`, `workbook.list_sheets`, `workbook.metadata`, `workbook.recalculate`, `workbook.capabilities_report`, `workbook.list_handles`, `workbook.audit`
- **Range I/O, styling & rules (8)** — `range.get`, `range.set`, `range.clear`, `range.fill`, `range.set_style`, `range.set_conditional_format`, `range.set_validation`, `range.get_validation`
- **Formula tracing (1)** — `cell.trace`
- **Sheet management, presentation & outlining (13)** — `sheet.create`, `sheet.delete`, `sheet.copy`, `sheet.rename`, `sheet.merged_regions`, `sheet.set_format`, `sheet.insert_rows`, `sheet.delete_rows`, `sheet.insert_cols`, `sheet.delete_cols`, `sheet.group_rows`, `sheet.group_cols`, `sheet.outline`
- **Charts (1)** — `chart.create`
- **Tables (2)** — `table.list`, `table.get`
- **Named ranges (4)** — `named_range.list`, `named_range.get`, `named_range.set`, `named_range.delete`
- **VBA, read-only (2)** — `vba.list_modules`, `vba.get_module`

Highlights: in-memory open/create behind a session handle; typed-cell reads that separate a real value from an uncomputed formula; **headless formula recalculation** (~280 of Excel's functions evaluated in place — uncommon for an agent-drivable spreadsheet tool); native cell styling, conditional formatting, data validation and sheet presentation through a single POI writer (no second-writer corruption); a **review** path as well as an authoring one (`workbook.audit` for hardcoded constants, error cells, uncomputed formulas, circular references, and rows hidden outside an outline group, `cell.trace` for precedents/dependents); atomic temp-file-and-rename saves.

Every tool declares an MCP `outputSchema` and behaviour annotations (`readOnlyHint` / `destructiveHint` / `idempotentHint`), so a client can tell a read from a write without parsing English — 16 of the 41 are read-only. The server validates its own results against those schemas at runtime.

## Limitations

- **VBA is read-only.** Modules and their source can be listed and extracted; macros cannot be created, edited, or executed.
- **Modern / dynamic-array functions are not recalculated.** The `LAMBDA` family, dynamic-array spills (`FILTER`, `SORT`, `UNIQUE`, `XLOOKUP` spill), `LET`, linked data types, `IMAGE`, `PY`, and ~220 of Excel's 500+ functions are not evaluated by the headless engine. Their formula text is preserved, but the cached result stays stale until Excel/LibreOffice reopens the file. Call `workbook.capabilities_report` to see which cells are affected before editing.
- **No Power Query / DAX / data model / pivot tables.** Not introspected or editable. Pivot tables and ListObject table *writes* are the next version's headline work; today `table.list` / `table.get` read them but nothing creates or modifies one.
- **Charts are create-only.** `chart.create` draws **line and bar** charts; there is no read-back, no delete, no PNG render, and no pie/scatter/combo. Re-running it adds a second chart rather than replacing the first.
- **Merged regions are read-only.** `sheet.merged_regions` reports them; nothing merges or unmerges. (Conditional formatting and data validation *are* now writable — see Capabilities.)
- **Edits are one range at a time.** There is no batch/multi-range write in a single call; a large edit is a sequence of `range.set` calls against the same in-memory handle.
- **Rich / linked data types are preserve-only.** Stock/geo and other rich-data parts survive a round-trip but cannot be read or modified.
- **`.xlsb` (binary) is rejected at open.** `.xlsx`, `.xlsm`, `.xls` are supported; styling is `.xlsx`/`.xlsm` only. The `FORMAT_UNSUPPORTED` message routes the caller to `use-anydoc` for read-only extraction — see **When NOT to reach for this** above for the two traps that come with it.

### Known issues in 0.3.0

All three are open date defects, measured rather than suspected, and all three matter most to exactly the modelling work this server is for. Track them in [`skaile-ai/excel-mcp`](https://github.com/skaile-ai/excel-mcp/issues).

- **1904-windowed workbooks read every date 1462 days early** ([#80](https://github.com/skaile-ai/excel-mcp/issues/80)). A workbook saved with the 1904 date system (the old Mac default, still found in the wild) is read against the 1900 epoch. Dates written by this server are unaffected; dates *read back* from such a file are wrong by a little over four years. If a workbook's dates look implausibly early, check its date system before trusting them.
- **A pre-1900 ISO date is written as `-1` and reported as success** ([#77](https://github.com/skaile-ai/excel-mcp/issues/77)). Excel's serial epoch starts at 1900, so `range.set` cannot represent an earlier date and does not currently refuse it. Do not write historical dates before 1900-01-01.
- **`range.fill` of a date cell with `copy_style: false` yields a bare serial** ([#81](https://github.com/skaile-ai/excel-mcp/issues/81)), which reads back as a plain number rather than a date. Leave `copy_style` at its default when filling dates, or re-apply a date `number_format` afterwards.

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
  → workbook.recalculate               # refresh cached formula results
  → range.get                          # read the computed values back and check them
  → range.set_style / sheet.set_format # style cells & sheet presentation
  → range.set_conditional_format       # rules that colour by value
  → range.set_validation               # dropdowns / bounds on input cells
  → chart.create                       # line or bar chart over a written range
  → workbook.audit / cell.trace        # review: hardcoded constants, hidden rows, precedents
  → workbook.save                      # flush to disk (atomic temp-file + rename)
  → workbook.close                     # release in-memory state
```

The process is **session-scoped**: one container per agent session, workbooks stay in memory across many tool calls so reads and edits don't pay a reload cost between turns. Closing without saving discards in-memory edits silently.

### The rebuild cycle — when you are writing a model, not just reading one

This is the write half of the flow above, tightened into the loop to run for any
multi-sheet write. It does not replace the orientation steps —
`workbook.capabilities_report` still comes before the first edit, or `workbook.recalculate`
will silently leave the modern-function cells stale:

```
workbook.open
  → workbook.capabilities_report  # which cells recalculate will leave stale — before the first edit
  → range.get                     # understand structure and existing formulas, before every change
  → range.clear / range.set       # seed row
  → range.fill                    # spread it
  → workbook.recalculate
  → range.get                     # read results back against expected values
  → workbook.audit                # errors, circular, uncomputed
  → workbook.save                 # explicit `path` — a NEW file, never the source
  → workbook.close
```

Two properties make it hold, and both are worth the extra calls:

- **Recalculate and read back after every write**, not once at the end. A formula that is
  written but never recalculated reads as `formula_uncomputed`, and a broken reference
  surfaces at the write that caused it instead of forty calls later.
- **Save to a NEW path** — pass `workbook.save` an explicit `path` rather than letting it
  write back over the handle's source. The input workbook stays intact, so a bad run is
  discarded rather than unwound, and the whole sequence is re-runnable from the same input.

## Non-obvious gotchas the agent must respect

- **Formula vs cached value.** `range.get` returns `{type: "formula", formula: "=SUM(A1:A3)", value: <cached>}`. Don't trust the cached value after a write — call `workbook.recalculate` first. Values of `type: "formula_uncomputed"` mean the cache is empty; always recalc before reading.
- **Modern functions are partially supported.** Post-2019 Excel functions (FILTER, SORT, LAMBDA family, dynamic-array spill, linked data types, IMAGE, PY, …) are not implemented by the recalc engine. Call `workbook.capabilities_report` first to see which cells `workbook.recalculate` will leave stale; plan writes accordingly. Excel / LibreOffice fix these on next open, but headless consumers won't.
- **Dates are timezone-naive.** Excel stores date serials as wall-clock values. Reads return `2024-03-05T10:00:00` (no `Z`, no offset) — do not interpret as a UTC instant.
- **Handles are session-local.** `workbook.open` / `workbook.create` return `wb-<hex>` handles that live for the process lifetime. A second `docker run` starts fresh.
- **Paths are container-local.** If the image mounts the host at `/data`, all `path` arguments must be `/data/...`, not host paths.
- **Sandbox is fail-closed.** The server refuses to start without `EXCEL_MCP_ROOT` unless `EXCEL_MCP_ALLOW_UNSANDBOXED=true` is explicitly set.
- **`workbook.audit` defaults are deliberately noisy — narrow them before reporting.** Every literal on an assumptions sheet is a finding by the letter of the rule, and so is the `1` in `=B2*(1+growth)`. Pass `input_sheets: ["Assumptions"]` to exempt the sheets where constants belong, and `ignore_literals: [0, 1]` to drop the idiomatic ones. On a well-built model that is the difference between six findings and none; do not present the raw default as a verdict on someone's workbook.
- **`chart.create` and the row/column inserts are not idempotent.** Calling `chart.create` twice makes two charts, and `sheet.insert_rows` twice inserts twice. Retry only after checking what actually landed — the tools declare this in their MCP `idempotentHint`, so a client that reads annotations already knows.
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
