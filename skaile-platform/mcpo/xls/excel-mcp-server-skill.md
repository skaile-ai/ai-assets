# Excel MCP Server / Skill — Working Notes

Working notes for the Excel MCP server. The engine and language are now decided: **Java + Apache POI**, in a Linux Docker container, with LibreOffice/UNO and other tools held in reserve as optional expansions when POI hits a wall.

## Goal

Give an AI agent first-class ability to **create, modify, review, and summarize Excel files** with real fidelity (formula recalc, charts, named tables, VBA introspection).

The agent should be able to:
- Open an existing `.xlsx`/`.xlsm`/`.xls` (or create a new one) and keep it loaded across many turns.
- Query structure (`what sheets exist?`, `what tables are defined?`, `give me A1:X100`).
- Distinguish **typed formulas** from **cached/computed values** on every read.
- Mutate cells, ranges, formulas, and formatting.
- Trigger a recalc on demand.
- Flush back to disk (same path or a new one), or close without saving.
- Extract VBA module source (read-only) — feasible early via POI's `VBAMacroExtractor`.
- Later, optionally: extract Power Query M code, extract DAX/PowerPivot model metadata, render charts, inspect pivots, diff two workbooks.

## Scope — what is and isn't my task

Per the chef's clarification, a lot of the surrounding infrastructure is handled by someone else. My deliverable is the **MCP server program itself**.

**Out of scope (handled elsewhere):**
- Deployment — the server will run inside a Docker image provided by someone else.
- Data sourcing — SharePoint / GitHub-repo contents are mounted into the container before the agent talks to the server.
- Path resolution, auth, transport — the agent passes a plain local filesystem path pointing at a file already present inside the container.

**In scope (my deliverable):**
1. A **Java MCP server** that speaks the protocol using the official Java SDK.
2. A **tool surface** the agent can call: open, list sheets, read ranges/tables, set cells/formulas, recalculate, save, close (see sketch below).
3. A **handle / session registry** inside the process so workbooks stay loaded across tool calls — this is the stateful bit that makes MCP the right choice over a plain skill.
4. **POI integration** behind each tool, with hooks to add optional engines/tools later.
5. **Agent-friendly return shapes** — compact JSON, typed formulas vs cached values distinguished, pagination or summarization for large ranges.
6. Error handling, graceful shutdown, idle-handle eviction so forgotten workbooks don't leak memory.

## Why MCP (not just a skill)

A **skill** is instructions to the agent — stateless, lives in the prompt.
An **MCP server** is a long-running process the agent talks to over stdio/SSE — it can hold workbooks in memory across turns, which is the whole point here.

A skill on top of the MCP is still useful: it tells the agent *when and how* to reach for the MCP tools, what return shapes to expect, and the conventions for representing ranges to itself. So the deliverable is **MCP server + thin skill that documents its use**.

## Architecture

```
┌─────────────────┐   stdio/SSE   ┌──────────────────────┐   in-process   ┌─────────────────┐
│   AI agent      │ ────────────▶ │   Excel MCP server   │ ─────────────▶ │   Apache POI    │
│ (Claude, etc.)  │ ◀──────────── │ (handle registry +   │ ◀───────────── │ (XSSF/HSSF +    │
└─────────────────┘   tool calls  │  tool surface)       │                │ FormulaEvaluator│
                                  └──────────────────────┘                │ + VBAMacroExtr.)│
                                          │                               └─────────────────┘
                                          │
                                          └─▶ Document handles: { handle_id → workbook object }
                                              with per-handle lock + idle TTL eviction

                                          (Optional, later, on demand:)
                                          ─▶ subprocess to LibreOffice/UNO  (chart rendering, full recalc)
                                          ─▶ subprocess to PBIXRay          (DAX / PowerPivot model)
                                          ─▶ subprocess to PowerQueryViewer (PQ M extraction)
                                          ─▶ subprocess to calamine         (better .xlsb read fallback)
```

## Chosen engine: Apache POI

POI is the in-process Java library that does the actual Excel work. It's an Apache Software Foundation project, **Apache-2.0 licensed** (commercial-friendly), pure-Java so no external runtime, and has the broadest single-library coverage of what this MCP needs.

### What POI gives us out of the box

| Capability | POI support |
|---|---|
| `.xlsx` / `.xlsm` read/write/edit | ✅ Full, via `XSSFWorkbook` |
| `.xls` (legacy BIFF) read/write/edit | ✅ Full, via `HSSFWorkbook` |
| `.xlsb` (binary OOXML) | ⚠️ Read-only, streaming, marked experimental (`XSSFBReader`) — write not supported |
| Cell values + types | ✅ |
| Formula strings (typed) | ✅ |
| Cached formula values | ✅ (separate from typed formula — easy to expose both) |
| Formula evaluation / recalc | ✅ via `FormulaEvaluator` for ~280 of Excel's ~500+ functions; **structural gaps** on everything Microsoft added since Excel 2019 (FILTER, SORT, UNIQUE, SEQUENCE, LAMBDA family, array shaping, new text/pivot, IMAGE, PY); also no dynamic-array spill semantics, no Linked Data Types — see "Optional later expansions" for the recalc-fallback options |
| Named ranges / defined names | ✅ |
| Tables (ListObjects) | ✅ |
| **VBA module source extraction** | ✅ via `VBAMacroExtractor` — modules and classes as text |
| Charts (read/write XML) | ⚠️ Structural only — POI doesn't *render* charts |
| Pivot tables | ⚠️ Very limited (HSSF can't; XSSF "very limited read/change") |
| External connections / `connections.xml` | ⚠️ Accessible via OOXML parts; no high-level API |
| Streaming (large files) | ✅ SAX EventModel for read; SXSSF for streaming write |

### What POI doesn't do — the gap list

Each of these is the trigger for one of the optional expansions below:

- **Chart rendering** (rasterizing charts to PNG) — needs LibreOffice.
- **Full-fidelity formula recalc** for post-2019 functions (FILTER, SORT, LAMBDA, etc.) and dynamic-array spill — needs **HyperFormula** (lightweight, Node sidecar, MIT) or LibreOffice (heavier, broader coverage) or Aspose.
- **Linked Data Types** (Stocks, Geography, custom PQ types) — no open-source engine handles these. Detect-and-flag is the realistic v1 strategy; full round-trip needs Microsoft Graph / Excel Online.
- **`.xlsb` writing** — needs LibreOffice (convert to `.xlsx` round-trip) or Aspose.
- **Power Query (M) code extraction** — needs PowerQueryViewer or a custom DataMashup parser.
- **DAX / PowerPivot model introspection** (measures, relationships, schema) — needs PBIXRay.
- **Pivot table deep editing** — POI is too limited; needs LibreOffice or Aspose.
- **PDF/image export** — needs LibreOffice or Aspose.

### Optional later expansions (in priority order)

These are not needed for v1. Add them when the agent actually needs the capability.

| When | Add | What it gives | Container cost |
|---|---|---|---|
| Need recalc for post-2019 functions (FILTER/SORT/LAMBDA/dynamic arrays) | **HyperFormula sidecar** (MIT, Node) | 400+ functions including dynamic arrays; lightweight | Small Node sidecar; ~50 MB |
| Need chart rendering, `.xlsb` write, or PDF export, or broader recalc than HyperFormula covers | **LibreOffice + UNO bridge** (MPL-2.0 / LGPL-3.0) | Real chart rendering, ~95% Excel-fidelity recalc, all four formats, `convert-to pdf/png` | ~300 MB image growth; subprocess or UNO Java bindings |
| Need to read Power Query M code | **PowerQueryViewer** (Python, Apache-2.0) | Extracts M from `Formulas/Section1.m` / DataMashup | Python 3 + one pip package |
| Need DAX / PowerPivot model metadata | **PBIXRay** (Python, MIT) | DAX measures, model schema, relationships, RLS — works on `.xlsx` with embedded PowerPivot | Python 3 + `apsw`, `kaitaistruct` (may need `build-essential` + `sqlite3-dev`) |
| `.xlsb` read coverage > POI's `XSSFBReader` allows | **calamine** (Rust binary, MIT) | Fast multi-format reader incl. `.xlsb`; has known `PtgExp` gap on shared/array formulas | One static binary |
| POI + LibreOffice still can't hit the fidelity bar | **Aspose.Cells for Java** (commercial) | Near-100% fidelity, all formats, charts, recalc, PDF/image export | Paid license |

**License caveat to flag:** POI is Apache-2.0 (very permissive). LibreOffice/UNO is **MPL-2.0 + LGPL-3.0** — fine for typical use, but if this MCP ever ships as part of a closed commercial product, the LGPL piece needs review with whoever owns licensing. Doesn't affect us *running* it in a Docker container.

## Language: Java

Decided. The team has Java experience, POI is a first-party Java library so calling it is in-process (no IPC, no serialization), the official MCP Java SDK exists, and the structured tool surface (typed payloads, request/response shapes) plays to Java's strengths.

The optional expansions above (PowerQueryViewer, PBIXRay, calamine) live outside the JVM. That's fine — they're invoked as subprocesses when needed. MCP servers can absolutely shell out to other binaries; the agent doesn't see the difference.

Performance is not a deciding factor. The wrapper code does almost nothing — parse a tool call, hand off to POI, serialize the result. POI dominates wall-clock time.

## Tech stack

### Runtime

- **Java 21 LTS** (or 17 if a constraint forces it). Modern records and sealed types help with the tool-payload modeling.
- **Build tool**: Maven or Gradle — pick whatever the team already uses.
- **Container base**: `eclipse-temurin:21-jre-alpine` or `eclipse-temurin:21-jre-jammy`. Alpine is smaller; Jammy is friendlier if you later add LibreOffice (which expects glibc).

### Core dependencies

```
# MCP
io.modelcontextprotocol.sdk:mcp                  # official Java MCP SDK

# Apache POI (Excel)
org.apache.poi:poi                               # core + HSSF (.xls)
org.apache.poi:poi-ooxml                         # XSSF (.xlsx/.xlsm)
org.apache.poi:poi-scratchpad                    # VBAMacroExtractor lives here
                                                 # (also brings HSLF/HWPF for sibling MCPs)
org.apache.poi:poi-ooxml-full                    # if you hit OOXML schema gaps with -lite

# Serialization, logging, testing
com.fasterxml.jackson.core:jackson-databind      # JSON
org.slf4j:slf4j-api + ch.qos.logback:logback-classic
org.junit.jupiter:junit-jupiter
org.assertj:assertj-core
```

POI deliberately does **not** ship a single fat jar — each module above pulls in only what it needs. Pin versions explicitly; POI majors do break APIs.

### Project layout (sketch)

```
excel-mcp/
├── pom.xml (or build.gradle)
├── Dockerfile
├── src/main/java/
│   └── com/skaile/excelmcp/
│       ├── McpServer.java          # protocol entry point
│       ├── tools/                  # one file per tool (open, range_get, …)
│       ├── handles/                # HandleRegistry, idle-TTL eviction
│       ├── poi/                    # POI adapters: WorkbookAdapter, RangeReader, …
│       ├── recalc/                 # FormulaEvaluator wrapper
│       ├── vba/                    # VBAMacroExtractor wrapper
│       ├── path/                   # safe-path validation against mount root
│       └── shape/                  # JSON return-shape builders
└── src/test/java/...
```

### Optional-expansion bits (only when needed, not in v1)

- **LibreOffice / UNO**: install `libreoffice-core` + `libreoffice-calc` in the image. Two integration shapes:
  1. Shell out to `soffice --headless --convert-to xlsx /in/foo.xlsb` or `--convert-to pdf` per call (simple, robust, slow per invocation).
  2. Long-running `soffice --accept="socket,host=localhost,port=2002;urp;"` and use UNO Java bindings (`com.sun.star.*`) for in-process recalc. Faster per call, more setup.
- **PowerQueryViewer**: add `python3` + `pip` to the image, `pip install powerqueryviewer` (or vendor a pinned commit). Subprocess and capture stdout.
- **PBIXRay**: same — `pip install pbixray`. May need `build-essential` + `libsqlite3-dev` for the `apsw` dep depending on wheel availability.
- **calamine**: copy the prebuilt static Rust binary into `/usr/local/bin/`. No runtime deps.
- **Aspose.Cells for Java**: add the dependency + license file. Behind a feature flag.

### Container layout strategy

Start the v1 image **POI-only** — small, fast, deterministic. When an expansion is added, decide whether it goes in:

- **The same image** (simpler ops, bigger image), or
- **A second sidecar container** (e.g. a "libreoffice-recalc" service) that this MCP shells out to over a Unix socket (better separation, harder ops).

Default to the same image until the size or coupling actually hurts.

## Format coverage

The agent will see paths to `.xlsx`, `.xlsm`, `.xls`, and possibly `.xlsb`. Concrete coverage with POI:

| Format | What it is | POI capability | Action if input |
|---|---|---|---|
| `.xlsx` | Standard OOXML (ZIP + XML) | Full read/write/edit | Just use it |
| `.xlsm` | OOXML + VBA macros | Full read/write/edit + VBA source extraction | Just use it; VBA is bonus |
| `.xls` | Legacy BIFF (OLE/CFB binary) | Full read/write/edit (via `HSSFWorkbook`) | Just use it; tell the agent it's a legacy format |
| `.xlsb` | Binary BIFF12 inside ZIP | Read-only via `XSSFBReader`, marked experimental, **no write** | v1: error out clearly. Later: convert via LibreOffice on open, write back as `.xlsx`, OR use calamine to read |

Choose now: do we **silently convert `.xlsb` → `.xlsx` on save** (lossy on save), or **refuse `.xlsb` writes** (honest but limiting)? Default suggestion: refuse writes in v1, surface the limitation to the agent, address in a later iteration.

## Tool surface (sketch)

```
workbook.open(path)              → handle
workbook.create()                → handle
workbook.list_sheets(handle)
workbook.save(handle, path?)
workbook.close(handle)
workbook.recalculate(handle)

range.get(handle, sheet, a1)     → { range, values, formulas, formats }
range.set(handle, sheet, a1, values | formulas)

table.list(handle)
table.get(handle, name)

vba.list_modules(handle)         → [{ name, type }]   (POI VBAMacroExtractor)
vba.get_module(handle, name)     → { name, source }

# Optional / later (each gated on its expansion being installed)
power_query.list(handle)         # via PowerQueryViewer
power_query.get(handle, name)
model.dax_measures(handle)       # via PBIXRay
model.schema(handle)
model.relationships(handle)
chart.list(handle, sheet?)
chart.render(handle, sheet, name) → PNG bytes  # via LibreOffice
pivot.refresh(handle, name)
workbook.export_pdf(handle, path)              # via LibreOffice
workbook.diff(handle_a, handle_b)
```

## Sibling MCPs (Word, PowerPoint)

The chef hinted at two MCPs ("for the two mcps") — Excel and PowerPoint. Word likely follows. The architecture transfers cleanly but the MCPs should be **separate projects**:

**What carries over unchanged:**
- MCP skeleton: transport, handle registry, per-handle locking, idle-TTL eviction, open/query/mutate/save/close lifecycle.
- Engine: POI sister projects — `XSLF` for `.pptx` (in `poi-ooxml`), `XWPF` for `.docx` (in `poi-ooxml`).
- All the watch-outs and the same Java tech stack.

**What's genuinely different — and forces separate servers:**

The domain models share almost nothing:

| Concept | Excel | Word | PowerPoint |
|---|---|---|---|
| Primary unit | cell grid with formulas | flowing styled paragraphs | positioned shapes on slides |
| "Recalc" means | formula evaluation | field updates (TOC, cross-refs, page numbers) | layout/master cascade — mostly trivial |
| Typical agent op | `range.get/set` | `paragraph.list`, `find_replace`, `comment.add`, `track_changes.accept` | `slide.list`, `shape.add`, `text.set`, `layout.apply` |

**Engine quality is uneven across formats** — POI is rock-solid for Excel but genuinely weaker for `.docx` (XWPF has gaps around comments, complex tables, track-changes). Realistic outcome: **default to POI across all three servers, swap on a per-server basis if you hit a wall** — most likely candidate is Word needing LibreOffice if XWPF falls short.

**Same language across all three** is a strong yes, with a small shared utility lib (handle registry, lock helper, TTL eviction, common return-shape helpers, path validation) consumed by all three projects.

## Suggested build order

1. **Skeleton MCP** with `workbook.open / list_sheets / range.get / workbook.save / workbook.close` using POI only, no recalc, no writes. Proves wiring inside the Docker image with a mounted file.
2. **Add writes**: `range.set` + `workbook.recalculate` via POI's `FormulaEvaluator`. Enough to demo live edits + recalc on common formulas.
3. **Add VBA introspection**: `vba.list_modules` + `vba.get_module` via `VBAMacroExtractor`. Cheap to add since POI already does it.
4. **Add tables and named ranges** properly: `table.list/get`, defined-name lookups, structured-reference resolution.
5. **Polish**: large-range pagination/summarization, clearer error messages to the agent, idle-handle eviction, golden-file tests.
6. **First optional expansion** — pick the one driven by an actual agent use case. Most likely **LibreOffice/UNO** for chart rendering and recalc fidelity, **OR** PowerQueryViewer/PBIXRay if PQ/DAX use cases come first.
7. **Sibling MCPs**: spin up Word and PowerPoint servers reusing the shared utility lib.

## Things to watch out for

- **Don't let the AI touch raw OOXML XML.** The "rename .xlsx to .zip and edit XML" trick technically works for trivial reads but corrupts on round-trip — shared strings table, indexed styles, formula caches, `_rels/` parts. Always go through POI.
- **Always distinguish typed formula from cached value.** This is the single most common thing LLMs get wrong about spreadsheets. Every read should expose both.
- **Return shapes matter for the LLM.** Don't dump raw cell objects. Use compact JSON (`{range, values, formulas, headers?}`). Big ranges need pagination or summarization or you'll burn the agent's context on a 10k-row sheet.
- **POI `FormulaEvaluator` has gaps.** LAMBDA, dynamic-array spilling, and some array formulas don't evaluate. Surface "could not evaluate" cleanly to the agent rather than returning silently wrong values.
- **`.xlsb` writes are not supported by POI.** Decide the v1 policy explicitly and tell the agent.
- **Recalc can block.** Even POI's `FormulaEvaluator` can be slow on big workbooks. Run recalc off the MCP's main loop or the server wedges. UNO/COM block even harder if you add them later.
- **State safety.** Per-session MCP processes are simpler than shared. If you go shared, you need real per-handle locking (`java.util.concurrent.locks.Lock` per handle is fine) and idle-TTL eviction so abandoned workbooks don't leak heap.
- **VBA is read-mostly.** Extracting source via POI is fine. Executing VBA safely effectively requires real Excel in a sandbox — not something we should promise.
- **Snapshots / determinism for tests.** POI is reasonably byte-stable on save. LibreOffice (if added later) reorders parts — test that path differently.
- **Path handling inside the container.** The agent passes a local path; the server must validate it points inside the mounted area and reject anything outside. Trivial check, easy to forget. (Java: `Path.toRealPath().startsWith(mountRoot.toRealPath())`.)
- **File mutation safety.** Mounted files may be backed by SharePoint or a Git checkout — overwriting in place may have side effects beyond our process. Default to write-through but make it easy to save to a sibling path.

## Security hardening (for the Docker image)

Mostly delivered by whoever builds the image, but worth specifying so the contract is clear:

- **No network**: run with `--network=none` if the MCP itself never needs outbound traffic.
- **Read-only rootfs**: `--read-only` + `tmpfs` for `/tmp`.
- **Non-root user** in the Dockerfile (`USER 10001`).
- **Resource limits**: memory + CPU caps, plus `ulimit`s on file size — ZIP bombs and pathological OOXML files exist and POI will happily try to load them.
- **POI memory caps**: POI exposes `IOUtils.setByteArrayMaxOverride()` and similar — set sane upper bounds to fail fast on absurd inputs.
- **VBA output is untrusted code as text.** When the MCP returns extracted VBA source to the agent, it's literally untrusted source code. The agent should treat it as data, not as something to execute or interpret as instructions.
- **`connections.xml` may contain credentials**, DSNs, or URLs. If/when we expose connection metadata, plan a redaction policy (mask `connectionString`, etc.) — don't just dump the raw XML to the agent.
- **XML bomb protection**: only relevant if we ever add a Python sub-tool that parses OOXML XML directly (e.g. PowerQueryViewer / PBIXRay step). Use `defusedxml` in those Python steps. POI itself has its own protections.

## Open questions to resolve before committing

- **Does VBA execution need to work, or is "read VBA source" enough?** Affects whether real Excel is ever needed downstream. (Source extraction via POI is in-scope from day one.)
- **Is there existing internal xlport / POI code to build on?** The chef's earlier mention suggests yes — would be a real head start.
- **`.xlsb` policy in v1**: refuse writes (honest), or silently round-trip via LibreOffice on save (convenient, lossy)?
- **`.xlsb` reads in v1**: rely on POI's experimental `XSSFBReader`, or pull in calamine immediately as a sidecar binary?
- **What's the largest workbook we realistically need to handle?** Drives memory/streaming decisions and pagination defaults.
- **Single-tenant per agent process, or shared multi-tenant server?** Drives the locking / eviction design. The Docker context probably implies one server per agent session, but worth confirming.
- **Which optional expansion do we expect first** — chart rendering / fidelity (LibreOffice), or PQ/DAX (PowerQueryViewer + PBIXRay)? Drives which Python or LibreOffice infrastructure we plan into the image first.
- **Same engine across all three sibling MCPs (Excel/Word/PPT)?** Default same (POI everywhere); revisit only on concrete fidelity failures.
