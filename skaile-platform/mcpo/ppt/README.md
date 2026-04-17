# PPT MCP Server

MCP server (stdio) that gives an AI agent first-class ability to **create, modify, review, and render PowerPoint presentations** with full fidelity (stateful document sessions, slide manipulation, text styling, image insertion, table editing, PDF/image export).

## Quick Start

Build and run locally:

```bash
./mvnw verify                              # Compile, test, format check, produces fat jar
java -jar target/ppt-mcp-server-*.jar      # Run the server
```

Or test via Docker:

```bash
docker build -t ppt-mcp:dev .
docker run --rm -i ppt-mcp:dev
```

For interactive testing with the MCP Inspector, see "Manual testing with the MCP Inspector" below.

## Tech stack

- **Java 21 LTS** (Eclipse Temurin), **Maven** with the wrapper pinned to 3.9.9, **Spotless** for Google Java Format.
- **Apache POI 5.5.x** (`poi`, `poi-ooxml`) — in-process, Apache-2.0 licensed, for PPTX manipulation.
- **Apache Batik** — SVG rendering and PDF conversion fallback.
- **LibreOffice** (`soffice`) — high-fidelity PDF export. The Docker image installs it and sets `SOFFICE_PATH=/usr/bin/soffice`.
- **Official MCP Java SDK** over stdio. No HTTP / SSE in v1.
- **Jackson** for JSON, **SLF4J + Logback** for logging (stderr-only — stdout is reserved for the MCP protocol).
- **JUnit 5 + AssertJ** for tests; smoke and regression tests included.
- Packaged as a single fat jar via `maven-shade-plugin`, shipped in a `eclipse-temurin:21-jre-jammy` Docker image.

## How it's structured

Three-layer separation enforced by package boundaries:

```
server/    — MCP SDK glue, JSON-RPC dispatch, ToolRegistry. No PPT logic.
tools/     — One file per tool (e.g. CreateDocumentTool). Business logic; never imports POI directly.
engine/    — PptDocumentSession, SessionStore, and POI integration layer.
  engine/poi/ — PoiPresentationAdapter, PoiSlideRenderer, PoiTableEditor, …
shape/     — JSON DTOs (records) shared across tools: SlideShape, TextBoxShape, TableShape.
error/     — ErrorCode enum, McpException, ErrorEnvelope with `status`, `code`, `error`, `retriable` fields.
config/, log/ — env-var parsing and Logback setup (stderr routing).
```

Design choices:

- Tools are registered with JSON schema and validated at runtime before execution.
- All mutating operations mark the document session dirty (`PptDocumentSession.dirty`).
- Stateful in-memory document sessions live in `SessionStore` with opaque `doc_<uuid>` handles.
- Server shutdown attempts best-effort cleanup of all open sessions.

## Tools

v1 ships 26 tools across document lifecycle, slide management, text and content mutation, rendering, templating, and metadata — no charts, animation, slide layouts, or embedded media (see Known Gaps).

| Category | Tool | Parameters | Description |
|---|---|---|---|
| Document lifecycle | `ppt.create_document` | `template_path?` — optional template to clone from | Create a new empty or template-based presentation and return an opaque handle. |
| Document lifecycle | `ppt.open_document` | `path` — absolute path to .pptx file | Open a .pptx presentation from disk and return an opaque handle. |
| Document lifecycle | `ppt.close_document` | `document_id` | Close a document handle. Unsaved changes are discarded. |
| Document lifecycle | `ppt.save_document` | `document_id`; `output_path?` — optional explicit destination | Save the presentation to disk. If opened from file, defaults to source path. |
| Document lifecycle | `ppt.get_document_info` | `document_id` | Retrieve metadata: filename, slide count, modification time, modified flag. |
| Slide management | `ppt.list_slides` | `document_id` | List all slides with zero-based index and title. |
| Slide management | `ppt.add_slide` | `document_id`; `index?` (default end); `layout?` | Add a new slide at the specified position with optional layout. |
| Slide content | `ppt.get_slide_content` | `document_id`; `slide_index` | Retrieve all shapes, text boxes, images, tables, and notes on a slide. |
| Slide content | `ppt.update_text` | `document_id`; `slide_index`; `shape_index`; `text` | Update the text content of a text box or shape. |
| Slide content | `ppt.add_textbox` | `document_id`; `slide_index`; `x`; `y`; `width`; `height`; `text` | Add a new text box to a slide at the specified position. |
| Slide content | `ppt.set_text_style` | `document_id`; `slide_index`; `shape_index`; `font_size?`; `bold?`; `italic?`; `color?` | Apply font styling (size, weight, style, color) to text in a shape. |
| Slide content | `ppt.get_slide_notes` | `document_id`; `slide_index` | Retrieve speaker notes for a slide. |
| Slide content | `ppt.set_slide_notes` | `document_id`; `slide_index`; `notes` | Set speaker notes for a slide. |
| Media | `ppt.insert_image` | `document_id`; `slide_index`; `image_path`; `x`; `y`; `width`; `height` | Insert an image at the specified position and size. |
| Tables | `ppt.add_table` | `document_id`; `slide_index`; `rows`; `cols`; `x`; `y`; `width`; `height` | Create a table with the specified dimensions. |
| Tables | `ppt.get_table_cell` | `document_id`; `slide_index`; `table_index`; `row`; `col` | Read the text content of a table cell. |
| Tables | `ppt.set_table_cell` | `document_id`; `slide_index`; `table_index`; `row`; `col`; `text` | Set the text content of a table cell. |
| Search | `ppt.find_text` | `document_id`; `search_term` | Find all occurrences of text across all slides. Returns slide and shape indices. |
| Rendering | `ppt.render_slide_image` | `document_id`; `slide_index`; `output_path`; `format` (png/jpg); `width?`; `height?` | Export a slide to PNG or JPEG image. |
| Rendering | `ppt.render_slide_svg` | `document_id`; `slide_index`; `output_path`; `width?`; `height?` | Export a slide to SVG vector format for scalability. |
| Rendering | `ppt.render_selection_image` | `document_id`; `slide_index`; `shape_indices`; `output_path`; `format`; `width?`; `height?` | Export selected shapes to PNG or JPEG. |
| Rendering | `ppt.render_selection_svg` | `document_id`; `slide_index`; `shape_indices`; `output_path`; `width?`; `height?` | Export selected shapes to SVG. |
| Templates | `ppt.upload_template` | `template_path` — path to .pptx to use as a template | Register a .pptx file as a reusable template. |
| Templates | `ppt.set_default_template` | `template_path` | Set the default template for `ppt.create_document` and `ppt.generate_presentation`. |
| Templates | `ppt.get_default_template` | (none) | Retrieve the path to the currently active default template, or null if none set. |
| Presentation generation | `ppt.generate_presentation` | `title`; `slides` (array of slide specs); `output_path?` — optional explicit destination; `template_path?` | Generate a complete presentation from a structured specification. Uses default or explicit template. |

## Error/Response Model

All tool responses follow a structured envelope:

```json
{
  "status": "success" | "error",
  "code": "VALIDATION_ERROR" | "FILE_NOT_FOUND" | "IO_ERROR" | "...",
  "error": "Human-readable error message (only when status=error)",
  "retriable": true | false,
  "correlation_id": "<json-rpc-request-id>",
  "tool_name": "ppt.open_document",
  "data": { ... }
}
```

Key fields:

- `status`: `success` on normal completion; `error` if the tool failed.
- `code`: Structured error code for programmatic handling (e.g., `VALIDATION_ERROR`, `FILE_NOT_FOUND`, `IO_ERROR`, `UNSUPPORTED_FORMAT`).
- `error`: Message describing what went wrong (only present when status is `error`).
- `retriable`: Whether retry is expected to help (e.g., `true` for transient I/O; `false` for invalid arguments).
- `correlation_id`: Echo of the JSON-RPC request id for tracing.
- `data`: Tool-specific response payload (e.g., document handle, slide list, rendered image path).

## Build

**If you only want to test via Docker / MCP Inspector: skip this section.** `docker build -t ppt-mcp:dev .` compiles the fat jar inside the build stage of the `Dockerfile`, so the image always contains freshly-compiled code regardless of what's in your host `target/`. See "Manual testing with the MCP Inspector" below.

The commands below are for host-side development only — running the server as `java -jar …` without Docker, running tests, and pre-commit formatting. They are independent alternatives, not a sequence.

The repo ships with the Maven Wrapper pinned to **Maven 3.9.9**. A global `mvn` is not required — and is not what this project uses. **Always invoke the build through `./mvnw` (`mvnw.cmd` on Windows).** CI and local dev must use the same pinned Maven to avoid build-reproducibility drift.

| Command | When to run it |
|---|---|
| `./mvnw verify` | Before declaring a change done. Runs compile + tests + Spotless check; also produces the fat jar. |
| `./mvnw -DskipTests package` | Only when you want the fat jar but want to skip tests. |
| `./mvnw spotless:apply` | When `./mvnw verify` fails on formatting. Auto-rewrites sources to Google Java Format. |
| `./mvnw clean` | For a truly from-scratch host rebuild. Rarely needed. |

The output is a single fat jar at `target/ppt-mcp-server-<version>.jar`, runnable with `java -jar`.

## Run

The server speaks MCP over **stdio only** in v1. No HTTP. Launched via the shaded jar:

```bash
java -jar target/ppt-mcp-server-0.1.0-SNAPSHOT.jar
```

Or via Docker:

```bash
docker build -t ppt-mcp:dev .

# With workspace mount (typical development):
docker run --rm -i \
  -v "$(pwd)/resources:/workspace/resources:rw" \
  ppt-mcp:dev

# Without mounts (dev only; accepts any path the agent gives):
docker run --rm -i ppt-mcp:dev
```

### MCP client descriptor

```json
{
  "mcpServers": {
    "ppt": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "-v", "/host/workspace:/workspace:rw",
        "ppt-mcp:dev"
      ]
    }
  }
}
```

## Manual testing with the MCP Inspector

Run the server under [`@modelcontextprotocol/inspector`](https://github.com/modelcontextprotocol/inspector) to click tools by hand and watch raw request/response frames. Useful when verifying a new tool before wiring it into an agent.

1. **Rebuild the image** after any source change — `docker build` caches layers aggressively on WSL, so if behaviour looks stale, force a rebuild:

   ```bash
   cd skaile-platform/mcpo/ppt/
   docker build -t ppt-mcp:dev .
   # or, if you suspect a cache issue:
   docker build --no-cache -t ppt-mcp:dev .
   ```

2. **Smoke-run the image standalone** (no inspector, no mounts) to confirm it starts and exits cleanly on EOF. Should log startup diagnostics and `mcp server started … tools=<N>`:

   ```bash
   docker run --rm -i --user 1000:1000 ppt-mcp:dev
   ```

3. **Launch the inspector pointed at the image**, mounting a host directory for test files:

   ```bash
   # Run from skaile-platform/mcpo/ppt/
   pwd && npx @modelcontextprotocol/inspector \
     docker run --rm -i --user 1000:1000 \
       -v "$PWD/resources:/workspace/resources:rw" \
       ppt-mcp:dev
   ```

   `--user 1000:1000` makes the container write as the host user so `ppt.save_document` can atomically replace files in the bind-mounted directory. Adjust the UID/GID if your host account differs.

4. **Open the inspector URL** printed to the terminal — typically `http://localhost:6274/?MCP_PROXY_AUTH_TOKEN=<token>`.

5. **Connect** (the inspector picks up the docker command automatically), then browse the tool list.

## Document paths and handles when clicking tools

- **File paths are container-local.** The inspector is outside the container; tool arguments are evaluated inside. Host path `./resources/file1.pptx` is mounted at `/workspace/resources/file1.pptx` (per the `-v` above), so in `ppt.open_document` you pass `/workspace/resources/file1.pptx`, not a host path.
- **Document handles** returned by `ppt.create_document` / `ppt.open_document` look like `doc_50f0d1e7`. Copy the value verbatim into the `document_id` argument of subsequent tools. Handles live for the container's lifetime — a second `docker run` starts fresh.
- **Document storage behavior:**
  - `ppt.create_document` creates a presentation in memory only.
  - New documents are written to disk only when you call `ppt.save_document` with `output_path`.
  - If a document was opened from disk (`ppt.open_document`), `ppt.save_document` without `output_path` overwrites the original file path.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `LOG_LEVEL` | `INFO` | Logback root level. Accepts `ERROR` / `WARN` / `INFO` / `DEBUG`. |
| `SOFFICE_PATH` | `/usr/bin/soffice` | Path to LibreOffice executable for PDF export. Container sets this; override if needed. |

## External Dependencies

- **Apache POI 5.5.x** — PPTX file manipulation (licensed Apache-2.0).
- **Apache Batik** — SVG rendering for slide exports.
- **LibreOffice** (`soffice` command) — **required for PDF export**. The Docker image includes it and sets `SOFFICE_PATH=/usr/bin/soffice`. If `soffice` is not available in the container, PDF export fails gracefully with a tool error. Override `SOFFICE_PATH` environment variable if LibreOffice is installed in a non-standard location.

## v1 limits (by design)

- Formats: `.pptx` and `.pptm` (macro-enabled) are fully supported. Older `.ppt` files (ODP) are not yet supported.
- Tool surface: 26 tools across document lifecycle, slide and content management, rendering (PNG/JPEG/SVG), templating, and search. No charts, pivots, animations, slide layouts, embedded media, or VBA source inspection.
- Transport: stdio only. No HTTP / SSE.
- Tenancy: one process per agent session; no per-handle locking or idle-handle eviction (process death is the eviction).
- Rendering: slides render via POI native → image/SVG. LibreOffice used only for PDF export; if unavailable, PDF export fails gracefully with a tool error.

## Logging

Log output goes **only to stderr** — stdio transport reserves stdout for MCP protocol messages. `System.out` is redirected to stderr at startup as a belt-and-braces measure. POI's log4j output is routed through SLF4J/Logback via `log4j-to-slf4j`.

## Production hardening (expected of the deployment)

These are delegated to whoever owns the container image:

- `--read-only` rootfs with `tmpfs:/tmp`.
- `--network=none` (the MCP itself never needs outbound traffic).
- Memory + CPU caps at the orchestrator level.
- Session state is in-memory only (no persistence across restart) — add a Redis/DB backend for production resilience.
- No multi-instance distributed locking/versioning for collaboration conflicts — optimistic versioning and conflict detection are future work.
- No integrated authn/authz policy hooks yet — these should be added before regulated environments.
- No full telemetry stack (metrics/tracing dashboarding) — add observability hooks before production deployment.
- Limited operational controls for queueing/backpressure and per-tool time budgets — add worker queue and timeouts for SLA guarantees.

## Quick Example Calls

Open a presentation:

```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "method": "tools/call",
  "params": {
    "name": "ppt.open_document",
    "arguments": {
      "path": "/workspace/resources/demo.pptx"
    }
  }
}
```

Expected response (success):

```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "result": {
    "status": "success",
    "code": "OK",
    "data": {
      "document_id": "doc_50f0d1e7",
      "filename": "demo.pptx",
      "slide_count": 5,
      "modified": false
    }
  }
}
```

Insert an image into a slide:

```json
{
  "jsonrpc": "2.0",
  "id": "req-2",
  "method": "tools/call",
  "params": {
    "name": "ppt.insert_image",
    "arguments": {
      "document_id": "doc_50f0d1e7",
      "slide_index": 0,
      "image_path": "/workspace/resources/logo.png",
      "x": 80,
      "y": 80,
      "width": 240,
      "height": 120
    }
  }
}
```

Render a slide to SVG:

```json
{
  "jsonrpc": "2.0",
  "id": "req-3",
  "method": "tools/call",
  "params": {
    "name": "ppt.render_slide_svg",
    "arguments": {
      "document_id": "doc_50f0d1e7",
      "slide_index": 0,
      "output_path": "/workspace/resources/slide-0.svg",
      "width": 1920,
      "height": 1080
    }
  }
}
```

Save and close the document:

```json
{
  "jsonrpc": "2.0",
  "id": "req-4",
  "method": "tools/call",
  "params": {
    "name": "ppt.save_document",
    "arguments": {
      "document_id": "doc_50f0d1e7"
    }
  }
}
```
