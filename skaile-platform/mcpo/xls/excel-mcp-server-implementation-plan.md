# Excel MCP Server — Implementation Plan (v1)

> **Audience:** an implementation agent (or human developer) building the v1 server.
> **Companion doc:** `excel-mcp-server-skill.md` is the design doc — read it first for the *why*. This doc is the *what* and *how*, concrete enough to implement from.

This plan is built up iteratively. Sections marked **TBD** are pending answers to the foundational questions in the conversation that produced this doc.

---

## 1. Foundational decisions (locked)

These were chosen in early planning. Each one constrains everything below.

### 1.1 Tenancy & transport — **one process per agent session, stdio**

The MCP server runs as a JVM process launched by the agent for the duration of one session, communicating over stdio. No HTTP, no shared multi-tenant state. The "stateful in-memory" picture from the chef ("open the file, keep it loaded across turns, recalc, then flush") works because the *session* is the state — there is no inter-session sharing to lock.

Consequence: no per-handle locking is required (single-threaded request loop), no idle-handle eviction is required (process death is the eviction). HTTP can be added later as a transport without touching tool code.

### 1.2 Path sandboxing — **optional mount root via `EXCEL_MCP_ROOT`**

If the env var `EXCEL_MCP_ROOT` is set, the server validates that every path passed to `workbook.open` / `workbook.create` / `workbook.save` (with explicit destination) resolves inside that subtree, using `Path.toRealPath().startsWith(root.toRealPath())`. If unset, any path is accepted (developer convenience).

Consequence: the production Docker image sets `EXCEL_MCP_ROOT` to the mount point of the agent's working directory. Local dev runs without it. SharePoint / GitHub / Drive sourcing is **out of our scope** — the chef's team mounts those into the container before the MCP starts; we only see local file paths.

### 1.3 Save semantics — **overwrite original, atomic temp + rename**

`workbook.save(handle)` writes to the path the workbook was opened from. To avoid leaving a half-written `.xlsx` on a crash, the implementation writes to a sibling temp file (`<name>.xlsx.tmp-<random>`) and `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` it into place. `workbook.save(handle, path)` saves to the explicit destination using the same atomic pattern. `workbook.save` on a workbook created via `workbook.create()` (no source path) requires an explicit destination.

### 1.4 `.xlsb` policy in v1 — **reject with a clear, future-friendly error**

`workbook.open(path)` and `workbook.create(path)` reject `.xlsb` paths up front. The error message includes the literal text `".xlsb is not supported in v1"` and `"future expansion: calamine read or LibreOffice convert-on-open"` so the agent (and any human reading logs) can surface this as a known TODO. Other formats (`.xlsx`, `.xlsm`, `.xls`) are accepted normally.

Track this in the team's open-todo list as: *"Add `.xlsb` support — pick between calamine sidecar (read) or LibreOffice convert-on-open (read+write)."*

### 1.5 Existing internal code (`xlport-internal`) — **inspiration only, not a fork**

The chef shared an internal Java/POI codebase (`xlport-internal`) for context. Findings:

- It's a Java 8 servlet-based HTTP service for bidirectional Excel ↔ JSON, built around a template/named-range abstraction. Architecture does not match our MCP design.
- No LICENSE file (internal Molnify code). Treat as **read-only reference**, not a code source to copy.
- POI usage is sound and worth borrowing as patterns (not code): `FormulaEvaluator` setup with `IgnoreMissingWorkbooks(true)`, `workbook.setForceFormulaRecalculation(true)`, `removeCalcChain` reflection trick, cached-formula-result-type handling, defensive ISO 8601 date conversion (they had unresolved DST/timezone bugs we should design around).
- POI version: their `5.2.5` is dated. Use the **current stable POI** (5.4.x at time of writing); their own notes mention version-bump bugs.
- Test fixtures in `src/test/resources/test-suites/` cover real-world edge cases (formats, dates, formulas, lookups, data validation, multi-sheet). **Action item:** ask the chef whether a subset can be copied into this project's `src/test/resources/`.

This plan does **not** depend on xlport-internal. The implementation agent should not import or vendor any of its code.

### 1.6 Future-work tracking — implementation agent creates a separate doc

During implementation, the agent **must create** a sibling document `excel-mcp-server-future-work.md` and seed it with everything currently listed in §14 (out of v1). As the build progresses, every "we'll handle this later" judgment call gets appended there with:

- a one-line description of the deferred item,
- the *reason* it was deferred (out of v1 scope, hit a POI quirk, blocked on input from chef, etc.),
- if an implementation idea already came up while deferring, a one-line sketch of it.

This is lower priority than working code but is the single artifact that lets the team pick up post-v1 work without re-discovering everything.

## 2. v1 scope

### 2.1 Tools

v1 ships **25 tools** in 7 categories. All read-only or "structure-aware edit" — no chart/pivot/format manipulation.

| Category | Tools |
|---|---|
| Workbook lifecycle | `workbook.open`, `workbook.create`, `workbook.save`, `workbook.close`, `workbook.recalculate`, `workbook.list_sheets`, `workbook.metadata`, `workbook.capabilities_report` |
| Range I/O | `range.get`, `range.set`, `range.clear` |
| Sheet management | `sheet.create`, `sheet.delete`, `sheet.rename`, `sheet.merged_regions` |
| Row/col mutation | `sheet.insert_rows`, `sheet.delete_rows`, `sheet.insert_cols`, `sheet.delete_cols` |
| Tables | `table.list`, `table.get` |
| Named ranges | `named_range.list`, `named_range.get` |
| VBA (read-only) | `vba.list_modules`, `vba.get_module` |

Out of scope for v1: charts, pivots, formatting (font/colors/borders/conditional), data validation rules, copy_range, sheet copy, Power Query, DAX, sibling MCPs (Word/PowerPoint), HTTP transport, multi-tenant. See §14.

### 2.2 Build sequence

Build in **phases**, in this order. Each phase has a "verify" gate the agent must satisfy before moving to the next. This keeps the system runnable end-to-end at every step instead of accumulating a large unconnected mass of code.

**Phase 0 — Project bootstrap.** `pom.xml` with all pinned dependencies (§3.2), `Dockerfile` (§12.1), Spotless + Logback config, package skeleton matching §4. *Verify:* `mvn package` succeeds; `java -jar` runs the entry point and exits cleanly.

**Phase 1 — Server skeleton with empty tool registry.** `McpServerMain`, `McpServer`, `ToolRegistry`, `ToolDefinition`, plus the leaf packages with no business logic: `error/`, `shape/` (the records, no values yet), `handles/`, `path/`, `config/`, `log/`. *Verify:* an MCP client can connect over stdio; `tools/list` returns an empty array; logs go to stderr only.

**Phase 2 — Engine adapter.** `engine/WorkbookEngine` interface plus `engine/XssfInMemoryEngine` and the POI helpers in `engine/poi/` (`PoiCellReader`, `PoiCellWriter`, `PoiAtomicSaver`, `PoiSizeGuard`). No tools yet. *Verify:* a unit test loads a fixture `simple.xlsx`, reads a known cell via the engine directly, asserts the value.

**Phase 3 — Workbook lifecycle tools (no recalc yet).** `WorkbookOpenTool`, `WorkbookCreateTool`, `WorkbookSaveTool`, `WorkbookCloseTool`, `WorkbookListSheetsTool`, `WorkbookMetadataTool`. Wire them into the registry. *Verify:* end-to-end smoke test through MCP transport: `open` → `list_sheets` → `close` returns expected results.

**Phase 4 — Range I/O.** `RangeGetTool`, `RangeSetTool`, `RangeClearTool`. *Verify:* smoke test: `open` → `range.set` → `save` → reopen the file (new handle) → `range.get` sees the written values.

**Phase 5 — Recalc + capabilities report.** `PoiFormulaEvaluation`, `WorkbookRecalculateTool`, `PoiCapabilityScanner`, `WorkbookCapabilitiesReportTool`. *Verify:* smoke test: write `=SUM(A1:A3)` to a cell, call `workbook.recalculate`, read back the cached value. Run `capabilities_report` against a fixture with `FILTER(...)` in it; assert it shows up under `unsupported_functions_used`.

**Phase 6 — Sheet management.** `SheetCreateTool`, `SheetDeleteTool`, `SheetRenameTool`, `SheetMergedRegionsTool`.

**Phase 7 — Row/column mutation.** `SheetInsertRowsTool`, `SheetDeleteRowsTool`, `SheetInsertColsTool`, `SheetDeleteColsTool`. **Mind the POI footgun in §9.4** (the `shiftColumns` end-of-data check) — copy the defensive pattern.

**Phase 8 — Tables and named ranges.** `TableListTool`, `TableGetTool`, `NamedRangeListTool`, `NamedRangeGetTool`.

**Phase 9 — VBA.** `PoiVbaExtractor`, `VbaListModulesTool`, `VbaGetModuleTool`. Test against an `.xlsm` fixture with a known module.

**Phase 10 — Acceptance + polish.** Walk every §13 acceptance criterion; fix gaps. Write the README. Confirm `excel-mcp-server-future-work.md` was created and seeded per §1.6. Final `mvn verify` clean; Docker image builds and starts under 5 seconds.

### 2.3 When stuck or when the plan is silent

The plan can't anticipate every decision. When you (the implementation agent) need to make a call that isn't covered:

1. **Prefer the simplest option** that satisfies the visible requirements and the §11b code-quality bar.
2. **Mark the decision in code** with an inline comment: `// PLAN-DEFER: <one-line description of what was chosen and why>`.
3. **Append to `excel-mcp-server-future-work.md`** under a section called `## Plan deferrals during implementation` — same one-line description plus pointer to the file.
4. **Do not block** to ask the human user for routine decisions. Keep building.
5. **Surface to the user** *only* when the decision is genuinely architectural — would require substantial rework to change later, or contradicts something in §1, §11b, or §13. In that case, stop, write up the question concisely, and wait.

Examples of what *not* to surface: helper method names, exact log message wording, the order of fields in a record, whether to use a `for` or a `stream`, internal package-private vs private visibility.

Examples of what *to* surface: discovering the MCP Java SDK doesn't expose a feature the plan assumed; finding that POI's `XSSFBReader` is needed earlier than expected; concluding that the four-layer separation in §4.1 conflicts with how the SDK wants to be wired.

## 3. Project setup

### 3.1 Toolchain

- **Java 21 LTS** (Eclipse Temurin). `<source>21</source>`, `<target>21</target>`. Use modern features where they help (records for DTOs, sealed types for tool payloads, pattern matching). Do **not** introduce virtual threads in v1 — single-threaded request loop, no need.
- **Maven** as the build tool. Standard `pom.xml`, no fancy plugins beyond compiler / surefire / shade / spotless.
- **Spotless** plugin for code formatting (Google Java Format) — set up early so style isn't a recurring discussion.
- **Container base**: `eclipse-temurin:21-jre-jammy`. Jammy (Debian/Ubuntu) over Alpine: friendlier later if/when LibreOffice gets added (LibreOffice expects glibc), and POI has occasional musl edge cases.

### 3.2 Dependencies

```
# MCP
io.modelcontextprotocol:mcp                                # official Java MCP SDK
                                                           # confirm exact group/artifact at impl time

# Apache POI — current stable (5.5.x line as of 2026-04)
org.apache.poi:poi:5.5.1
org.apache.poi:poi-ooxml:5.5.1
org.apache.poi:poi-scratchpad:5.5.1                        # for VBAMacroExtractor

# JSON / serialization
com.fasterxml.jackson.core:jackson-databind:2.18.x
com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.x

# Logging
org.slf4j:slf4j-api:2.0.x
ch.qos.logback:logback-classic:1.5.x

# Testing
org.junit.jupiter:junit-jupiter:5.11.x
org.assertj:assertj-core:3.26.x
```

Pin versions explicitly. POI majors break APIs; transitive Jackson updates have caused issues elsewhere.

### 3.3 Distribution

Build a single fat JAR via `maven-shade-plugin` so the container only needs `java -jar excel-mcp.jar`. No WAR, no servlet container.

## 4. Module layout

Concrete file tree. The four-layer separation from §11b is enforced by package boundaries.

```
excel-mcp/
├── pom.xml
├── Dockerfile
├── README.md
├── excel-mcp-server-future-work.md          # created by impl agent (§1.6)
├── src/main/java/com/skaile/excelmcp/
│   ├── McpServerMain.java                   # entry point: parse env, init server, start stdio loop
│   │
│   ├── server/                              # LAYER 1 + 2: transport + dispatch (no Excel logic)
│   │   ├── McpServer.java                   # MCP SDK glue
│   │   ├── ToolRegistry.java                # registers all tools, dispatches calls
│   │   └── ToolDefinition.java              # interface every tool implements
│   │
│   ├── tools/                               # LAYER 3: business logic, one file per tool
│   │   ├── workbook/
│   │   │   ├── WorkbookOpenTool.java
│   │   │   ├── WorkbookCreateTool.java
│   │   │   ├── WorkbookSaveTool.java
│   │   │   ├── WorkbookCloseTool.java
│   │   │   ├── WorkbookRecalculateTool.java
│   │   │   ├── WorkbookListSheetsTool.java
│   │   │   ├── WorkbookMetadataTool.java
│   │   │   └── WorkbookCapabilitiesReportTool.java
│   │   ├── range/
│   │   │   ├── RangeGetTool.java
│   │   │   ├── RangeSetTool.java
│   │   │   └── RangeClearTool.java
│   │   ├── sheet/
│   │   │   ├── SheetCreateTool.java
│   │   │   ├── SheetDeleteTool.java
│   │   │   ├── SheetRenameTool.java
│   │   │   ├── SheetMergedRegionsTool.java
│   │   │   ├── SheetInsertRowsTool.java
│   │   │   ├── SheetDeleteRowsTool.java
│   │   │   ├── SheetInsertColsTool.java
│   │   │   └── SheetDeleteColsTool.java
│   │   ├── table/
│   │   │   ├── TableListTool.java
│   │   │   └── TableGetTool.java
│   │   ├── namedrange/
│   │   │   ├── NamedRangeListTool.java
│   │   │   └── NamedRangeGetTool.java
│   │   └── vba/
│   │       ├── VbaListModulesTool.java
│   │       └── VbaGetModuleTool.java
│   │
│   ├── handles/                             # in-process handle registry
│   │   ├── HandleRegistry.java
│   │   ├── HandleId.java                    # opaque "wb-<8hex>" id type (record)
│   │   └── OpenWorkbook.java                # record holding handle + POI workbook + metadata
│   │
│   ├── engine/                              # LAYER 4: ONLY place that imports POI
│   │   ├── WorkbookEngine.java              # interface — what tools call
│   │   ├── XssfInMemoryEngine.java          # the v1 implementation
│   │   └── poi/                             # POI helper classes (internal to engine layer)
│   │       ├── PoiCellReader.java           # cell -> CellShape conversion
│   │       ├── PoiCellWriter.java           # CellShape -> cell conversion
│   │       ├── PoiFormulaEvaluation.java    # FormulaEvaluator wrapper, recalc logic
│   │       ├── PoiVbaExtractor.java         # VBAMacroExtractor wrapper
│   │       ├── PoiAtomicSaver.java          # write to .tmp + atomic rename
│   │       ├── PoiSizeGuard.java            # pre-load file size + post-load cell count check
│   │       └── PoiCapabilityScanner.java    # FunctionEval lookup + LDT / dynamic-array detection
│   │
│   ├── path/                                # path validation + format whitelist
│   │   ├── PathValidator.java
│   │   ├── ExcelMcpRoot.java                # holds the canonical root (or empty)
│   │   └── FormatWhitelist.java             # extension check, .xlsb rejection message
│   │
│   ├── shape/                               # JSON DTOs (records), shared across tools
│   │   ├── CellShape.java                   # record (a1, type, value, formula, optionalFormatting)
│   │   ├── RangeShape.java
│   │   ├── SheetShape.java
│   │   └── ShapeMapper.java                 # Jackson ObjectMapper config
│   │
│   ├── error/
│   │   ├── ErrorCode.java                   # enum
│   │   ├── McpException.java                # carries ErrorCode + details map
│   │   └── ErrorEnvelope.java               # JSON shape for error responses
│   │
│   ├── config/
│   │   └── ServerConfig.java                # parses EXCEL_MCP_ROOT, EXCEL_MCP_MAX_FILE_BYTES, etc.
│   │
│   └── log/
│       └── LoggingSetup.java                # routes Logback to stderr only
│
└── src/test/
    ├── java/com/skaile/excelmcp/...      # mirrors main/, smoke tests only (§11.2)
    └── resources/fixtures/
        ├── simple.xlsx
        ├── formulas.xlsx
        ├── xlsb-rejection.xlsb
        └── over-cap.xlsx
```

### 4.1 Layer rules (enforced by review, optionally by ArchUnit later)

- `engine/` may import `org.apache.poi.*`, `shape/*`, `error/*`. Nothing else.
- `tools/` may import `engine.WorkbookEngine`, `handles/*`, `shape/*`, `error/*`, `path/*`. **Not** `org.apache.poi.*`.
- `server/` may import `tools/*`, `error/*`. Not `engine/*` or `org.apache.poi.*`.
- `shape/`, `handles/`, `error/`, `path/`, `config/`, `log/` are leaf packages — no upward imports.

## 5. Handle / session model

Single-process, single-session, single-threaded — the MCP request loop processes one tool call at a time. The handle model is therefore much simpler than a multi-tenant design would need.

### 5.1 Handle identity

A handle is an opaque string returned by `workbook.open` or `workbook.create`. Format: `wb-<8-hex-chars>` (e.g. `wb-3f9a1c4d`), generated from `java.util.UUID.randomUUID()` truncated. Opaque to the agent — never reuse, never let the agent invent one.

### 5.2 Handle registry

A simple `Map<String, OpenWorkbook>` field on the server. No locking required (single-threaded loop). No idle TTL eviction in v1 (process death = eviction).

```java
record OpenWorkbook(
    String handle,
    Path sourcePath,        // null if created via workbook.create() with no path
    XSSFWorkbook workbook,
    FormulaEvaluator evaluator,  // lazily created on first recalc/read-with-eval
    Instant openedAt
) {}
```

### 5.3 Lifecycle

- `workbook.open(path)` → load via POI, validate path against `EXCEL_MCP_ROOT`, validate format (reject `.xlsb`), enforce size cap (see §3.1 size limit), create `XSSFWorkbook`, register, return handle.
- `workbook.create(path?)` → new `XSSFWorkbook`, register, return handle. If `path` provided, validate it for later save.
- `workbook.save(handle, path?)` → atomic temp+rename to source path (or explicit `path`). Workbook stays open after save.
- `workbook.close(handle)` → close POI workbook (releases file locks and memory), remove from registry. Calling tools on a closed handle returns a clear error.
- **Process exit** (agent disconnect, shutdown signal): close all open handles cleanly. **Do not auto-save** on shutdown — agents that wanted to save called `save` explicitly; auto-save risks unwanted writes.

### 5.4 Size cap

Enforced in `workbook.open` before POI fully parses:
- Reject if file size > `100 MB` (configurable via `EXCEL_MCP_MAX_FILE_BYTES`).
- After POI loads: reject if total cell count > `1_000_000` (configurable via `EXCEL_MCP_MAX_CELLS`).

Both produce errors that include the offending number and the active limit so the agent can surface a useful message.

### 5.5 Engine seam

`XSSFWorkbook` access is wrapped behind a `WorkbookEngine` interface so a streaming or LibreOffice-backed engine can slot in later as a sibling implementation, not a rewrite. v1 ships exactly one implementation: `XssfInMemoryEngine`. Don't over-design the interface — keep it limited to what v1 tools actually need.

## 6. Path validation

Centralized in a single `PathValidator` class. Every tool that takes a path parameter routes through it.

### 6.1 Rules

1. Resolve to a real path: `Path.of(input).toAbsolutePath().normalize()`.
2. Reject if the path contains a NUL byte (Java filesystem APIs reject these but the error is poor — catch early).
3. If `EXCEL_MCP_ROOT` is set:
   - Resolve `EXCEL_MCP_ROOT` once at startup to its canonical real path.
   - For each path: `realPath.startsWith(rootCanonical)` must be true. Use `Path.toRealPath()` only on the parent if the file doesn't exist yet (e.g. `workbook.create(path)`); reject if even the parent doesn't resolve under the root.
4. If `EXCEL_MCP_ROOT` is unset: skip the containment check. Log this once at startup as `WARN: EXCEL_MCP_ROOT not set; path sandboxing disabled`.

### 6.2 Format validation (separate concern)

After path validation succeeds, check the file extension (lowercase) against a whitelist:

- `.xlsx`, `.xlsm`, `.xls` → accepted
- `.xlsb` → rejected with the future-friendly error from §1.4
- anything else → rejected with `unsupported file extension: <ext>`

Format check is by extension only in v1 — no magic-byte sniffing. POI will catch genuine format mismatches at load time and we surface that error.

### 6.3 Error shape

Path errors are part of the unified error envelope (§8) with `code: "PATH_INVALID"`, `code: "PATH_OUTSIDE_ROOT"`, or `code: "FORMAT_UNSUPPORTED"` and a `details` field naming the offending path and the active root (if any).

## 7. JSON return-shape conventions

All shapes are Jackson-serialized Java records living in `shape/`. Field names are `snake_case` in JSON (Jackson `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)`). Records are camelCase in Java.

### 7.1 `CellShape`

```jsonc
{
  "a1": "C2",                  // always present
  "type": "number",            // string | number | boolean | date | error | blank | formula_uncomputed
  "value": 150,                // typed by `type`; null if blank
  "formula": "=B2*1.5"         // typed formula text if cell has one, else null
  // "formatting": { … }       // ONLY present when include_formatting=true was requested
}
```

**`type` semantics:**
- `string` → `value` is a JSON string.
- `number` → `value` is a JSON number (integer or float — preserve POI's distinction).
- `boolean` → `value` is `true` / `false`.
- `date` → `value` is an ISO 8601 string (`YYYY-MM-DDTHH:mm:ss[.SSS]`, no timezone — Excel dates are naive). Detected via `DateUtil.isCellDateFormatted(c)`.
- `error` → `value` is the Excel error string (`"#REF!"`, `"#DIV/0!"`, `"#NAME?"`, etc.) using `ErrorEval.valueOf(c.getErrorCellValue()).getErrorString()`.
- `blank` → `value` is `null`, `formula` is `null`. Used when the cell has neither value nor formula.
- `formula_uncomputed` → cell has a `formula` but no cached result yet (e.g. just-set, before `recalculate`). `value` is `null`.

**`formatting` field** (only when requested via `include_formatting: true` on the read tool):

```jsonc
"formatting": {
  "number_format": "0.00%",
  "font": {"bold": false, "italic": false, "color": "#000000", "size": 11, "name": "Calibri"},
  "fill_color": null,
  "horizontal_alignment": "general",   // general|left|center|right
  "wrap_text": false
}
```

Keep this minimal in v1 — these are the fields most agents actually inspect. Add more on demand.

### 7.2 `RangeShape`

```jsonc
{
  "sheet": "Sheet1",
  "range": "A1:C2",
  "rows": 2,
  "cols": 3,
  "cells": [
    [ {CellShape}, {CellShape}, {CellShape} ],
    [ {CellShape}, {CellShape}, {CellShape} ]
  ]
}
```

`cells` is a row-major 2D array. Always rectangular; blank cells are present as `CellShape` with `type: "blank"`.

### 7.3 Other shapes

```jsonc
// SheetShape (used in workbook.list_sheets)
{ "name": "Sheet1", "index": 0, "is_hidden": false }

// WorkbookMetadataShape (used in workbook.metadata)
{
  "filename": "report.xlsx",
  "size_bytes": 12345,
  "modified": "2026-04-16T13:42:11Z",
  "format": "xlsx",                       // xlsx | xlsm | xls
  "sheets": [ {SheetShape}, … ],
  "named_ranges": [ {name, sheet, range, scope} ],
  "tables": [ {name, sheet, range} ]
}

// MergedRegionShape (used in sheet.merged_regions)
{ "range": "B2:D4" }

// VbaModuleShape (used in vba.list_modules)
{ "name": "Module1", "type": "module" }   // module | class | document | form

// VbaModuleSourceShape (used in vba.get_module)
{ "name": "Module1", "type": "module", "source": "Sub Hello()\n  ...\nEnd Sub" }
```

### 7.4 Pagination for large ranges

`range.get` accepts an optional `max_cells` parameter (default `10000`). If the requested range exceeds this, the server returns the first `max_cells` cells *and* a top-level `truncated: true` flag with `total_cells: N`. The agent can re-request with explicit smaller bounds. v1 does not implement cursors — pagination is "request a smaller range."

### 7.5 Read-side input shape

Reads accept either:

- A range string: `"A1:C10"` or `"C5"` (single cell).
- An expanded form: `{"start": "A1", "end": "C10"}`.

Internally normalized to `RangeAddress(sheet, startRow, startCol, endRow, endCol)` in `shape/`.

## 8. Error model

### 8.1 Envelope

Errors are returned via the MCP error mechanism (not as a successful result). The error payload is:

```jsonc
{
  "code": "RANGE_OUT_OF_BOUNDS",         // ErrorCode enum value
  "message": "Range D1:D10 is outside sheet 'Sheet1' (max column C, max row 5)",
  "details": {                           // free-form, code-specific
    "handle": "wb-3f9a1c4d",
    "sheet": "Sheet1",
    "requested": "D1:D10",
    "sheet_max": "C5"
  }
}
```

Always include `code` and `message`. `details` carries code-specific structured info — keep it small but useful (handle id, the offending input, the active limit, etc.). Agents will use `code` for branching and surface `message` to the human.

### 8.2 Initial code set

```
// Path / format
PATH_INVALID                  // unparseable, NUL byte, etc.
PATH_OUTSIDE_ROOT             // EXCEL_MCP_ROOT set and path escapes it
FORMAT_UNSUPPORTED            // .xlsb (with future-friendly text), unknown extension
FILE_NOT_FOUND                // path resolves but no such file

// Size / resource limits
WORKBOOK_TOO_LARGE            // file size exceeds EXCEL_MCP_MAX_FILE_BYTES
WORKBOOK_TOO_MANY_CELLS       // post-load cell count exceeds EXCEL_MCP_MAX_CELLS

// Handles
HANDLE_UNKNOWN                // tool called with handle not in registry
HANDLE_CLOSED                 // handle existed but was already closed

// Worksheet / range
SHEET_NOT_FOUND
SHEET_ALREADY_EXISTS          // sheet.create with conflicting name
RANGE_INVALID                 // unparseable A1 reference
RANGE_OUT_OF_BOUNDS           // range exceeds sheet bounds (read or write)
ROW_INDEX_INVALID             // for sheet.insert_rows / delete_rows
COLUMN_INDEX_INVALID          // for sheet.insert_cols / delete_cols

// Formulas
FORMULA_INVALID               // cannot be parsed by POI's formula parser
FORMULA_EVAL_FAILED           // recalc threw (often a feature POI's evaluator doesn't support)

// VBA
VBA_NOT_PRESENT               // vba.* called on a workbook without VBA project
VBA_MODULE_NOT_FOUND

// Tables / named ranges
TABLE_NOT_FOUND
NAMED_RANGE_NOT_FOUND

// Save
SAVE_FAILED                   // I/O error during atomic save
SAVE_REQUIRES_PATH            // workbook.save called on a created (no source) workbook with no path arg

// Catch-all
INTERNAL_ERROR                // unexpected exception; details include the exception class name (not stack)
```

### 8.3 Conventions

- `ErrorCode` is a `public enum`. Add new codes by extending it; document each new code in the README error table.
- `McpException` is a checked exception in `error/` carrying `ErrorCode` + `details` map. Tools throw it; the dispatch layer in `server/` catches and translates to the MCP error response.
- `INTERNAL_ERROR` is the only code where the agent shouldn't expect a stable shape — it's the fallback. Log the full stack at `ERROR` level when it fires; surface only the exception class name in `details.exception`.
- Never return error info inside a successful response. If something failed, raise `McpException`.

## 9. v1 tool contracts

Each tool below specifies: input shape, success output shape, the error codes it can produce, the POI implementation pointer, and at least one golden example. JSON examples are illustrative — the canonical schemas are the Java records.

### 9.1 Workbook lifecycle

#### `workbook.open`

**Input:** `{ "path": "/data/report.xlsx" }`
**Output:** `{ "handle": "wb-3f9a1c4d", "format": "xlsx", "sheet_count": 3 }`
**Errors:** `PATH_INVALID`, `PATH_OUTSIDE_ROOT`, `FORMAT_UNSUPPORTED` (incl. `.xlsb` rejection), `FILE_NOT_FOUND`, `WORKBOOK_TOO_LARGE`, `WORKBOOK_TOO_MANY_CELLS`, `INTERNAL_ERROR`
**POI:** `WorkbookFactory.create(file)` then cast to `XSSFWorkbook` (or `HSSFWorkbook` for `.xls`). Wrap pre-load size check in `PoiSizeGuard`.
**Example:**
```jsonc
→ {"path": "/data/Q1.xlsx"}
← {"handle": "wb-3f9a1c4d", "format": "xlsx", "sheet_count": 3}
```

#### `workbook.create`

**Input:** `{ "path": "/data/new.xlsx" }` (path optional; if omitted, workbook has no source path until `save(handle, path)`)
**Output:** `{ "handle": "wb-7b2e9c11" }`
**Errors:** `PATH_INVALID`, `PATH_OUTSIDE_ROOT`, `FORMAT_UNSUPPORTED`
**POI:** `new XSSFWorkbook()`. If a path is given, validate it but don't write the file yet.

#### `workbook.save`

**Input:** `{ "handle": "wb-…", "path": "/data/out.xlsx" }` (path optional; defaults to source path)
**Output:** `{ "saved_to": "/data/Q1.xlsx", "size_bytes": 14823 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `PATH_INVALID`, `PATH_OUTSIDE_ROOT`, `FORMAT_UNSUPPORTED`, `SAVE_REQUIRES_PATH`, `SAVE_FAILED`
**POI:** delegate to `PoiAtomicSaver`: write to `<dest>.tmp-<random>`, then `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`. Workbook stays open after save.

#### `workbook.close`

**Input:** `{ "handle": "wb-…" }`
**Output:** `{ "closed": true }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`
**POI:** `workbook.close()` then remove from registry.

#### `workbook.recalculate`

**Input:** `{ "handle": "wb-…" }`
**Output:** `{ "evaluated_cells": 142 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `FORMULA_EVAL_FAILED`
**POI:** lazily build `FormulaEvaluator` (cache on `OpenWorkbook`), call `evaluator.setIgnoreMissingWorkbooks(true)`, then `workbook.setForceFormulaRecalculation(true)` and `evaluator.evaluateAll()`. Wrap in try/catch; on POI throw, raise `FORMULA_EVAL_FAILED` with the cell address (best-effort) in `details`.

#### `workbook.list_sheets`

**Input:** `{ "handle": "wb-…" }`
**Output:** `{ "sheets": [ {SheetShape}, … ] }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`
**POI:** iterate `workbook.sheetIterator()` with index; check `workbook.isSheetHidden(i)`.

#### `workbook.metadata`

**Input:** `{ "handle": "wb-…", "include_named_ranges": true, "include_tables": true }` (both default `true`)
**Output:** `{WorkbookMetadataShape}`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`
**POI:** `workbook.getAllNames()` for named ranges; iterate sheets and call `XSSFSheet.getTables()` for tables.

#### `workbook.capabilities_report`

The agent calls this **before editing** to learn what POI can and cannot evaluate in the loaded workbook. It exists because POI's formula coverage is ~280 of Excel's ~500+ functions and several modern features (Linked Data Types, dynamic arrays) are structurally unsupported. Returning a clear inventory lets the agent reason about safe edits instead of discovering breakage at recalc time.

**Input:** `{ "handle": "wb-…" }`
**Output:**
```jsonc
{
  "poi_version": "5.5.1",
  "supported_function_count": 281,                 // FunctionEval.getSupportedFunctionNames().size()
  "functions_used": ["SUM", "VLOOKUP", "XLOOKUP", "FILTER", "SORT"],
  "unsupported_functions_used": [
    {"name": "FILTER", "count": 3, "sample_cells": ["Sheet1!D2", "Sheet1!D3", "Sheet2!A10"]},
    {"name": "LAMBDA", "count": 1, "sample_cells": ["Sheet1!Z1"]}
  ],
  "has_linked_data_types": false,                  // /xl/richData/* parts present in OPC package
  "has_dynamic_array_formulas": true,              // any cm="1" markers found on cells
  "has_legacy_array_formulas": false,              // CSE {=...} formulas
  "has_vba": true,
  "warnings": [
    "Workbook uses FILTER (3 cells) and LAMBDA (1 cell). POI cannot recalculate these — values will be stale until the workbook is opened in Excel 365 or LibreOffice 7.5+.",
    "Workbook contains dynamic-array formulas (cm=\"1\" markers). POI cannot recompute spilled ranges; treat the cached values as read-only."
  ]
}
```

**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`

**POI implementation** (`PoiCapabilityScanner`):
- `supported_function_count`: `FunctionEval.getSupportedFunctionNames().size()`.
- `functions_used` + `unsupported_functions_used`: walk every cell with a formula, parse function names. Two implementation paths:
  - *Cheap & adequate*: regex `\b[A-Z][A-Z0-9.]*(?=\()` on `cell.getCellFormula()` text. Some false positives (matches `IF(` inside text constants) but good enough for an inventory tool.
  - *Correct*: use `FormulaParser.parse(formula, fpb, FormulaType.CELL, sheetIndex)` and walk the returned `Ptg[]` for `FuncPtg` / `FuncVarPtg` instances; call `FunctionMetadataRegistry.getFunctionByName(...)`. More work, no false positives.
  - **Pick the cheap path for v1**, document false-positive caveat in the JSON response, upgrade to parser-based later if it matters.
  - Cross-reference each found name against `FunctionEval.getSupportedFunctionNames()` to populate the unsupported list.
  - For each unsupported function, collect up to 5 sample cell addresses (cap to keep response small).
- `has_linked_data_types`: `OPCPackage.getPartsByContentType(...)` or list parts under `/xl/richData/` prefix. Presence = LDT cells exist somewhere.
- `has_dynamic_array_formulas`: scan worksheet XML for `cm="1"` attributes on `<f>` elements. POI doesn't expose this in its high-level API, so this requires reaching into the underlying XML (`XSSFSheet.getCTWorksheet()` and walking row → cell → formula nodes). Document the workaround in the implementation.
- `has_legacy_array_formulas`: `cell.isPartOfArrayFormulaGroup()` while iterating cells.
- `has_vba`: check `workbook.getCTWorkbook()` or look for `xl/vbaProject.bin` part in the OPC package.
- `warnings`: derived from the booleans above and the unsupported-functions list. Keep messages short and actionable.

**Performance**: this tool is O(cells with formulas), not O(all cells). On a 10k-formula workbook it should complete in well under a second. If it ever becomes a hot path, cache the result against workbook hash.

### 9.2 Range I/O

#### `range.get`

**Input:**
```jsonc
{
  "handle": "wb-…",
  "sheet": "Sheet1",
  "range": "A1:C10",                 // OR {"start": "A1", "end": "C10"}
  "include_formatting": false,        // default false
  "max_cells": 10000                  // default 10000
}
```
**Output:** `{RangeShape}` — possibly augmented with `"truncated": true, "total_cells": N` if `max_cells` was exceeded.
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `RANGE_INVALID`, `RANGE_OUT_OF_BOUNDS`
**POI:** iterate cells via `sheet.getRow(r).getCell(c)`; convert each via `PoiCellReader.read(cell)` (handles type detection + cached formula result). Pre-validate bounds against `sheet.getLastRowNum() + 1` and `sheet.getRow(...).getLastCellNum()`.

#### `range.set`

**Input:**
```jsonc
{
  "handle": "wb-…",
  "sheet": "Sheet1",
  "range": "A1:B2",                   // optional — inferred from values dimensions if omitted
  "start": "A1",                      // alternative: just specify start, infer extent from values
  "values": [["Name", "Q1"], ["Acme", 100]],   // 2D, row-major
  "formulas": [[null, null], [null, "=B2*1.5"]] // optional; same shape as values; nulls mean "use value"
}
```
Either `range` or `start` is required. `values` is required. `formulas` is optional (sparse cells allowed via nulls).

**Output:** `{ "written_cells": 4 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `RANGE_INVALID`, `RANGE_OUT_OF_BOUNDS`, `FORMULA_INVALID`
**POI:** `PoiCellWriter.write(cell, value, formulaOrNull)`: if a formula is present, `cell.setCellFormula(...)`; else set value via the type-appropriate setter. Strings, numbers, booleans handled; dates expected as ISO strings, parsed and written via `cell.setCellValue(java.util.Date)`. Setting a formula does **not** auto-evaluate — caller must follow with `workbook.recalculate`.

#### `range.clear`

**Input:** `{ "handle": "wb-…", "sheet": "Sheet1", "range": "A1:C10" }`
**Output:** `{ "cleared_cells": 30 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `RANGE_INVALID`, `RANGE_OUT_OF_BOUNDS`
**POI:** for each non-null cell in the range, `row.removeCell(cell)`. Don't touch styling.

### 9.3 Sheet management

#### `sheet.create`

**Input:** `{ "handle": "wb-…", "name": "Summary" }` (optional `index` to insert at a position; default end)
**Output:** `{SheetShape}`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_ALREADY_EXISTS`
**POI:** `workbook.createSheet(name)`; if `index` given, `workbook.setSheetOrder(name, index)`.

#### `sheet.delete`

**Input:** `{ "handle": "wb-…", "name": "Old" }`
**Output:** `{ "deleted": true }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`
**POI:** `workbook.removeSheetAt(workbook.getSheetIndex(name))`.

#### `sheet.rename`

**Input:** `{ "handle": "wb-…", "old_name": "Sheet1", "new_name": "Q1Data" }`
**Output:** `{ "old_name": "Sheet1", "new_name": "Q1Data" }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `SHEET_ALREADY_EXISTS`
**POI:** `workbook.setSheetName(workbook.getSheetIndex(old), new)`.

#### `sheet.merged_regions`

**Input:** `{ "handle": "wb-…", "sheet": "Sheet1" }`
**Output:** `{ "merged_regions": [ {"range": "B2:D4"}, {"range": "F1:F3"} ] }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`
**POI:** `sheet.getMergedRegions()` → `List<CellRangeAddress>` → format each via `CellRangeAddress.formatAsString()`.

### 9.4 Row & column mutation

⚠️ **Known POI footgun** (per xlport notes): `XSSFSheet.shiftColumns` throws `IllegalArgumentException` ("firstMovedIndex, lastMovedIndex out of order") when shifting at the last column. Mirror xlport's defensive check: only call `shiftColumns` if not already at the end.

#### `sheet.insert_rows`

**Input:** `{ "handle": "wb-…", "sheet": "Sheet1", "start_row": 5, "count": 3 }` (1-based row index)
**Output:** `{ "inserted_at": 5, "count": 3 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `ROW_INDEX_INVALID`
**POI:** `sheet.shiftRows(startRow0Based, sheet.getLastRowNum(), count)` then create empty rows at the inserted indices if needed.

#### `sheet.delete_rows`

**Input:** `{ "handle": "wb-…", "sheet": "Sheet1", "start_row": 5, "count": 3 }`
**Output:** `{ "deleted_at": 5, "count": 3 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `ROW_INDEX_INVALID`
**POI:** remove rows in the range, then `sheet.shiftRows(startRow0Based + count, sheet.getLastRowNum(), -count)`. Only shift if not at the end of data.

#### `sheet.insert_cols`

**Input:** `{ "handle": "wb-…", "sheet": "Sheet1", "start_col": 3, "count": 1 }` (1-based, A=1)
**Output:** `{ "inserted_at": 3, "count": 1 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `COLUMN_INDEX_INVALID`
**POI:** `XSSFSheet.shiftColumns(startCol0Based, lastFilledCol, count)` with the defensive end-of-data check.

#### `sheet.delete_cols`

**Input:** `{ "handle": "wb-…", "sheet": "Sheet1", "start_col": 3, "count": 1 }`
**Output:** `{ "deleted_at": 3, "count": 1 }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `SHEET_NOT_FOUND`, `COLUMN_INDEX_INVALID`
**POI:** mirror xlport's `Utils.removeColumn` pattern: remove cells in column, then `shiftColumns` only if not at end.

### 9.5 Tables

#### `table.list`

**Input:** `{ "handle": "wb-…" }`
**Output:** `{ "tables": [ {"name": "tblSales", "sheet": "Sheet1", "range": "A1:E50"}, … ] }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`
**POI:** iterate sheets, `XSSFSheet.getTables()`, read `XSSFTable.getName()` and `XSSFTable.getArea().formatAsString()`.

#### `table.get`

**Input:** `{ "handle": "wb-…", "name": "tblSales", "include_formatting": false, "max_cells": 10000 }`
**Output:** `{RangeShape}` augmented with `"table_name": "tblSales"`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `TABLE_NOT_FOUND`
**POI:** find the `XSSFTable` by name across sheets, use its `AreaReference` to delegate to the same range-read path.

### 9.6 Named ranges

#### `named_range.list`

**Input:** `{ "handle": "wb-…" }`
**Output:** `{ "named_ranges": [ {"name": "Total", "sheet": "Sheet1", "range": "F100", "scope": "workbook"}, … ] }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`
**POI:** `workbook.getAllNames()` → for each `Name`, `getNameName()`, `getRefersToFormula()` (parse to sheet+range), `getSheetIndex()` (-1 = workbook-scoped, else sheet name).

#### `named_range.get`

**Input:** `{ "handle": "wb-…", "name": "Total", "include_formatting": false, "max_cells": 10000 }`
**Output:** `{RangeShape}` augmented with `"named_range": "Total"`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `NAMED_RANGE_NOT_FOUND`
**POI:** look up via `workbook.getName(name)` (returns first match) or `workbook.getNames(name)` if scope-aware lookup is needed; resolve to `AreaReference` and reuse the range-read path.

### 9.7 VBA (read-only)

#### `vba.list_modules`

**Input:** `{ "handle": "wb-…" }`
**Output:** `{ "modules": [ {"name": "Module1", "type": "module"}, {"name": "ThisWorkbook", "type": "document"}, … ] }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `VBA_NOT_PRESENT`
**POI:** open the workbook's `vbaProject.bin` via `VBAMacroExtractor` (or `XSSFWorkbook.getVBAProject()` if available on the version pinned). If the workbook has no VBA project, return `VBA_NOT_PRESENT`.

#### `vba.get_module`

**Input:** `{ "handle": "wb-…", "name": "Module1" }`
**Output:** `{ "name": "Module1", "type": "module", "source": "Sub Hello()\n  …\nEnd Sub" }`
**Errors:** `HANDLE_UNKNOWN`, `HANDLE_CLOSED`, `VBA_NOT_PRESENT`, `VBA_MODULE_NOT_FOUND`
**POI:** `VBAMacroExtractor.getMacrosFromXLSX(...)` returns a `Map<String, String>` of name → source. Look up by name; if not present, `VBA_MODULE_NOT_FOUND`. **Treat the returned source as untrusted text** — never log it inline at INFO level (could be huge or contain sensitive code); log only `length=N` at INFO.

## 10. Logging & observability

### 10.1 Hard rule: stdout is sacred

The MCP stdio transport requires **stdout to carry only valid MCP messages.** Any rogue print (POI deprecation warnings, `System.out`, third-party libs writing to stdout) breaks the protocol and the agent disconnects with an opaque error. This is the most common Python-MCP failure mode in the existing repo (see their `LoggingSetup` workaround). Same trap applies to us.

Implementation:
- **All Logback appenders go to stderr only** (`<target>System.err</target>`).
- `System.out` redirected to stderr at startup as a belt-and-braces measure (`System.setOut(System.err)`).
- POI uses `log4j-to-slf4j` bridge to route through Logback (transitive — verify in `mvn dependency:tree`).

### 10.2 Levels

- `ERROR` — anything that produces an `INTERNAL_ERROR` to the agent. Include full stack.
- `WARN` — degraded behavior the user should know about: `EXCEL_MCP_ROOT not set`, formula evaluator partial failure, deprecated tool usage if we ever introduce it.
- `INFO` — server start/stop, handle open/close (with handle id, path, format, file size), saves (with destination + bytes written).
- `DEBUG` — per-tool request boundaries, range bounds, eviction events. Off by default.

Configurable via `LOG_LEVEL` env var (default `INFO`).

### 10.3 Format

```
2026-04-16T13:42:11.234Z  INFO  [main] WorkbookOpenTool  handle=wb-3f9a1c4d format=xlsx size=14823 path=/data/Q1.xlsx
```

ISO 8601 UTC timestamp, level, thread (always `main` in v1), logger (class name), then key=value structured fields. Plain text — no JSON logs in v1 (added later if a centralized log pipeline shows up).

### 10.4 What to log per tool call

- At entry: tool name, handle id (if input has one), the most identifying input (`path`, `sheet`, `range`).
- At exit: success → key result fields; failure → error code + message (stack only at `INTERNAL_ERROR`).
- Never log full file contents. Never log VBA source. Never log full range contents. Sizes and counts only.

### 10.5 Metrics — out of v1

No Prometheus, no OpenTelemetry, no metrics endpoint. Add later only if the deployment infra has somewhere to send them. Track on the future-work doc.

## 11. Test plan

**Tests are a means to the goal, not the goal itself.** The goal is a working server with structure that makes future changes safe. Aim for *enough* tests to anchor the contract and catch obvious regressions, then stop.

### 11.1 What we actually want

- **A handful of smoke tests** that prove the golden path works end to end (open → read → write → recalc → save → close).
- **A regression test** for every bug we hit during development (so it doesn't come back).
- **No coverage targets, no exhaustive matrix.** If a piece of code is hard to test, that's signal — usually it means the structure should change, not that we need more test infrastructure.

### 11.2 Concrete v1 test list (small on purpose)

**Engine smoke tests** (`XssfInMemoryEngine`):
- Open a fixture `.xlsx`, read a known range, assert values.
- Create a workbook in-memory, write a range, save, reopen, assert values match.
- Write a formula, recalculate, assert the cached value.
- Save with simulated mid-write failure leaves source file intact (atomic-rename test).

**Validation smoke tests:**
- `PathValidator` rejects a path outside `EXCEL_MCP_ROOT` when set; accepts when unset.
- `.xlsb` rejection produces the documented error code.
- Size cap rejects an oversize fixture at open time.

**Integration smoke test:**
- One end-to-end flow over the in-memory MCP transport: open → range.get → range.set → recalculate → range.get (sees new value) → save → close.

That's roughly 8-10 tests total for v1. Add more only when bugs surface.

### 11.3 Fixtures

- A tiny `src/test/resources/fixtures/` directory with: `simple.xlsx`, `formulas.xlsx`, `xlsb-rejection.xlsb`, `over-cap.xlsx`.
- **Optional later**: ask the chef whether a curated subset of `xlport-internal/src/test/resources/test-suites/` can be copied for richer fixtures. Useful when adding date/format/lookup edge cases — not blocking for v1.

### 11.4 Explicitly NOT tested in v1

- Coverage as a metric.
- Charts / pivots / VBA / PQ / DAX (not in v1 scope — see §14).
- Concurrency, performance, large workbooks, LibreOffice paths.

## 11b. Code quality & maintainability bar

This is what "production-shaped" actually means here. The implementation agent should treat these as hard constraints, not nice-to-haves.

### 11b.1 Structural

- **One tool per file.** Don't dump every `@Tool` handler into a 900-line server.java (the existing Python repo's mistake). Each tool's input parsing, validation, dispatch, and response shaping lives in its own small file under `tools/`.
- **Separate transport / dispatch / business logic / engine.** Four clear layers:
  1. Transport (MCP SDK glue) — knows nothing about Excel.
  2. Tool dispatch — routes typed payloads to handlers.
  3. Business logic in `tools/` — handles validation, builds responses, calls the engine.
  4. Engine adapter (`engine/`) — only place that imports POI.
  No POI imports outside the engine layer. No MCP imports inside the engine layer.
- **Engine seam** (`WorkbookEngine` interface) is real, not nominal. v1 has one impl; the interface should make the second impl obviously addable without rewriting the tool layer.
- **DTOs are records.** Tool inputs and outputs use `record` types with explicit fields, not `Map<String,Object>`.
- **No static state.** Server holds the handle registry; everything else is constructor-injected. Easy to instantiate in tests, easy to extend.

### 11b.2 Naming & idioms

- Modern Java 21 (records, sealed types, pattern matching, `Optional` where appropriate).
- Clear, full names — `WorkbookHandle` not `WBH`, `RangeAddress` not `Addr`.
- Functions stay small. If a tool handler is over ~60 lines, factor.
- Comments only where the *why* is non-obvious. Don't narrate what the code does.

### 11b.3 Extension points (must be obvious from the code)

A new contributor should be able to find these without asking:
- **Add a new tool**: drop a file in `tools/`, register it in one place.
- **Add a new engine** (LibreOffice, Aspose, streaming): implement `WorkbookEngine`, wire selection via env var.
- **Add a new file format**: extend the format whitelist + format dispatcher in `path/`.
- **Add a new error code**: extend the `ErrorCode` enum, document in the README's error table.

### 11b.4 Tooling that enforces the bar

- **Spotless** with Google Java Format — formatting non-negotiable, runs in CI.
- **Spotbugs** (optional but recommended given xlport used findsecbugs) — catches obvious null/resource bugs.
- `mvn verify` runs format check + Spotbugs + tests. CI runs `mvn verify` on every push.
- Logback config in repo so log format is consistent across dev and prod.

### 11b.5 What this rules out

- One-giant-file architecture.
- POI types leaking into the tool layer or response shapes.
- Mutable singletons.
- Stringly-typed tool inputs (`Map<String, Object>` for payloads).
- "TODO: handle this later" littered through error paths.

## 12. Dockerfile + run instructions

### 12.1 Dockerfile

Multi-stage build: heavy JDK + Maven for compilation, slim JRE for runtime.

```dockerfile
# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q -e dependency:go-offline
COPY src ./src
RUN mvn -B -q -e -DskipTests package
# Output: /build/target/excel-mcp-<version>.jar (shaded fat jar)

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-jammy

# Non-root user
RUN useradd --uid 10001 --create-home --shell /sbin/nologin appuser

# App layout
WORKDIR /app
COPY --from=build /build/target/excel-mcp-*.jar /app/excel-mcp.jar

# Healthcheck stub (stdio transport — there's no HTTP endpoint to ping).
# Optional: a `--healthcheck` CLI flag could be added later.

USER 10001
ENV LOG_LEVEL=INFO
# EXCEL_MCP_ROOT, EXCEL_MCP_MAX_FILE_BYTES, EXCEL_MCP_MAX_CELLS:
#   set by the deployment, not baked in.

ENTRYPOINT ["java", "-jar", "/app/excel-mcp.jar"]
```

Image size target: under 250 MB (JRE alone is ~150 MB).

### 12.2 Build & run

```bash
# Build
docker build -t excel-mcp:dev .

# Run locally with a mounted directory as the sandbox root
docker run --rm -i \
  -v "$(pwd)/data:/data:rw" \
  -e EXCEL_MCP_ROOT=/data \
  -e LOG_LEVEL=INFO \
  excel-mcp:dev
# stdio is open on stdin/stdout; an MCP client can connect.

# Run locally without sandboxing (dev only, accepts any path the agent gives)
docker run --rm -i excel-mcp:dev
```

### 12.3 Suggested production hardening (delegated to whoever owns the image)

These aren't this project's responsibility but worth noting in the README so the deployment team knows what we expect:

- `--read-only` rootfs with `tmpfs:/tmp`.
- `--network=none` (the MCP itself never needs outbound traffic).
- Memory + CPU caps via the orchestrator (`--memory=2g`, etc.).
- `EXCEL_MCP_ROOT` always set in production.

### 12.4 Connecting an MCP client (example)

For a client using the standard MCP stdio transport descriptor:

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

## 13. Acceptance criteria

v1 is "done" when **all** of the following are true:

### 13.1 Functional

- An MCP-speaking agent can connect via stdio to the running server.
- All v1 tools listed in §9 are exposed with the documented signatures and return shapes.
- The full golden-path sequence works end to end: `workbook.open` → `range.get` → `range.set` → `workbook.recalculate` → `range.get` (returns recalculated value) → `workbook.save` → `workbook.close`.
- `.xlsx`, `.xlsm`, `.xls` open and save round-trip cleanly without losing data.
- `.xlsb` is rejected at open with the documented error.
- Atomic save: killing the process mid-save leaves the source file intact.
- Path validation works in both `EXCEL_MCP_ROOT`-set and unset modes.

### 13.2 Quality

The bar is **structural**, not coverage-based. v1 ships when:

- Code structure matches §11b: one tool per file, four layers cleanly separated, engine seam real, DTOs as records, no static state.
- POI imports appear *only* under `engine/`. MCP imports do not appear inside `engine/`.
- The four extension points in §11b.3 are findable in the code without asking the original author.
- The smoke tests in §11.2 all pass.
- `mvn verify` is clean (compile + format check + Spotbugs + the smoke tests).
- Logs go to stderr (stdio transport requires stdout to be MCP-only).
- No `System.out.println` anywhere in production code; no `printStackTrace`.

### 13.3 Operational

- Single fat JAR builds via `mvn package`.
- Docker image builds and starts; `java -jar` boots the server in under 5 seconds.
- README documents: how to build, how to run locally (with and without `EXCEL_MCP_ROOT`), how to connect an MCP client, the env vars (`EXCEL_MCP_ROOT`, `EXCEL_MCP_MAX_FILE_BYTES`, `EXCEL_MCP_MAX_CELLS`, log level), and the v1 limits (no `.xlsb`, no charts, no VBA, etc.).

### 13.4 Explicitly NOT required for v1

- HTTP transport (stdio only).
- Multi-tenant / shared-server hardening.
- Performance benchmarks.
- Sibling MCPs (Word, PowerPoint).
- Optional expansions (LibreOffice, PowerQueryViewer, PBIXRay, calamine, Aspose).
- Anything in §14.

## 14. Explicitly out of v1

The implementation agent must **seed `excel-mcp-server-future-work.md` with this list** at project start (per §1.6) and append to it as deferral decisions come up during the build.

### 14.1 Format & engine

- **`.xlsb` read or write.** Rejected at open with future-friendly error. Future option A: calamine sidecar binary for read. Future option B: LibreOffice convert-on-open + write back as `.xlsx`.
- **Streaming engine** for very large workbooks (>100 MB / >1M cells). Future: second `WorkbookEngine` impl using SXSSF (write) + XSSF SAX EventModel (read), gated by a per-workbook size heuristic.
- **LibreOffice / UNO bridge** for chart rendering, PDF/image export, and full-fidelity recalc on functions POI's evaluator can't handle (see §14.1a). Future: ship as a sidecar process, MCP shells out per call.
- **Aspose.Cells fallback** for fidelity gaps neither POI nor LibreOffice cover. Commercial license — only pursue if a real need surfaces.

### 14.1a Formula coverage gap (the biggest known limitation)

POI's `FormulaEvaluator` implements ~280 of Excel's ~500+ functions. Everything Microsoft has added since Excel 2019 is missing. v1 strategy: **detect via `workbook.capabilities_report` and rely on recalc-on-open** (Excel/LibreOffice fixes stale values when a human opens the file). This is fine for interactive flows; it breaks if the workbook is consumed headlessly by another system.

**Concretely missing (the families to plan around):**
- Dynamic arrays: FILTER, SORT, SORTBY, UNIQUE, SEQUENCE, RANDARRAY
- LAMBDA family: LAMBDA, LET, BYROW, BYCOL, MAP, REDUCE, SCAN, MAKEARRAY
- Array shaping: VSTACK, HSTACK, TOCOL, TOROW, WRAPCOLS, WRAPROWS, CHOOSECOLS, CHOOSEROWS, TAKE, DROP, EXPAND
- New text: TEXTSPLIT, TEXTBEFORE, TEXTAFTER, REGEX family
- New pivot: GROUPBY, PIVOTBY, PERCENTOF
- IMAGE() (2023), PY() (2024), TRIMRANGE (2024)

**Structural gaps** (not just missing functions — POI's model is pre-2019):
- **Dynamic-array spill semantics.** POI handles legacy CSE array formulas (`{=...}`) but not the modern dynamic-array model where one formula spills into adjacent ghost cells. POI reads cached spilled values but cannot recompute them.
- **`@` implicit intersection** and **`#` spilled range reference** operators — parsed literally, not applied semantically.

**Future options to close the gap, in order of likely adoption:**
1. **HyperFormula sidecar** (MIT, JS, 400+ functions including dynamic arrays). Run in Node, communicate over a Unix socket. ~2-3 weeks integration. Best cost/benefit for v1.1 or v2.
2. **Univer sidecar** (Apache 2.0, JS, full dynamic array + some LDT support). Alternative to HyperFormula; younger but actively developed.
3. **LibreOffice headless** (already in §14.1) — slower per call, heavier image, but covers more than HyperFormula.
4. **Hybrid cascade** — POI native → HyperFormula → LibreOffice fallback. Most complex; only worth building if multiple downstream consumers actually need it.
5. **Microsoft Graph / Excel Online** — 100% parity but requires cloud and data egress. Probably a non-starter for sovereignty reasons; document as the "if all else fails" option.

### 14.1b Linked Data Types (Stocks, Geography, custom Power Query types)

POI has **zero support**. The data lives in OPC parts under `/xl/richData/*` (rdRichValue.xml, rdRichValueTypes.xml, rdRichValueStructure.xml) referencing `/xl/metadata.xml`. POI preserves the parts as opaque blobs on round-trip but cannot introspect or modify them. Older viewers see `#VALUE!` in such cells.

v1 strategy: **detect via `workbook.capabilities_report` and warn**. Don't attempt to read or modify LDT cells. Don't strip them.

**Future options:**
1. **Detect + flag** (current): the capabilities report says "this workbook has LDT cells; preserve only, do not modify."
2. **Strip and degrade** to the fallback string value, drop the rich data parts. Destructive — only suitable if the user explicitly opts in.
3. **Microsoft Graph / Excel Online** — the only realistic open-source-adjacent option for actually round-tripping LDTs.

### 14.2 Workbook content not in v1

- **Charts**: read structural info, render to PNG, create. (Render needs LibreOffice; structural read/create is POI but verbose.)
- **Pivot tables**: read structural info, refresh, create. (POI's pivot support is very limited — needs LibreOffice for anything beyond trivial cases.)
- **Cell formatting** beyond what `include_formatting=true` already exposes for reads: **writing** font/color/border/fill/conditional formatting.
- **Data validation rules** (read or write) — list of allowed values, ranges, error messages.
- **Copy operations**: `range.copy`, `sheet.copy` (across workbooks too). xlport's `copyCell` style-map pattern is a useful reference.
- **Merge / unmerge cells** (the v1 `sheet.merged_regions` is read-only).
- **Hyperlinks**, **comments**, **threaded comments**, **images / drawings / shapes**.
- **External workbook links** read/repair.
- **Sheet protection** (lock cells, protect with password). xlport's `Template.protectWorkbook` is a reference.
- **Page setup / print area / headers & footers**.

### 14.3 Power Query & Data Model

- **Power Query (M) extraction**: subprocess to PowerQueryViewer. Adds Python to image.
- **DAX / PowerPivot model introspection**: subprocess to PBIXRay. Adds Python + apsw deps.
- **Power Query / DAX writing or modification**: not realistically possible without Excel itself. Defer indefinitely.

### 14.4 VBA beyond read

- **VBA execution**: requires real Excel in a sandbox. Defer indefinitely.
- **VBA module editing / writing**: technically possible (rebuild `vbaProject.bin`) but extremely fiddly. Defer until there's a real need.
- **UserForm extraction**: known-incomplete in every Java/Python tool. Document the limit if/when first asked.

### 14.5 Transport & deployment

- **HTTP transport** (streamable HTTP, SSE) — the architecture supports it (one process per session is just a deployment convention), but v1 ships stdio only.
- **Multi-tenant shared server** — would require per-handle locking, idle-handle eviction, optional per-session sandboxing. Adds real complexity; defer until there's a concrete reason.
- **Auth on the MCP transport itself** — currently delegated to whoever owns the container.
- **Metrics endpoint** (Prometheus / OpenTelemetry).
- **Sibling MCPs** (Word via XWPF, PowerPoint via XSLF) — same architecture, separate projects, shared utility lib. See the design doc.

### 14.6 Test & ops nice-to-haves

- **Larger fixture corpus** (subset of xlport-internal `test-suites/` if the chef approves).
- **Performance regression tests** on large workbooks.
- **ArchUnit** tests enforcing the §4.1 layer rules.
- **Per-workbook auto-save policy** option (currently we never auto-save).
- **Soak / leak tests** for handle registry under long sessions.
- **JSON log format** option (alongside the current plain-text format) for centralized log pipelines.
