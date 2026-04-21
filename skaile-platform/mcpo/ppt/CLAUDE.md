# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Stateful MCP server (`ai.skaile.mcpo:ppt-mcp-server`) that exposes PowerPoint manipulation as JSON-RPC tools over stdio. An agent opens a `.pptx`, receives a `document_id` handle, and drives subsequent mutations through that handle until it calls `ppt.save_document` / `ppt.close_document`. Backed by Apache POI 5.5.x; LibreOffice (`soffice`) is only invoked for PDF export.

`README.md` contains the authoritative tool catalog (currently 52 tools after Phase 1 consolidation, Phase 4 addition of `ppt.set_picture_effects`, and Phase 5 addition of `ppt.list_charts` + `ppt.update_chart_data`), response envelope schema, and example JSON-RPC frames — read it before adding or modifying a tool. Note that README sometimes drifts ahead of the pom (see "Known doc/source drift" below).

## Commands

Maven Wrapper is pinned to **3.9.9** in `.mvn/wrapper/maven-wrapper.properties` — always use `./mvnw`.

| Command | Purpose |
|---|---|
| `./mvnw verify` | Compile + run tests + jacoco coverage gate + build fat jar. |
| `./mvnw -DskipTests package` | Fat jar only. Output: `target/ppt-mcp-server-all.jar`. |
| `./mvnw test -Dtest=PptToolServiceTest` | Run a single test class. |
| `./mvnw test -Dtest=PptToolServiceTest#methodName` | Run one test method. |
| `java -jar target/ppt-mcp-server-all.jar` | Run the server on stdio. |
| `docker build -t ppt-mcp:dev .` | Build the runtime image (includes LibreOffice). |

`DockerPptMcpServerSmokeTest` shells out to `docker build` + `docker run`, so it is slow and skips itself via JUnit `Assumptions` when Docker is unavailable. When iterating on non-Docker logic, target `PptToolServiceTest` / `JsonRpcIOTest` directly.

For interactive tool inspection: `npx @modelcontextprotocol/inspector docker run --rm -i --user 1000:1000 -v "$PWD/resources:/workspace/resources:rw" ppt-mcp:dev` (run from this module directory). The `--user 1000:1000` is required so `ppt.save_document` can atomically replace files in the bind mount.

## Architecture

Three packages under `src/main/java/ai/skaile/mcpo/ppt/`. Keep concerns inside their package — adding utility logic directly to `PptToolService` is the anti-pattern the refactor was built to avoid.

- **`server/`** — JSON-RPC / MCP framing only.
  - `JsonRpcIO` reads and writes length-prefixed frames on `System.in` / `System.out`.
  - `McpServer` is the message loop. It handles `initialize`, `ping`, `tools/list`, `tools/call`, stamps `correlation_id` and `tool_name` onto the tool payload, emits both `content[]` (pretty-printed text) and `structuredContent` on each call, and invokes `toolService.closeAllSessions()` on shutdown.
- **`session/`** — document lifecycle.
  - `PptDocumentSession` wraps one `XMLSlideShow` with id, source path, dirty flag, timestamps.
  - `SessionStore` is the in-memory handle table. Handles (`doc_xxxxxxxx`) live for the life of the JVM; there is no persistence, no idle eviction, and no per-handle locking.
- **`tooling/`** — everything about tool contracts and execution.
  - `PptToolDefinitions` declares the tool list + JSON schemas consumed by `tools/list`.
  - `PptToolService` is the dispatch facade (~200 lines). It owns the `SessionStore`, builds the tool registry by merging every operations class's `handlers()` map, wraps every `document_id`-scoped invocation in a `ReentrantLock`, and translates the typed exceptions from `PptShapeFinder` / `ColorParser` into structured error codes. It holds **no** tool business logic. Adding a tool is a two-file edit — see `tooling/README.md`.
  - `tooling/contracts/` — `ToolHandler` (functional interface), `ToolDefinition`, `ToolCallResult`. Do not put implementation detail here.
  - `tooling/infra/` — shared primitives, none of which know about tool semantics:
    - `PptServerConfig` (record; **the only file that reads `System.getenv`**): `MCPO_ALLOWED_ROOT`, `MCPO_TEMPLATE_DIR`, `MCPO_DEFAULT_TEMPLATE_CONFIG`, `MCPO_MAX_OPEN_DOCS`, `SOFFICE_PATH`, plus the cached `java.version`.
    - `PptPathResolver` — allowed-root-aware path resolution, sandbox temp files, image-format inference.
    - `PptShapeFinder` — resolves `(document_id, slide_index, shape_index)` to typed objects; throws `DocumentNotFoundException` / `SlideIndexOutOfRangeException` / `ShapeIndexOutOfRangeException` which the dispatcher translates to uniform error codes.
    - `PptLimits` — safety-limit constants (`MAX_SLIDES`, `MAX_SHAPES_PER_SLIDE`, `MAX_IMAGE_BYTES`, `MAX_RENDER_DIMENSION`) and the `enforce*` helpers that return `LIMIT_*` error payloads.
    - `PptSlideBuilder` — shared slide-construction helpers (title placement, body text, best-effort layout).
    - `ColorParser` — the only hex-color parser. Malformed input → `INVALID_COLOR`.
    - `PptUnits` — EMU ↔ points ↔ pixels; the only place `914400 EMU/inch` appears.
    - `PptShapeXml` — Phase 4 XML helpers for things POI doesn't expose at the facade: gradient/pattern fills on `CTShapeProperties`, picture crop/alpha/recolor on `CTBlipFillProperties`, and shape-id management for clone.
    - `SofficeAvailability` — one-shot LibreOffice probe, cached process-wide.
    - `ToolArgumentValidator`, `ToolResponseFactory` — unchanged.
  - `tooling/operations/` — behavior organized by tool family. Each class is **self-contained**: real dependencies via constructor (no lambda-reference pass-through from `PptToolService`), owns its handler methods, and exposes `public Map<String, ToolHandler> handlers()`.
    - `PptDocumentOperations` — doc lifecycle, export (incl. all Phase-3 formats), transactions, merge, generate.
    - `PptSlideOperations` — slide content (add/duplicate/delete, update/replace/find, notes, get_slide_content).
    - `PptShapeMutationOperations` — shape add/move/resize/clone (Phase 4: any shape type via deep XML copy + cache invalidation)/delete, style (Phase 4: gradient/pattern fills), z-order, hyperlink, replace_image, **set_picture_effects**.
    - `PptTableOperations` — table add/get/edit (incl. Phase 4 `merge_cells` + `set_cell_border`).
    - `PptTextOperations` — `ppt.set_text` only. Split out of `PptTableOperations` in Phase 4 when `strikethrough`/`rotation`/`auto_fit` additions would have pushed the table class past the 800-line cap.
    - `PptPageOperations` — `set_page_setup`, `set_slide_background`, `set_slide_layout`, `set_document_metadata`.
    - `PptRenderOperations` — `render_slide`, `render_all_slides`, `find_text`, `get_slide_metrics`.
    - `PptTemplateOperations` — `insert_image`, template upload/default, `import_markdown_outline`. Owns the mutable default-template pointer consulted by `PptDocumentOperations` when creating sessions.
    - `PptChartOperations` — Phase 5: `list_charts` + `update_chart_data`. Updates both the chart's numCache/strCache XML and the embedded XLSX workbook cells so PowerPoint's "Edit Data" dialog stays consistent. Creating charts from scratch is v2.
    - `PptCapabilitiesOperations` — `ppt.capabilities` self-describe handler.
    - `PptTransactionManager` — per-session transaction snapshots (begin/commit/rollback).
    - `SofficeRenderer` — single entry point for all soffice invocations; semaphore-guarded, 90s timeout, cleans temp files on exception paths.

### Response envelope

Every tool — success or failure — returns the envelope documented in README under "Error/Response Model": `status`, `code`, `error`, `retriable`, `correlation_id`, `tool_name`, `data`. `correlation_id` and `tool_name` are injected by `McpServer` after the tool returns. Always build responses through `ToolResponseFactory`, never hand-craft an `ObjectNode`.

### Paths and sandboxing

`PptPathResolver` enforces that every path argument sits beneath `MCPO_ALLOWED_ROOT` (set to `/workspace/resources` in the Docker image). Outside Docker — and in tests — that variable is unset and any absolute path is accepted. When writing tool code, call `pathResolver.resolvePath(raw, forWrite)` rather than touching `Path.of(...)` directly so the sandbox check is not bypassed.

### Stdio discipline

`System.out` carries MCP frames only. Logging (SLF4J → Logback) goes to stderr, and `System.out` is redirected to stderr at startup as defense in depth. Any `System.out.println` / `e.printStackTrace()` call anywhere in the server path will corrupt the protocol stream — use the logger.

### Transactions

`ppt.transaction_begin/commit/rollback` live in `PptTransactionManager`. Snapshots are per-document, in-memory only, and single-level (no nesting). They serialize the full `XMLSlideShow` to bytes, so begin/rollback is expensive on large decks.

## Environment variables

| Variable | Default (Docker) | Purpose |
|---|---|---|
| `MCPO_ALLOWED_ROOT` | `/workspace/resources` | Sandboxing root for tool path arguments. Unset = no sandbox. |
| `MCPO_TEMPLATE_DIR` | `/workspace/resources/.mcpo-ppt/templates` | Template store location for `ppt.upload_template`. |
| `MCPO_DEFAULT_TEMPLATE_CONFIG` | `/workspace/resources/.mcpo-ppt/default-template.json` | Persisted default-template pointer. |
| `SOFFICE_PATH` | `/usr/bin/soffice` | LibreOffice binary used for PDF export. If missing, PDF export fails gracefully. |
| `MCPO_STDIO_FRAMED` | `false` | Opt-in `Content-Length`-framed stdio mode for legacy clients. Read directly by `server/JsonRpcIO` at class load — the one documented exception to the "only `PptServerConfig` reads env" rule, because it is a protocol-framing debug flag rather than tooling config. |
| `LOG_LEVEL` | `INFO` | Logback root level. |

## Known doc/source drift

- The Maven Wrapper is pinned to 3.9.9 — use `./mvnw` as the README says. `pom.xml` sets `maven.compiler.release=17`; the Docker build stage uses `maven:3.9-eclipse-temurin-21` and the runtime uses `eclipse-temurin:21-jre-jammy`, so the produced jar is Java-17 bytecode running on a Java 21 JVM.
- `README.md` may still list Spotless and AssertJ in the tech stack; neither is in `pom.xml`. Only `junit-jupiter` is declared as a test dep. Don't rely on Spotless formatting gates.
