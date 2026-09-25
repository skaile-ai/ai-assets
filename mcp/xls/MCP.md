---
name: excel
description: "A stateful, formula-aware Excel engine an agent can actually drive - not a file it has to parse by hand. Opens existing .xlsx/.xlsm/.xls workbooks (or creates new ones) entirely in memory, then queries and richly edits them across a whole session without reloading: cell values, typed formulas, styling, conditional formatting, data validation, charts, merged regions, cell notes and threaded comments, sheet structure and outline grouping, rows and columns, and named ranges (tables are read-only) - flushed to disk with an atomic, corruption-safe save. Its standout capability is headless recalculation: Apache POI evaluates ~280 Excel functions in place, so the agent works with real computed results instead of the stale cached zeros that code-based approaches (openpyxl/pandas, or Claude's built-in spreadsheet handling) leave behind - and every read distinguishes a genuine value from an as-yet-uncomputed formula. It can also review a model, not just write one: workbook.audit scans for hardcoded constants and hidden rows, and cell.trace walks precedents and dependents. Because all edits flow through one POI writer, it avoids the 'Excel repaired records' corruption a second serializer over the same file causes. A workbook too large to load can still be read: workbook.open's read-only stream mode pages through it in flat memory. 55 tools across workbook lifecycle (incl. audit and stream mode), agent context, range I/O (incl. cell styling, conditional formats written and read back, data validation, formula tracing, formula-pattern search, and cell comments), sheet management (incl. merge/unmerge, sheet reordering and row paging), presentation written and read back, print setup and outlining, line and bar charts, tables, named ranges, read-only VBA extraction, and multi-call batching."
version: 0.4.0 # mcp-catalog-version
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

`workbook.open` and `workbook.create` are pinned eager — both ship with
`_meta: {"anthropic/alwaysLoad": true}` — so in a client that hides MCP tools behind a search step
they stay visible and callable without searching. They are the only two tools that mint a workbook
handle, and every other tool needs one, so between them they cover every opening move: the door into
an existing file and the door into a new one. The other 53 do not, including
`workbook.list_handles` — the third tool callable without a handle, but empty on turn one and so
useless as an opening move.

## When to reach for this

- The user asks to read, inspect, summarize, modify, or create an Excel workbook (.xlsx / .xlsm / .xls).
- The agent needs to evaluate formulas, edit cells, insert or delete rows/columns, rename or reorder sheets, manage named ranges, or extract VBA source.
- The task is to **present** a model, not just populate it: cell styling, number formats, column widths, freeze panes, conditional formatting, dropdown/range data validation, outline grouping, or a line/bar chart.
- The task is to **review** a workbook someone else built: `workbook.audit` flags hardcoded constants, error cells, uncomputed formulas, circular references, and rows hidden outside an outline group; `cell.trace` walks a formula's precedents and dependents; `formula.search` finds every cell whose formula mentions a given reference or function.
- The workbook is **too large to load** — an ERP export with millions of rows, where `workbook.open` answers `WORKBOOK_TOO_LARGE`. Reopen it with `mode: "stream"` and read it read-only (see **Large workbooks** below) rather than reaching for a script.
- The task involves structured spreadsheet data where formula correctness and cell-type fidelity matter — not quick-and-dirty CSV work (use plain file tools for that).

## When NOT to reach for this

- **`.xlsb` (binary workbooks)** — rejected at open; POI has no binary-workbook reader. Convert with `use-anydoc` for read-only extraction.
- **`.csv`** — plain text with no cells, formulas or styling to preserve. Read and write it with ordinary file tools; neither this server nor an extraction path is needed.
- **The file isn't a spreadsheet** (`.docx`, `.pdf`, `.odt`, `.rtf`, `.epub`) — `use-anydoc` reads all of these to Markdown; `use-docling` if it needs OCR.

The two `use-anydoc` routes above are **read-only extraction paths, not a shortcut for editing**. Two traps if you use `use-anydoc` to orient before editing here:

- **Its grid has no A1 addresses**, and it starts at the sheet's *used range*, not at A1 — a sheet whose data begins at D11 yields a table whose first column is D, with nothing in the output saying so. Never translate a position in anydoc output into an A1 address for a `range.set`. Re-locate the cell with `range.get` / `table.list` / `named_range.list` first.
- **It reads cached values only**, so cells this server left as `type: "formula_uncomputed"` come through as *blank*. Call `workbook.recalculate` and `workbook.save` before converting a file this server wrote.

## Capabilities

55 tools over stdio, grouped by area:

<!-- mcp-catalog-tools -->
- **Workbook lifecycle, state & review (10)** — `workbook.open`, `workbook.create`, `workbook.save`, `workbook.close`, `workbook.list_sheets`, `workbook.metadata`, `workbook.recalculate`, `workbook.capabilities_report`, `workbook.list_handles`, `workbook.audit`
- **Agent context (3)** — `context.get`, `context.set`, `context.clear`
- **Range I/O, styling & rules (9)** — `range.get`, `range.set`, `range.clear`, `range.fill`, `range.set_style`, `range.set_conditional_format`, `range.get_conditional_format`, `range.set_validation`, `range.get_validation`
- **Formula tracing & search (2)** — `cell.trace`, `formula.search`
- **Cell comments (3)** — `cell.set_comment`, `cell.get_comment`, `range.get_comments`
- **Sheet management, presentation, outlining & paging (18)** — `sheet.create`, `sheet.delete`, `sheet.copy`, `sheet.move`, `sheet.rename`, `sheet.merged_regions`, `sheet.merge_cells`, `sheet.unmerge_cells`, `sheet.set_format`, `sheet.get_format`, `sheet.scan`, `sheet.insert_rows`, `sheet.delete_rows`, `sheet.insert_cols`, `sheet.delete_cols`, `sheet.group_rows`, `sheet.group_cols`, `sheet.outline`
- **Charts (1)** — `chart.create`
- **Tables (2)** — `table.list`, `table.get`
- **Named ranges (4)** — `named_range.list`, `named_range.get`, `named_range.set`, `named_range.delete`
- **VBA, read-only (2)** — `vba.list_modules`, `vba.get_module`
- **Batching (1)** — `batch`
<!-- /mcp-catalog-tools -->

Highlights: in-memory open/create behind a session handle; a per-workbook agent-context note (`context.get`/`context.set`/`context.clear`) that travels with the file in a normal-hidden `_agent_context` sheet — discoverable via Excel's Unhide, capped at 32,000 characters, and flagged by `workbook.open` when one is present; typed-cell reads that separate a real value from an uncomputed formula; **headless formula recalculation** (~280 of Excel's functions evaluated in place — uncommon for an agent-drivable spreadsheet tool); native cell styling, conditional formatting (written and read back, including Excel 2010+ extension rules), data validation, merged-region writes and sheet presentation including print setup (written and read back), all through a single POI writer (no second-writer corruption); `formula.search` to find every cell whose formula matches a pattern, across sheets, by substring or regex; a **review** path as well as an authoring one (`workbook.audit` for hardcoded constants, error cells, uncomputed formulas, circular references, and rows hidden outside an outline group, `cell.trace` for precedents/dependents); cell notes and threaded comments read back with their replies, one cell at a time or a whole sheet at once; `batch` to run up to 200 tool calls in one round trip; a read-only **stream mode** for workbooks too large to load; atomic temp-file-and-rename saves.

Every tool declares an MCP `outputSchema` and behaviour annotations (`readOnlyHint` / `destructiveHint` / `idempotentHint`), so a client can tell a read from a write without parsing English — 23 of the 55 are read-only, and 11 are flagged destructive. Every one of the 55 states all four hints explicitly, because MCP's documented defaults are the opposite of what most of them do. The server validates its own results against those schemas at runtime.

## Limitations

- **VBA is read-only.** Modules and their source can be listed and extracted; macros cannot be created, edited, or executed.
- **Modern / dynamic-array functions are not recalculated.** The `LAMBDA` family, dynamic-array spills (`FILTER`, `SORT`, `UNIQUE`, `XLOOKUP` spill), `LET`, linked data types, `IMAGE`, `PY`, and ~220 of Excel's 500+ functions are not evaluated by the headless engine. Their formula text is preserved, but the cached result stays stale until Excel/LibreOffice reopens the file — which `workbook.save` now actively arranges by marking the saved file for full recalculation on open (`force_recalc_on_open`, default true). That fixes the file for a *person* who opens it; a headless consumer reading the saved bytes still sees the stale cache. Call `workbook.capabilities_report` to see which cells are affected before editing.
- **No Power Query / DAX / data model / pivot tables.** Not introspected or editable. `table.list` / `table.get` read ListObject tables, but nothing creates or modifies one, and pivot tables are not reachable at all — both sit in the server's deferred future-work list with no open issue scheduling them.
- **Charts are create-only.** `chart.create` draws **line and bar** charts; there is no read-back, no delete, no PNG render, and no pie/scatter/combo. Re-running it adds a second chart rather than replacing the first.
- **Merging is writable, but a merge is lossy.** `sheet.merge_cells` creates a region, `sheet.unmerge_cells` removes every region overlapping a range, and `sheet.merged_regions` reports them. A merge keeps the **top-left cell's value and discards the contents of every other cell it covers** — unmerging splits the block back up but cannot bring those contents back (their formatting survives). An overlap with an existing region is refused with `MERGED_REGION_OVERLAP` and changes nothing, so call `sheet.merged_regions` first when unsure. Row/column inserts and deletes maintain a region the way Excel does: grow on an insert inside, move on an insert above/left, shrink on a partial delete.
- **Sheet presentation reads back with named gaps.** `sheet.get_format` returns column widths, row heights, the frozen pane, tab colour and print setup in the shape `sheet.set_format` accepts, so a read can be edited and written back. Only *explicit* widths and heights are listed. What those keys cannot express — a split (rather than frozen) pane, a theme tab colour — is named in `unreadable` rather than silently dropped. `.xlsx`/`.xlsm` only.
- **Conditional formats read back further than they write.** `range.get_conditional_format` returns every rule, including the ones `range.set_conditional_format` cannot create — icon sets, top-10 and text filters, three-stop colour scales, and the sheet's Excel 2010+ (`x14`) extension rules, whose settings arrive under `rule.extension`. Anything an entry leaves undescribed sets `incomplete: true` with a reason. Only `incomplete: false`, `unlisted_rules: []` *and* `truncated: false` together claim you are seeing the whole picture.
- **Threaded comments are read-only.** `cell.get_comment` and `range.get_comments` return a threaded comment's body, author and replies; a legacy note (`kind: "note"`) returns text and author. Only notes can be written: `cell.set_comment` refuses a threaded-comment cell with `THREADED_COMMENT_UNSUPPORTED` rather than overwriting it, so read before writing. `sheet.copy` does not carry a conversation to the copy — `uncopied_threads` names the cells that lost one. Comments are `.xlsx`/`.xlsm` only — `.xls` fails with `STYLE_INVALID`.
- **`batch` is not a transaction.** It runs up to 200 tool calls in order in one round trip, each exactly as it would run alone, but there is no rollback: with `stop_on_error: true` the operations before the failing one stay applied. Its operations' arguments are validated against each named tool's own schema when that operation runs.
- **Stream mode is read-only and partial.** A `mode: "stream"` handle answers only `range.get` (without `include_formatting`), `sheet.scan`, `table.list` / `table.get`, `named_range.list` / `named_range.get`, `context.get`, `workbook.list_sheets`, `workbook.metadata`, `workbook.list_handles` and `workbook.close`; everything else fails with `STREAMING_UNSUPPORTED`. `.xls` cannot be streamed. See **Large workbooks** below.
- **Rich / linked data types are preserve-only.** Stock/geo and other rich-data parts survive a round-trip but cannot be read or modified.
- **`.xlsb` (binary) is rejected at open.** `.xlsx`, `.xlsm`, `.xls` are supported; styling is `.xlsx`/`.xlsm` only. The `FORMAT_UNSUPPORTED` message routes the caller to `use-anydoc` for read-only extraction — see **When NOT to reach for this** above for the two traps that come with it.

### Known issues

Open against the release named in `version:` above. Track them in [`skaile-ai/excel-mcp`](https://github.com/skaile-ai/excel-mcp/issues).

- **A cell stored with the ISO date type (`t="d"`) cannot be read in memory mode** ([#180](https://github.com/skaile-ai/excel-mcp/issues/180)). Some generators write it. `range.get` over such a cell fails with an untyped error. Stream mode reads it.
- **A boolean written as `<v>true</v>` reads as `false` in memory mode** ([#181](https://github.com/skaile-ai/excel-mcp/issues/181)). Excel writes `1`/`0`, which read correctly; the non-standard spelling some generators use is silently inverted. Stream mode reads it as `true`.
- **`named_range.list` reports names `named_range.set` cannot write back** ([#164](https://github.com/skaile-ai/excel-mcp/issues/164)). A named constant (`0.19`), an expression (`vat_rate+1`) or a name that refers to another name is listed — in a field called `range`, though it holds no range — but writing that same value back fails with `RANGE_INVALID`. Only sheet-qualified rectangular ranges round-trip.
- **`sheet.insert_rows` near a multi-cell array formula** ([#154](https://github.com/skaile-ai/excel-mcp/issues/154)). An insert that would move cells onto the array is refused with `ARRAY_FORMULA_PROTECTED`, but a larger insert at the same row succeeds and **stretches the array over the new rows**, which changes what it computes. Recalculate and read the array back after any insert inside or just below one.

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
  → formula.search                     # find cells by formula pattern (substring or regex)
  → range.set / sheet.insert_rows …    # write values & formulas
  → workbook.recalculate               # refresh cached formula results
  → range.get                          # read the computed values back and check them
  → range.set_style / sheet.set_format # style cells & sheet presentation
  → range.set_conditional_format       # rules that colour by value
  → range.set_validation               # dropdowns / bounds on input cells
  → sheet.merge_cells                  # banner across columns (lossy — see Limitations)
  → chart.create                       # line or bar chart over a written range
  → workbook.audit / cell.trace        # review: constants, errors, circular, uncomputed, hidden rows; precedents
  → workbook.save                      # flush to disk (atomic temp-file + rename; marks recalc-on-open)
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

## Large workbooks

`workbook.open` checks an `.xlsx`/`.xlsm` **before** loading it: it estimates the heap the load needs from the zip's own directory and refuses with `WORKBOOK_TOO_LARGE` when that exceeds the server's heap. The refusal lists each sheet's size and carries a `hint` pointing at stream mode. A load that still runs out of memory answers the same code rather than failing untyped. `.xls` is only checked by file size.

Such a file can still be read:

```
workbook.open { path, mode: "stream" }   # read-only; loads nothing, no size limit
  → workbook.list_sheets                 # each sheet's declared_range — Excel's claim, often overstated
  → sheet.scan { sheet, start_row: 1 }   # populated rows only; pass next_start_row back until it is absent
  → range.get                            # or a bounded window, when you know where to look
  → workbook.close
```

- **Read forwards.** The handle remembers where its last read stopped, so consecutive `sheet.scan` pages — or `range.get` windows that each start where the previous one ended — read the sheet once in total. A read that starts *above* that point restarts from the top: about 45 s for a window a million rows down.
- **Formulas come with their cached values**, never recalculated. A cell filled down from another (a shared-formula follower) reports its cached value without formula text, and so does a non-anchor cell of an array formula.
- **Nothing can be written.** To edit such a file, work on a smaller extract.

## Non-obvious gotchas the agent must respect

- **Formula vs cached value.** `range.get` returns `{type: "formula", formula: "=SUM(A1:A3)", value: <cached>}`. Don't trust the cached value after a write — call `workbook.recalculate` first. Values of `type: "formula_uncomputed"` mean the cache is empty; always recalc before reading.
- **Modern functions are partially supported.** Post-2019 Excel functions (FILTER, SORT, LAMBDA family, dynamic-array spill, linked data types, IMAGE, PY, …) are not implemented by the recalc engine. Call `workbook.capabilities_report` first to see which cells `workbook.recalculate` will leave stale; plan writes accordingly. Excel / LibreOffice fix these on next open, but headless consumers won't.
- **A value write destroys the formula under it.** `range.set` replaces the destination outright: a non-null entry in `values` clears any formula the cell held. If you might be overwriting a formula you meant to keep, `range.get` it first; pass `formulas` when you mean to write one. A destination inside a multi-cell array formula is refused with `ARRAY_FORMULA_PROTECTED` before anything in the block is written.
- **`workbook.save` marks the file for recalculation on open.** `force_recalc_on_open` defaults to true, so a person opening the saved file in Excel/LibreOffice gets every formula recomputed — including the ~220 functions this server's engine cannot evaluate, and a workbook left on manual calculation is switched to automatic. It changes nothing for a *headless* reader of the saved bytes, so it is no substitute for `workbook.recalculate` before you read values back yourself.
- **An argument key the tool does not declare is an error.** Every tool rejects an undeclared key — at the top level and inside object arguments — with `VALIDATION_ERROR` naming it and listing what that object accepts (with a nearest match when it looks like a misspelling), rather than succeeding having ignored it. The exceptions are a few styling keys `sheet.set_format` and `range.set_conditional_format` know but cannot apply, which answer `STYLE_INVALID`. Fix the key; do not drop it and retry blind.
- **A date before the workbook's epoch is refused.** `range.set` answers `DATE_OUT_OF_RANGE`, naming the cell and the earliest storable day: 1899-12-31, or 1904-01-01 in a 1904-windowed workbook. Keep historical dates as text deliberately if you need them.
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
