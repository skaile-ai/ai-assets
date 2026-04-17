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

## Authoring conventions (standing directives)

These apply to every phase from this point forward, across sessions. Follow them when writing new code; if you inherit code that violates them, fix it.

### Tool description template — required for every new tool from Phase 5 onward

The `ToolDefinition.description()` returned by each tool must be a single block of text with **three parts, in this order**:

1. **Effect statement** — one sentence describing what changes in the world when the tool runs (e.g. "Loads an Excel workbook into memory and returns an opaque handle for subsequent calls.").
2. **Prerequisite / lifecycle note** — what must have run before and/or what this tool enables after (e.g. "Requires a handle previously returned by `workbook.open` or `workbook.create`; the returned handle stays valid until `workbook.close` or process exit.").
3. **One important gotcha or constraint** the LLM needs in order to use the tool correctly (e.g. "Changes are in-memory until `workbook.save`.", "Empty cells return `type:blank`, not an error.", "Formula cells do not auto-evaluate — call `workbook.recalculate` to refresh cached values.").

Keep each part one sentence. The whole description should read in under ~60 words.

### Parameter description rules

Every `properties.<name>.description` in a tool's input schema must state:

- **Valid value forms** with at least one concrete example (e.g. `"A1 range like \"A1:C10\" or a single-cell form like \"C5\""`).
- **Constraints** (length, domain, allowed enum values).
- **Units** where applicable (bytes, row indices 1-based vs 0-based, UTC vs naive datetime).

Do **not** duplicate what the JSON Schema already encodes (`type`, `required`, `default`). Do **not** mention POI, XSSF, or any engine-layer concept — agents see the tool surface only.

---

## Plan deferrals during implementation

Appended as decisions are made. Each entry: what was chosen, why, and a one-line sketch of the future-better option if one came to mind. Look for matching `// PLAN-DEFER:` comments in code.

> 🛑 **STOP-GATES for future implementation sessions.** Before acting on any of these, pause and ask the human to choose. Do NOT decide unilaterally — these interact with constraints outside the plan.
>
> - **Phase 10, Dockerfile user:** pick one of the three options in "Container UID vs. bind-mount ownership" below before touching the Dockerfile.
> - **Phase 10, tool-description audit:** review all 25 tool `description()` strings and their parameter descriptions in a single editorial pass against the "Tool description template" and "Parameter description rules" above. Fix any that don't conform (Phases 0–4 tools predate the template). Roughly a 30-minute pass over ~250 lines total. Do this as polish, not ad-hoc during earlier phases.

### Open items to discuss at the end of v1

- **`PATH_OUTSIDE_ROOT` fix: reproducibility disagreement between implementer and operator.** After the §6.1 reorder landed (see "Fixed: `PATH_OUTSIDE_ROOT` was unreachable…" below), end-to-end verification inside a fresh `docker build --no-cache -t excel-mcp:dev .` image returned the expected `PATH_OUTSIDE_ROOT` for `/tmp/foo.xlsx`, `/data/../etc/passwd`, and `/etc/os-release` — and `FILE_NOT_FOUND` for `/data/doesntexist.xlsx` — with server startup confirming `EXCEL_MCP_ROOT=/data (path sandboxing enabled)`. The human still reported seeing `FILE_NOT_FOUND` for `/tmp/foo.xlsx` under the MCP Inspector against what they believed was a fresh image. Most likely: stale docker layer, inspector UI caching a prior response, or a dual docker daemon (WSL/Desktop) showing different images to different shells. **To revisit at end of v1:** reproduce together, capture the exact inspector response payload + server stderr, confirm which daemon / image hash is actually serving the MCP call, and close out one way or the other. No code change to attempt until that diagnosis.

- **Jackson version: 2.20.1 instead of the plan's 2.18.x.** (Phase 0, pom.xml) The MCP Java SDK `mcp-json-jackson2:1.1.1` transitively pulls `jackson-databind:2.20.1`; pinning 2.18.x would force a dual-version shade and surprise null/date handling. Chose SDK alignment. Future better: watch for an SDK drop that realigns and re-pin to whatever the team standardises on.
- **Container UID vs. bind-mount ownership — decision deferred to Phase 10.** (Observed Phase 4, WSL dev host) The `Dockerfile` currently runs the app as `USER 10001`. When the operator bind-mounts a host directory (`-v ./data:/data`), UID 10001 inside the container cannot write to files owned by the host user (typically UID 1000), so `workbook.save` fails with `SAVE_FAILED` unless the operator overrides with `--user 1000:1000` or a Kubernetes `securityContext.runAsUser`. Confirmed reproducible in local WSL dev.

  **Three candidate resolutions — recorded here, not chosen. When Phase 10 begins, STOP before touching the Dockerfile and ask the human to pick one.** This decision interacts with production deployment policies (K8s `runAsNonRoot`, enterprise security baselines) that are outside the plan's scope.

  1. **Documentation only.** Keep `USER 10001`; add a README section explaining the operator must pass `--user $(id -u):$(id -g)` (or `securityContext.runAsUser`) when bind-mounting host directories. Zero Dockerfile change; pushes responsibility to the operator, which is arguably where it belongs for production. Developer ergonomics suffer on first run.
  2. **Change Dockerfile default to UID 1000.** Most Linux/WSL dev hosts have UID 1000 as the primary user, so most users of the image will not need `--user` at all. Operators can still override. Small one-line Dockerfile change; no new dependencies. Trade-off: it's a heuristic that happens to be usually right, not truly automatic; on hosts where UID 1000 maps to a different user, the original problem recurs.
  3. **Entrypoint script with runtime UID detection.** Install `gosu` (or `su-exec`), remove the static `USER` from the Dockerfile, ship an `entrypoint.sh` that stats the mounted directory, rewrites the app user's UID with `usermod -u <owner-uid> appuser`, then `exec gosu appuser java -jar …`. The container "just works" regardless of mount ownership. Trade-offs: the container starts briefly as root (breaks K8s `runAsNonRoot: true` and many enterprise baselines); adds an installed-binary dependency and a shell script to maintain.

- **Fixed: `A1:XFE1` returned blanks instead of `RANGE_OUT_OF_BOUNDS`.** (Phase 4 manual testing) `readRange`/`writeRange`/`clearRange` iterated the requested address blindly, so any range whose end column or row exceeded the workbook's hard format limits (xlsx: XFD / 1,048,576; xls: IV / 65,536) silently returned empty cells. Fixed by asserting against `SpreadsheetVersion.getLastRowIndex()` / `getLastColumnIndex()` in all three range ops before iteration. Regression suite: `RangeBoundsTest` (5 cases, incl. XFE1 on reads/writes/clears and the accept-at-XFD boundary). **Chose to NOT enforce the §9.2 "data-extent" variant** ("Range D1:D10 is outside sheet 'Sheet1' (max column C, max row 5)") — it's noisy for legitimate "show me A1:Z100 on this small sheet" calls, interacts poorly with cleared cells (after `range.clear`, `getLastCellNum()` may return -1 and honest reads like A1:B2 would then fail), and is O(rows) to compute the max column. Format bounds alone address the reported bug. Future better: an opt-in `strict_bounds=true` parameter on `range.get` for callers who do want the data-extent check.
- **Fixed: `PATH_OUTSIDE_ROOT` was unreachable (security — must-fix class, caught pre-production).** (Phase 4 manual testing) In the original `PathValidator`, file-existence and (elsewhere) the format-whitelist ran before the `EXCEL_MCP_ROOT` containment check, so every path outside the sandbox surfaced as `FILE_NOT_FOUND` or `FORMAT_UNSUPPORTED` and the sandbox signal was dead code. Reproduced with `EXCEL_MCP_ROOT=/data`: `/tmp/foo.xlsx → FILE_NOT_FOUND`, `/data/../etc/passwd → FORMAT_UNSUPPORTED`, `/etc/os-release → FORMAT_UNSUPPORTED` — all should have been `PATH_OUTSIDE_ROOT`. Fixed by reordering to §6.1: parse → containment → format → existence, and replacing `allowParent` with a single best-effort canonicaliser that uses `parent.toRealPath()` when the target is missing so symlink escapes (`/data/link -> /etc/passwd`) are caught even for destination paths. Added 8 regression tests in `PathValidatorTest`. Severity: **security — must fix before production.** Impact for local dev was low (no sandboxing without `EXCEL_MCP_ROOT`).
- **`workbook.create` seeds a default "Sheet1".** (Phase 4 manual testing) A brand-new `XSSFWorkbook` has zero sheets and Excel rejects the saved file as corrupt. Since `sheet.create` isn't available until Phase 6, `workbook.create` + `workbook.save` would have been unusable end-to-end. Fixed by calling `workbook.createSheet("Sheet1")` immediately after `new XSSFWorkbook()` in `XssfInMemoryEngine.create`, matching Excel's "New Workbook" default. Plan §9.1 updated inline. Future consideration: once `sheet.create` exists (Phase 6), an optional `initial_sheet` arg on `workbook.create` could let the caller pick the seed sheet's name.
- **Case-insensitive sheet-name lookup.** (Phase 2, `XssfInMemoryEngine.requireSheet`) The plan does not specify whether sheet lookups are case-sensitive. Excel itself treats sheet names as case-insensitive in practice. Chose: exact match first, case-insensitive fallback. Future better: if a user reports ambiguous-match surprises, add a strict-mode toggle via env var.
- **`workbook.recalculate` tolerates `NotImplementedException` per cell instead of failing the whole recalc.** (Phase 5, `PoiFormulaEvaluation`) The plan said "on POI throw, raise `FORMULA_EVAL_FAILED` with the cell address." Literal reading: *any* POI exception, including the expected `NotImplementedException` POI raises for FILTER / LAMBDA / dynamic-array functions it doesn't implement. That would make the tool unusable on any modern workbook. Chose: catch `NotImplementedException` per cell and skip (leaving the cached value alone), rethrow every other `RuntimeException` as `FORMULA_EVAL_FAILED` with the cell address. The skipped cells are the ones `workbook.capabilities_report` already surfaces as `unsupported_functions_used`, so the agent has the information to reason about staleness. Future better: return the skip count in the tool response (currently only successful evaluations are counted).
- **Per-workbook `FormulaEvaluator` caching lives on the engine map, not on `OpenWorkbook`.** (Phase 5, `XssfInMemoryEngine`) Plan §5.2 sketch put the evaluator on the `OpenWorkbook` record; that leaks POI types into `handles/` and violates the §4.1 / §11b.5 boundary (same reasoning as the original `OpenWorkbook` deviation). Chose: hold `Map<HandleId, FormulaEvaluator> evaluators` inside `XssfInMemoryEngine`, mirror the existing `workbooks` map, clean up both on close. Future better: wrap the POI workbook + evaluator + registry metadata into an engine-internal `OpenWorkbookState` record so the per-handle lifecycle is a single thing to manage.
- **Dynamic-array detection uses `CTCell.isSetCm()`, not `CTCellFormula.getCm()`.** (Phase 5, `PoiCapabilityScanner`) The plan §9.1 text suggested walking `<f cm="1">` attributes, but the `cm` attribute is actually on the parent `<c>` element in the OOXML schema — and the `poi-ooxml-lite` build omits `cm` from `CTCellFormula` entirely, only `CTCell.isSetCm()` is exposed. Chose: cast `cell` to `XSSFCell`, read `getCTCell().isSetCm()`. Works on XSSF workbooks; HSSF is always `false` (dynamic arrays post-date the binary format). Minor spec drift, no behavioural gap.
- **Unsupported-function detection uses the regex path from §9.1.** (Phase 5, `PoiCapabilityScanner`) Plan documented two options — regex over `cell.getCellFormula()` (false positives for identifier-shaped substrings inside string constants) vs. `FormulaParser.parse` walking `Ptg[]` (correct but much more code). Plan says "pick the cheap path for v1". Done. `_xlfn.` / `_xlws.` prefixes are stripped before the supported-set lookup so POI-normalised future-function names match properly. Future better: swap in the FormulaParser/`FuncPtg` walk if false positives ever bite.
- **`has_vba` heuristic includes `.xlsm` files without an inline `vbaProject.bin` part.** (Phase 5, `PoiCapabilityScanner`) POI's `XSSFWorkbook.isMacroEnabled()` returns true for anything loaded from a `.xlsm`; we OR that with the OPC-part check for `/xl/vbaProject.bin`. Over-inclusive on workbooks that were saved as `.xlsm` but have no actual macros, which is harmless for a "does this need VBA tooling?" signal. Future better: look at `vbaProject.bin` size / structure to distinguish empty-VBA from real VBA, once `vba.list_modules` lands in Phase 9.
- **No `SHEET_NAME_INVALID` error code.** (Phase 6, `XssfInMemoryEngine.createSheet`/`renameSheet`) POI's `IllegalArgumentException` for invalid sheet names (length > 31, forbidden chars `: \ / ? * [ ]`, leading/trailing apostrophe) is surfaced as `INTERNAL_ERROR` with POI's message in `details`. Plan §8.2 defined a fixed error-code set that didn't include a name-validity code, and the duplicate-name path (the common case) is already covered by `SHEET_ALREADY_EXISTS` from a pre-check. Future better: extend the enum with `SHEET_NAME_INVALID` and validate up front (regex + length) rather than letting POI throw.
- **Column-shifting on HSSF (.xls) surfaces as `INTERNAL_ERROR`.** (Phase 7, `sheet.insert_cols`/`delete_cols`) POI's `HSSFSheet` does not implement `shiftColumns`; calls throw `UnsupportedOperationException` which the dispatcher converts to `INTERNAL_ERROR`. Row-shifting works on HSSF, so the parity gap is columns only. Tool descriptions flag the limitation explicitly. Future better: detect the HSSF case up front and return a dedicated code (`FORMAT_UNSUPPORTED` with a specific sub-reason, or a new `OPERATION_NOT_AVAILABLE_FOR_FORMAT` code) so the agent sees a first-class signal; or implement column shifting for HSSF ourselves using the POI low-level row/cell APIs.
- **Row/column operations do not automatically recalculate dependent formulas.** (Phase 7, all four tools) POI rewrites cell references when rows shift but does NOT re-run the formula evaluator, so cached results on dependent cells go stale. The tool descriptions for `sheet.delete_rows`/`delete_cols` tell the agent to call `workbook.recalculate` afterwards. Future better: an optional `auto_recalc: true` flag on the mutation tools, or an engine-level policy that triggers recalc on every structural change.
- **`shiftColumns` defensive guard compares against `computeLastFilledColumn` scan.** (Phase 7, `XssfInMemoryEngine.insertCols`/`deleteCols`) The plan §9.4 footgun check uses a per-call O(rows) scan to find the last column with data. Fine for typical workbooks but not cheap on wide sheets with millions of cells. Future better: cache the per-sheet "max filled column" on the engine side and invalidate on writes / clears / shifts.
- **Fixed: `range.set` → `range.get` pre-recalc returned `{type:"number", value:0}` instead of `{type:"formula_uncomputed", value:null}` (plan §7.1 violation).** (Phase 5 follow-up) POI's `setCellFormula` leaves the `t` attribute defaulting to `"n"` (NUMERIC), so `getCachedFormulaResultType()` reports NUMERIC and `getNumericCellValue()` falls back to `0.0` when `<v>` is absent. The cached-type hint cannot be trusted as the "has a cached value" signal on its own. Fix is two-sided: `PoiCellWriter` unsets the `<v>` element on XSSF cells after `setCellFormula` as defense against stale cached values from prior writes on the same cell; `PoiCellReader` now treats `!isSetV()` on an XSSFCell with FORMULA type as the authoritative "uncomputed" signal and returns `formula_uncomputed` directly, bypassing the cached-type hint. Regression assertion added to `scripts/phase5-smoke.sh`: after writing `=SUM(A1:A3)` and before `workbook.recalculate`, A4 must read as `{type:"formula_uncomputed", value:null}`.
- **§5.2 `OpenWorkbook` record shape.** (Phase 1, `handles/OpenWorkbook.java`) The plan's example had `OpenWorkbook` hold an `XSSFWorkbook` + `FormulaEvaluator` directly. That leaks POI types into `handles/` and transitively into `tools/`, violating §4.1 layer rules and §11b.5 "POI types leaking into the tool layer or response shapes". Chose: `OpenWorkbook` carries metadata only (id, path, format, openedAt); the engine layer owns POI workbook state internally, keyed by `HandleId`. Future better: once engine layer is in place, document this contract explicitly in the README "Architecture" section and add ArchUnit tests enforcing it (already listed under §14.6).
