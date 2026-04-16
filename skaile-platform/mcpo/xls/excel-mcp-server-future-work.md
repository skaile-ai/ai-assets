# Excel MCP Server — Future Work

> Living document. Seeded from §14 of `excel-mcp-server-implementation-plan.md` at project start (per §1.6). Every "we'll handle this later" judgement call made during implementation gets appended under "Plan deferrals during implementation" below.

---

## 1. Format & engine

- **`.xlsb` read or write.** Rejected at open with future-friendly error. Future option A: calamine sidecar binary for read. Future option B: LibreOffice convert-on-open + write back as `.xlsx`.
- **Streaming engine** for very large workbooks (>100 MB / >1M cells). Future: second `WorkbookEngine` impl using SXSSF (write) + XSSF SAX EventModel (read), gated by a per-workbook size heuristic.
- **LibreOffice / UNO bridge** for chart rendering, PDF/image export, and full-fidelity recalc on functions POI's evaluator can't handle. Future: ship as a sidecar process, MCP shells out per call.
- **Aspose.Cells fallback** for fidelity gaps neither POI nor LibreOffice cover. Commercial license — only pursue if a real need surfaces.

### 1a. Formula coverage gap (the biggest known limitation)

POI's `FormulaEvaluator` implements ~280 of Excel's ~500+ functions. Everything Microsoft has added since Excel 2019 is missing. v1 strategy: **detect via `workbook.capabilities_report` and rely on recalc-on-open** (Excel/LibreOffice fixes stale values when a human opens the file). This is fine for interactive flows; it breaks if the workbook is consumed headlessly by another system.

**Concretely missing:**

- Dynamic arrays: FILTER, SORT, SORTBY, UNIQUE, SEQUENCE, RANDARRAY
- LAMBDA family: LAMBDA, LET, BYROW, BYCOL, MAP, REDUCE, SCAN, MAKEARRAY
- Array shaping: VSTACK, HSTACK, TOCOL, TOROW, WRAPCOLS, WRAPROWS, CHOOSECOLS, CHOOSEROWS, TAKE, DROP, EXPAND
- New text: TEXTSPLIT, TEXTBEFORE, TEXTAFTER, REGEX family
- New pivot: GROUPBY, PIVOTBY, PERCENTOF
- IMAGE() (2023), PY() (2024), TRIMRANGE (2024)

**Structural gaps:**

- **Dynamic-array spill semantics.** POI handles legacy CSE array formulas (`{=...}`) but not the modern dynamic-array model where one formula spills into adjacent ghost cells. POI reads cached spilled values but cannot recompute them.
- **`@` implicit intersection** and **`#` spilled range reference** — parsed literally, not applied semantically.

**Future options to close the gap, in priority order:**

1. **HyperFormula sidecar** (MIT, JS, 400+ functions including dynamic arrays). Run in Node, communicate over a Unix socket. ~2–3 weeks integration. Best cost/benefit for v1.1 or v2.
2. **Univer sidecar** (Apache 2.0, JS, full dynamic array + some LDT support). Alternative to HyperFormula.
3. **LibreOffice headless** — slower per call, heavier image, but covers more than HyperFormula.
4. **Hybrid cascade** — POI native → HyperFormula → LibreOffice fallback.
5. **Microsoft Graph / Excel Online** — 100% parity but requires cloud and data egress. Document as the "if all else fails" option.

### 1b. Linked Data Types (Stocks, Geography, custom Power Query types)

POI has **zero support**. The data lives in OPC parts under `/xl/richData/*` referencing `/xl/metadata.xml`. POI preserves the parts as opaque blobs on round-trip but cannot introspect or modify them. Older viewers see `#VALUE!`.

v1 strategy: **detect via `workbook.capabilities_report` and warn**. Don't attempt to read or modify LDT cells. Don't strip them.

**Future options:**

1. **Detect + flag** (current): capabilities report says "this workbook has LDT cells; preserve only, do not modify."
2. **Strip and degrade** to the fallback string value, drop the rich data parts. Destructive — only suitable if user explicitly opts in.
3. **Microsoft Graph / Excel Online** — only realistic option for actually round-tripping LDTs.

## 2. Workbook content not in v1

- **Charts**: read structural info, render to PNG, create. (Render needs LibreOffice; structural read/create is POI but verbose.)
- **Pivot tables**: read structural info, refresh, create. (POI's pivot support is very limited — needs LibreOffice for anything non-trivial.)
- **Cell formatting writes** beyond the read-side `include_formatting=true` exposure: font/color/border/fill/conditional formatting.
- **Data validation rules** (read or write).
- **Copy operations**: `range.copy`, `sheet.copy` (cross-workbook too). xlport's `copyCell` style-map pattern is a useful reference.
- **Merge / unmerge cells** (v1 `sheet.merged_regions` is read-only).
- **Hyperlinks**, **comments**, **threaded comments**, **images / drawings / shapes**.
- **External workbook links** read/repair.
- **Sheet protection** (lock cells, protect with password). xlport's `Template.protectWorkbook` is a reference.
- **Page setup / print area / headers & footers**.

## 3. Power Query & Data Model

- **Power Query (M) extraction**: subprocess to PowerQueryViewer. Adds Python to image.
- **DAX / PowerPivot model introspection**: subprocess to PBIXRay. Adds Python + apsw deps.
- **Power Query / DAX writing or modification**: not realistically possible without Excel itself. Defer indefinitely.

## 4. VBA beyond read

- **VBA execution**: requires real Excel in a sandbox. Defer indefinitely.
- **VBA module editing / writing**: technically possible (rebuild `vbaProject.bin`) but extremely fiddly. Defer until there's a real need.
- **UserForm extraction**: known-incomplete in every Java/Python tool. Document the limit if/when first asked.

## 5. Transport & deployment

- **HTTP transport** (streamable HTTP, SSE) — architecture supports it; v1 ships stdio only.
- **Multi-tenant shared server** — would require per-handle locking, idle-handle eviction, optional per-session sandboxing.
- **Auth on the MCP transport itself** — currently delegated to whoever owns the container.
- **Metrics endpoint** (Prometheus / OpenTelemetry).
- **Sibling MCPs** (Word via XWPF, PowerPoint via XSLF) — same architecture, separate projects, shared utility lib.

## 6. Test & ops nice-to-haves

- **Larger fixture corpus** (subset of `xlport-internal/src/test/resources/test-suites/` if approved).
- **Performance regression tests** on large workbooks.
- **ArchUnit** tests enforcing the §4.1 layer rules.
- **Per-workbook auto-save policy** option (currently we never auto-save).
- **Soak / leak tests** for handle registry under long sessions.
- **JSON log format** option (alongside plain-text) for centralized log pipelines.

---

## Plan deferrals during implementation

Appended as decisions are made. Each entry: what was chosen, why, and a one-line sketch of the future-better option if one came to mind. Look for matching `// PLAN-DEFER:` comments in code.

- **Jackson version: 2.20.1 instead of the plan's 2.18.x.** (Phase 0, pom.xml) The MCP Java SDK `mcp-json-jackson2:1.1.1` transitively pulls `jackson-databind:2.20.1`; pinning 2.18.x would force a dual-version shade and surprise null/date handling. Chose SDK alignment. Future better: watch for an SDK drop that realigns and re-pin to whatever the team standardises on.
- **Case-insensitive sheet-name lookup.** (Phase 2, `XssfInMemoryEngine.requireSheet`) The plan does not specify whether sheet lookups are case-sensitive. Excel itself treats sheet names as case-insensitive in practice. Chose: exact match first, case-insensitive fallback. Future better: if a user reports ambiguous-match surprises, add a strict-mode toggle via env var.
- **§5.2 `OpenWorkbook` record shape.** (Phase 1, `handles/OpenWorkbook.java`) The plan's example had `OpenWorkbook` hold an `XSSFWorkbook` + `FormulaEvaluator` directly. That leaks POI types into `handles/` and transitively into `tools/`, violating §4.1 layer rules and §11b.5 "POI types leaking into the tool layer or response shapes". Chose: `OpenWorkbook` carries metadata only (id, path, format, openedAt); the engine layer owns POI workbook state internally, keyed by `HandleId`. Future better: once engine layer is in place, document this contract explicitly in the README "Architecture" section and add ArchUnit tests enforcing it (already listed under §14.6).
