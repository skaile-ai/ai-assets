---
name: excel
description: "A stateful, formula-aware Excel engine an agent can actually drive - not a file it has to parse by hand. Opens existing .xlsx/.xlsm/.xls workbooks (or creates new ones) entirely in memory, then queries and richly edits them across a whole session without reloading: cell values, typed formulas, styling, sheet structure, rows and columns, tables, and named ranges - flushed to disk with an atomic, corruption-safe save. Its standout capability is headless recalculation: Apache POI evaluates ~280 Excel functions in place, so the agent works with real computed results instead of the stale cached zeros that code-based approaches (openpyxl/pandas, or Claude's built-in spreadsheet handling) leave behind - and every read distinguishes a genuine value from an as-yet-uncomputed formula. Because all edits flow through one POI writer, it never triggers the 'Excel repaired records' corruption that second-writer libraries cause. 28 tools across workbook lifecycle, range I/O (incl. cell styling), sheet management and presentation, tables, named ranges, and read-only VBA extraction."
version: 0.2.0
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

Provides 28 tools across workbook lifecycle, range I/O (incl. cell styling), sheet
management and presentation, row/column mutation, tables, named ranges, and read-only
VBA module extraction.

## Runtime

Built and pinned by the platform Nix flake (`platform/nix/flake.nix`'s `mcps.excel` derivation).
At session start the runner resolves `${recipe:excel}` to the closure's `/nix/store` path. No
`docker build` step required for platform-deployed sessions.

For local standalone testing without the platform: build the docker image
(`docker build -t excel-mcp:dev .`) and override `command`/`args` in `skaile.yaml`'s
`mcp_servers:` block.

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
