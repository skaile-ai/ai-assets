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
