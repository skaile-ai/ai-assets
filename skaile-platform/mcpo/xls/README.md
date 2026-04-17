# Excel MCP Server

MCP server (stdio) that gives an AI agent first-class ability to **create, modify, review, and summarize Excel files** with real fidelity (typed formulas vs. cached values, named ranges, tables, VBA source).

Design docs:
- `excel-mcp-server-skill.md` — design intent, why MCP over a skill, engine survey.
- `excel-mcp-server-implementation-plan.md` — the contract. Every tool shape, error code, and build decision.
- `excel-mcp-server-future-work.md` — everything explicitly deferred past v1, plus authoring conventions for new tools.

## Tech stack

- **Java 21 LTS** (Eclipse Temurin), **Maven** with the wrapper pinned to 3.9.9, **Spotless** for Google Java Format.
- **Apache POI 5.5.x** (`poi`, `poi-ooxml`, `poi-scratchpad`) — in-process, Apache-2.0, broadest single-library coverage of what this MCP needs. POI is only imported from the engine layer.
- **Official MCP Java SDK** over stdio. No HTTP / SSE in v1.
- **Jackson** for JSON, **SLF4J + Logback** for logging (stderr-only — stdout is reserved for the MCP protocol).
- **JUnit 5 + AssertJ** for tests; a handful of smoke + regression tests, no coverage targets.
- Packaged as a single fat jar via `maven-shade-plugin`, shipped in a `eclipse-temurin:21-jre-jammy` Docker image.

## How it's structured

Four-layer separation enforced by package boundaries:

```
server/    — MCP SDK glue, ToolRegistry, dispatch. No Excel logic.
tools/     — One file per tool (e.g. WorkbookOpenTool). Thin business logic; never imports POI.
engine/    — WorkbookEngine interface + XssfInMemoryEngine. The ONLY place POI is imported.
  engine/poi/ — PoiCellReader/Writer, PoiAtomicSaver, PoiSizeGuard, PoiFormulaEvaluation, …
shape/     — JSON DTOs (records) shared across tools: CellShape, RangeShape, SheetShape.
handles/   — In-process handle registry (Map<HandleId, OpenWorkbook>). Single-threaded, no locks.
path/      — PathValidator, ExcelMcpRoot, FormatWhitelist.
error/     — ErrorCode enum, McpException, ErrorEnvelope.
config/, log/ — env-var parsing and Logback setup (stderr routing).
```

## Tools

v1 ships 25 tools across workbook lifecycle, range I/O, sheet management, row/col mutation, tables, named ranges, and read-only VBA — no charts, pivots, formatting writes, Power Query, or DAX (see future-work doc).

Column **LLM-optimized description** is the string returned by `ToolDefinition.description()` — what the calling LLM sees when deciding whether and how to use the tool. Unimplemented tools are marked *(planned)*; a later agent will fill in the description when the tool ships, following the three-part effect / prerequisite / gotcha template in `excel-mcp-server-future-work.md` → "Authoring conventions". Some of the earliest tools predate that template and read more like plain summaries; they'll be rewritten when the full description audit runs.

| Category | Tool | Parameters | LLM-optimized description |
|---|---|---|---|
| Workbook lifecycle | `workbook.open` | `path` — absolute path to the workbook (must resolve inside `EXCEL_MCP_ROOT` if set) | Open a .xlsx/.xlsm/.xls workbook from disk and return an opaque handle. |
| Workbook lifecycle | `workbook.create` | `path?` — optional destination path (must end in .xlsx/.xlsm/.xls) | Create a new empty .xlsx workbook. If a path is provided, it is remembered for workbook.save. |
| Workbook lifecycle | `workbook.save` | `handle`; `path?` — optional explicit destination; defaults to the source path | Save the workbook. Atomic write via temp+rename. Workbook stays open afterwards. |
| Workbook lifecycle | `workbook.close` | `handle` | Close a workbook handle. Any unsaved changes are discarded. |
| Workbook lifecycle | `workbook.list_sheets` | `handle` | List all sheets with their name, zero-based index, and hidden flag. |
| Workbook lifecycle | `workbook.metadata` | `handle`; `include_named_ranges?` (default true); `include_tables?` (default true) | Aggregate workbook metadata: filename, size, modified time, format, sheets, and (optionally) named ranges and tables. |
| Workbook lifecycle | `workbook.recalculate` | `handle` | *(planned — description TBD)* |
| Workbook lifecycle | `workbook.capabilities_report` | `handle` | *(planned — description TBD)* |
| Range I/O | `range.get` | `handle`; `sheet`; `range` (A1, e.g. `"A1:C10"` or `"C5"`) or `start`+`end`; `include_formatting?` (default false); `max_cells?` (default 10000) | Read a rectangular range. Each cell carries type, value, and (if any) the typed formula. Set include_formatting=true to include styling. |
| Range I/O | `range.set` | `handle`; `sheet`; `range` or `start`; `values` (2D row-major array); `formulas?` (optional 2D array, same shape as values; non-null entries override the value) | Write a 2D block of values and/or formulas. Does NOT auto-recalc — call workbook.recalculate to refresh cached formula results. |
| Range I/O | `range.clear` | `handle`; `sheet`; `range` (A1, e.g. `"A1:C10"`) | Remove the contents of every non-empty cell in the given A1 range. Styling is preserved. |
| Sheet management | `sheet.create` | `handle`; `name`; `index?` (default end) | *(planned — description TBD)* |
| Sheet management | `sheet.delete` | `handle`; `name` | *(planned — description TBD)* |
| Sheet management | `sheet.rename` | `handle`; `old_name`; `new_name` | *(planned — description TBD)* |
| Sheet management | `sheet.merged_regions` | `handle`; `sheet` | *(planned — description TBD)* |
| Row / column mutation | `sheet.insert_rows` | `handle`; `sheet`; `start_row` (1-based); `count` | *(planned — description TBD)* |
| Row / column mutation | `sheet.delete_rows` | `handle`; `sheet`; `start_row` (1-based); `count` | *(planned — description TBD)* |
| Row / column mutation | `sheet.insert_cols` | `handle`; `sheet`; `start_col` (1-based, A=1); `count` | *(planned — description TBD)* |
| Row / column mutation | `sheet.delete_cols` | `handle`; `sheet`; `start_col` (1-based, A=1); `count` | *(planned — description TBD)* |
| Tables | `table.list` | `handle` | *(planned — description TBD)* |
| Tables | `table.get` | `handle`; `name`; `include_formatting?` (default false); `max_cells?` (default 10000) | *(planned — description TBD)* |
| Named ranges | `named_range.list` | `handle` | *(planned — description TBD)* |
| Named ranges | `named_range.get` | `handle`; `name`; `include_formatting?` (default false); `max_cells?` (default 10000) | *(planned — description TBD)* |
| VBA (read-only) | `vba.list_modules` | `handle` | *(planned — description TBD)* |
| VBA (read-only) | `vba.get_module` | `handle`; `name` | *(planned — description TBD)* |

## Build

**If you only want to test via Docker / MCP Inspector: skip this section.** `docker build -t excel-mcp:dev .` compiles the fat jar inside the build stage of the `Dockerfile`, so the image always contains freshly-compiled code regardless of what's in your host `target/`. See "Manual testing with the MCP Inspector" below.

The commands below are for host-side development only — running the server as `java -jar …` without Docker, running tests, and pre-commit formatting. They are independent alternatives, not a sequence.

The repo ships with the Maven Wrapper pinned to **Maven 3.9.9**. A global `mvn` is not required — and is not what this project uses. **Always invoke the build through `./mvnw` (`mvnw.cmd` on Windows).** CI and local dev must use the same pinned Maven to avoid build-reproducibility drift.

| Command | When to run it |
|---|---|
| `./mvnw verify` | Before declaring a change done. Runs compile + tests + Spotless check; also produces the fat jar. |
| `./mvnw -DskipTests package` | Only when you want the fat jar but want to skip tests. |
| `./mvnw spotless:apply` | When `./mvnw verify` fails on formatting. Auto-rewrites sources to Google Java Format. |
| `./mvnw clean` | For a truly from-scratch host rebuild. Rarely needed. |

The output is a single fat jar at `target/excel-mcp-<version>.jar`, runnable with `java -jar`.

## Run

The server speaks MCP over **stdio only** in v1. No HTTP. Launched via the shaded jar:

```bash
java -jar target/excel-mcp-0.1.0-SNAPSHOT.jar
```

Or via Docker:

```bash
docker build -t excel-mcp:dev .

# With sandboxed filesystem root (production shape):
docker run --rm -i \
  -v "$(pwd)/data:/data:rw" \
  -e EXCEL_MCP_ROOT=/data \
  -e LOG_LEVEL=INFO \
  excel-mcp:dev

# Without sandboxing (dev only; accepts any path the agent gives):
docker run --rm -i excel-mcp:dev
```

### MCP client descriptor

```json
{
  "mcpServers": {
    "excel": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "-v", "/host/data:/data:rw",
        "-e", "EXCEL_MCP_ROOT=/data",
        "excel-mcp:dev"
      ]
    }
  }
}
```

## Manual testing with the MCP Inspector

Run the server under [`@modelcontextprotocol/inspector`](https://github.com/modelcontextprotocol/inspector) to click tools by hand and watch raw request/response frames. Useful when verifying a new tool before wiring it into an agent.

1. **Rebuild the image** after any source change — `docker build` caches layers aggressively on WSL, so if behaviour looks stale, force a rebuild:

   ```bash
   cd skaile-platform/mcpo/xls/
   docker build -t excel-mcp:dev .
   # or, if you suspect a cache issue:
   docker build --no-cache -t excel-mcp:dev .
   ```

2. **Smoke-run the image standalone** (no inspector, no mounts) to confirm it starts and exits cleanly on EOF. Should log `EXCEL_MCP_ROOT not set; path sandboxing disabled` and `mcp server started … tools=<N>`:

   ```bash
   docker run --rm -i --user 1000:1000 excel-mcp:dev
   ```

3. **Launch the inspector pointed at the image**, mounting a host directory as the sandbox root:

   ```bash
   # Run from skaile-platform/mcpo/xls/
   pwd && npx @modelcontextprotocol/inspector \
     docker run --rm -i --user 1000:1000 \
       -v "$PWD/test-data:/data" \
       -e EXCEL_MCP_ROOT=/data \
       excel-mcp:dev
   ```

   `--user 1000:1000` makes the container write as the host user so `workbook.save` can atomically replace files in the bind-mounted directory. Adjust the UID/GID if your host account differs (see the Phase 10 stop-gate in `excel-mcp-server-future-work.md`).

4. **Open the inspector URL** printed to the terminal — typically `http://localhost:6274/?MCP_PROXY_AUTH_TOKEN=<token>`.

5. **Connect** (the inspector picks up the docker command automatically), then browse the tool list.

### Paths and handles when clicking tools

- **File paths are container-local.** The inspector is outside the container; tool arguments are evaluated inside. Host path `./test-data/file1.xlsx` is mounted at `/data/file1.xlsx` (per the `-v` above), so in `workbook.open` you pass `/data/file1.xlsx`, not a host path.
- **Workbook handles** returned by `workbook.open` / `workbook.create` look like `wb-50f0d1e7`. Copy the value verbatim into the `handle` argument of subsequent tools (`range.get`, `workbook.save`, `workbook.close`, …). Handles live for the container's lifetime — a second `docker run` starts fresh.
- Any path outside the sandbox root returns `PATH_OUTSIDE_ROOT`; missing files inside the sandbox return `FILE_NOT_FOUND`. If you see something different, the image is probably stale — rebuild with `--no-cache`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `EXCEL_MCP_ROOT` | unset | If set, every path passed to a tool must resolve inside this subtree. Unset → all paths accepted (developer convenience; warned at startup). |
| `EXCEL_MCP_MAX_FILE_BYTES` | `100000000` (100 MB) | Upper bound on workbook file size at open. |
| `EXCEL_MCP_MAX_CELLS` | `1000000` | Upper bound on total cell count after POI loads the workbook. |
| `LOG_LEVEL` | `INFO` | Logback root level. Accepts `ERROR` / `WARN` / `INFO` / `DEBUG`. |

## v1 limits (by design)

- Formats: `.xlsx`, `.xlsm`, `.xls` are supported. `.xlsb` is **rejected at open** — see `excel-mcp-server-future-work.md` for the calamine / LibreOffice future options.
- Tool surface: 25 tools across workbook lifecycle, range I/O, sheet management, row/col mutation, tables, named ranges, and read-only VBA. No charts, no pivots, no formatting writes, no Power Query / DAX.
- Transport: stdio only. No HTTP / SSE.
- Tenancy: one process per agent session; no per-handle locking or idle-handle eviction (process death is the eviction).
- Formula recalc: POI evaluates ~280 of Excel's ~500+ functions. Post-2019 additions (FILTER, SORT, LAMBDA family, dynamic-array spill, etc.) are not evaluated — use `workbook.capabilities_report` to see what's safe before editing. Detection-and-warn is v1's strategy; a HyperFormula/LibreOffice sidecar is the v1.1 direction.

## Logging

Log output goes **only to stderr** — stdio transport reserves stdout for MCP protocol messages. `System.out` is redirected to stderr at startup as a belt-and-braces measure. POI's log4j output is routed through SLF4J/Logback via `log4j-to-slf4j`.

## Production hardening (expected of the deployment)

These are delegated to whoever owns the container image:

- `--read-only` rootfs with `tmpfs:/tmp`.
- `--network=none` (the MCP itself never needs outbound traffic).
- Memory + CPU caps at the orchestrator level.
- `EXCEL_MCP_ROOT` always set.
