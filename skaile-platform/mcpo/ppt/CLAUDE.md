# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Stateful MCP server (`ai.skaile.mcpo:ppt-mcp-server`) that exposes PowerPoint manipulation as JSON-RPC tools over stdio. An agent opens a `.pptx`, receives a `document_id` handle, and drives subsequent mutations through that handle until it calls `ppt.save_document` / `ppt.close_document`. Backed by Apache POI 5.5.x; LibreOffice (`soffice`) is only invoked for PDF export.

`README.md` contains the authoritative tool catalog (currently 56 tools), response envelope schema, and example JSON-RPC frames — read it before adding or modifying a tool. Note that README sometimes drifts ahead of the pom (see "Known doc/source drift" below).

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
  - `PptToolService` is the dispatch facade: it owns the `SessionStore`, wires the operation classes via method references, and routes `tools/call` by name through `toolHandlers`. When adding a tool: add the definition in `PptToolDefinitions`, pick the right operations subclass (or add an inline handler on `PptToolService`), then register in `createToolHandlers()`.
  - `tooling/contracts/` — `ToolHandler` (functional interface), `ToolDefinition`, `ToolCallResult`. Do not put implementation detail here.
  - `tooling/infra/` — shared plumbing: `ToolArgumentValidator` (schema check + coerce), `ToolResponseFactory` (success/error envelope builder), `PptPathResolver` (sandbox-aware path resolution and image/format inference). Reuse these instead of re-rolling validation or response construction.
  - `tooling/operations/` — behavior organized by tool family: `PptDocumentOperations`, `PptSlideOperations`, `PptAdvancedMutationOperations`, `PptRenderService`, `PptTemplateService`. Each class is plain delegation — it receives method-reference callbacks from `PptToolService` so the POI-heavy implementations stay centralized.

### Response envelope

Every tool — success or failure — returns the envelope documented in README under "Error/Response Model": `status`, `code`, `error`, `retriable`, `correlation_id`, `tool_name`, `data`. `correlation_id` and `tool_name` are injected by `McpServer` after the tool returns. Always build responses through `ToolResponseFactory`, never hand-craft an `ObjectNode`.

### Paths and sandboxing

`PptPathResolver` enforces that every path argument sits beneath `MCPO_ALLOWED_ROOT` (set to `/workspace/resources` in the Docker image). Outside Docker — and in tests — that variable is unset and any absolute path is accepted. When writing tool code, call `pathResolver.resolvePath(raw, forWrite)` rather than touching `Path.of(...)` directly so the sandbox check is not bypassed.

### Stdio discipline

`System.out` carries MCP frames only. Logging (SLF4J → Logback) goes to stderr, and `System.out` is redirected to stderr at startup as defense in depth. Any `System.out.println` / `e.printStackTrace()` call anywhere in the server path will corrupt the protocol stream — use the logger.

### Transactions

`ppt.transaction_begin/commit/rollback` are implemented via `TransactionSnapshot` stored in a `HashMap` on `PptToolService`. Snapshots are per-document, in-memory only, and single-level (no nesting). They serialize the full `XMLSlideShow` to bytes, so begin/rollback is expensive on large decks.

## Environment variables

| Variable | Default (Docker) | Purpose |
|---|---|---|
| `MCPO_ALLOWED_ROOT` | `/workspace/resources` | Sandboxing root for tool path arguments. Unset = no sandbox. |
| `MCPO_TEMPLATE_DIR` | `/workspace/resources/.mcpo-ppt/templates` | Template store location for `ppt.upload_template`. |
| `MCPO_DEFAULT_TEMPLATE_CONFIG` | `/workspace/resources/.mcpo-ppt/default-template.json` | Persisted default-template pointer. |
| `SOFFICE_PATH` | `/usr/bin/soffice` | LibreOffice binary used for PDF export. If missing, PDF export fails gracefully. |
| `LOG_LEVEL` | `INFO` | Logback root level. |

## Known doc/source drift

- `README.md` mentions `./mvnw` and Java 21; there is no Maven Wrapper checked in and `pom.xml` sets `maven.compiler.release=17`. Use system `mvn`; the Docker build stage uses `maven:3.9-eclipse-temurin-21` and the runtime uses `eclipse-temurin:21-jre-jammy`, so the produced jar is Java-17 bytecode running on a Java 21 JVM.
- `README.md` lists Spotless and AssertJ in the tech stack; neither is in `pom.xml`. Only `junit-jupiter` is declared as a test dep. Don't rely on Spotless formatting gates.
