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

The module is now organized by responsibility instead of a single flat package:

```
src/main/java/ai/skaile/mcpo/ppt/
  McpServerMain.java            # executable entrypoint
  server/                       # MCP protocol loop and JSON-RPC framing
    McpServer.java
    JsonRpcIO.java
  tooling/                      # tool registry, schemas, and execution logic
    PptToolService.java
    PptDocumentOperations.java
    PptSlideOperations.java
    PptRenderService.java
    PptTemplateService.java
    ToolHandler.java
    ToolDefinition.java
    ToolCallResult.java
    ToolArgumentValidator.java
    ToolResponseFactory.java
    PptPathResolver.java
  session/                      # in-memory document lifecycle and handle store
    PptDocumentSession.java
    SessionStore.java
```

Testing follows the same structure:

```
src/test/java/ai/skaile/mcpo/ppt/
  DockerPptMcpServerSmokeTest.java
  server/JsonRpcIOTest.java
  tooling/PptToolServiceTest.java
```

Maintenance conventions:

- Keep protocol concerns inside `server` only.
- Keep tool schemas and execution behavior inside `tooling`.
- Keep document state and store behavior inside `session`.
- Use `ToolArgumentValidator`, `PptPathResolver`, and `ToolResponseFactory` for shared concerns instead of adding more utility logic directly into `PptToolService`.
- When adding a new tool, update `PptToolService` and add/extend tests in `tooling/PptToolServiceTest`.

## Tools

Current build ships 56 tools across document lifecycle, slide management, shape/text mutation, rendering, metadata, layout control, templating, markdown import, and transaction workflows. All tools are supported by Apache POI 5.5.x and/or LibreOffice.

Supported tool families include:
- Document lifecycle: `ppt.create_document`, `ppt.open_document`, `ppt.close_document`, `ppt.save_document`, `ppt.get_document_info`
- Page setup: `ppt.set_page_setup`
- Slide management: `ppt.list_slides`, `ppt.add_slide`, `ppt.duplicate_slide`, `ppt.delete_slides`, `ppt.reorder_slides`, `ppt.merge_presentations`
- Slide content: `ppt.get_slide_content`, `ppt.update_text`, `ppt.replace_text_globally`, `ppt.add_textbox`, `ppt.set_text_style`, `ppt.get_slide_notes`, `ppt.set_slide_notes`
- Media & tables: `ppt.insert_image`, `ppt.replace_image`, `ppt.add_table`, `ppt.get_table_cell`, `ppt.set_table_cell`, `ppt.modify_table_structure`, `ppt.set_table_row_height`, `ppt.set_table_column_width`, `ppt.set_table_header_style`
- Search: `ppt.find_text`
- Shapes & styling: `ppt.add_shape`, `ppt.move_shape`, `ppt.clone_shape`, `ppt.resize_shape`, `ppt.delete_shape`, `ppt.get_shape_properties`, `ppt.set_shape_style`, `ppt.set_shape_z_order`, `ppt.add_hyperlink`, `ppt.set_slide_background`
- Layout & metadata: `ppt.set_slide_layout`, `ppt.set_document_metadata`
- Text formatting: `ppt.set_text_style`, `ppt.set_text_run_style`, `ppt.set_text_formatting`, `ppt.set_list_formatting`
- Rendering: `ppt.render_slide_image`, `ppt.render_all_slides_image`, `ppt.render_slide_svg`
- Metrics: `ppt.get_slide_metrics`
- Templates: `ppt.upload_template`, `ppt.set_default_template`, `ppt.get_default_template`, `ppt.import_markdown_outline`
- Presentation generation: `ppt.generate_presentation`
- Transactions: `ppt.transaction_begin`, `ppt.transaction_commit`, `ppt.transaction_rollback`

For exact argument schemas, rely on `tools/list` output at runtime or the tool definitions in `src/main/java/ai/skaile/mcpo/ppt/tooling/PptToolService.java`.

| Category | Tool | Parameters | Description |
|---|---|---|---|
| Document lifecycle | `ppt.create_document` | `title?`; `template_path?` | Create a new in-memory presentation and return a document handle. |
| Document lifecycle | `ppt.open_document` | `path` | Open an existing presentation into memory and return a document handle. |
| Document lifecycle | `ppt.close_document` | `document_id` | Close a document handle and release resources. |
| Document lifecycle | `ppt.get_document_info` | `document_id` | Return document metadata, dirty state, page size, and timestamps. |
| Page setup | `ppt.set_page_setup` | `document_id`; `preset`; `width?`; `height?` | Set page size using a preset or custom dimensions. |
| Slide management | `ppt.list_slides` | `document_id` | List slides with index, text preview, and shape count. |
| Slide management | `ppt.reorder_slides` | `document_id`; `new_order` | Reorder all slides using a full index permutation. |
| Slide management | `ppt.add_slide` | `document_id`; `title?` | Add a slide, optionally initializing title text. |
| Slide management | `ppt.duplicate_slide` | `document_id`; `source_slide_index`; `target_index?` | Duplicate a slide and optionally place it at a target index. |
| Slide management | `ppt.delete_slides` | `document_id`; `slide_indices`; `keep_at_least_one?` | Delete one or more slides by index. |
| Slide management | `ppt.merge_presentations` | `document_id`; `merge_path`; `insert_at_index?` | Merge slides from another presentation into an open document. |
| Slide content | `ppt.get_slide_content` | `document_id`; `slide_index` | Return text and shape metadata for a single slide. |
| Slide content | `ppt.update_text` | `document_id`; `slide_index`; `old_text`; `new_text`; `occurrence?` | Replace a specific text occurrence on a slide. |
| Slide content | `ppt.replace_text_globally` | `document_id`; `old_text`; `new_text`; `case_sensitive?`; `max_replacements?` | Replace text occurrences across all slides. |
| Slide content | `ppt.add_textbox` | `document_id`; `slide_index`; `text`; `x`; `y`; `width`; `height`; `font_size?` | Add a textbox to a slide at a specific position. |
| Shapes | `ppt.add_shape` | `document_id`; `slide_index`; `shape_type`; `x`; `y`; `width`; `height`; `text?`; `fill_color?`; `border_color?`; `border_width?` | Add a primitive shape (rectangle/ellipse/line/arrow). |
| Media | `ppt.insert_image` | `document_id`; `slide_index`; `image_path`; `x`; `y`; `width`; `height` | Insert an image into a slide. |
| Media | `ppt.replace_image` | `document_id`; `slide_index`; `shape_index`; `image_path`; `keep_size?` | Replace a picture shape while preserving placement/size. |
| Notes | `ppt.get_slide_notes` | `document_id`; `slide_index` | Get speaker notes text for a slide. |
| Notes | `ppt.set_slide_notes` | `document_id`; `slide_index`; `notes_text` | Set speaker notes text for a slide. |
| Tables | `ppt.add_table` | `document_id`; `slide_index`; `rows`; `cols`; `x`; `y`; `width`; `height` | Add a table to a slide. |
| Tables | `ppt.get_table_cell` | `document_id`; `slide_index`; `shape_index`; `row_index`; `col_index` | Read text from a table cell. |
| Tables | `ppt.set_table_cell` | `document_id`; `slide_index`; `shape_index`; `row_index`; `col_index`; `text` | Set text in a table cell. |
| Tables | `ppt.modify_table_structure` | `document_id`; `slide_index`; `shape_index`; `operation`; `index?` | Insert/delete table rows or columns. |
| Tables | `ppt.set_table_row_height` | `document_id`; `slide_index`; `shape_index`; `row_index`; `height` | Set height for one table row. |
| Tables | `ppt.set_table_column_width` | `document_id`; `slide_index`; `shape_index`; `col_index`; `width` | Set width for one table column. |
| Tables | `ppt.set_table_header_style` | `document_id`; `slide_index`; `shape_index`; `row_index?`; `fill_color?`; `font_color?`; `bold?` | Apply style to a table header row. |
| Text formatting | `ppt.set_text_style` | `document_id`; `slide_index`; `shape_index`; `bold?`; `italic?`; `underline?`; `font_size?`; `font_color?` | Apply style updates to all runs in a text shape. |
| Text formatting | `ppt.set_text_run_style` | `document_id`; `slide_index`; `shape_index`; `target_text`; `occurrence?`; `case_sensitive?`; `bold?`; `italic?`; `underline?`; `font_size?`; `font_color?` | Style one matched text segment via rich text runs. |
| Text formatting | `ppt.set_list_formatting` | `document_id`; `slide_index`; `shape_index`; `bullet_enabled?`; `numbered?`; `bullet_character?`; `bullet_level?`; `line_spacing?`; `space_before?`; `space_after?` | Apply bullet/numbering and paragraph spacing semantics. |
| Shapes | `ppt.move_shape` | `document_id`; `slide_index`; `shape_index`; `x`; `y` | Move a shape to new coordinates. |
| Shapes | `ppt.clone_shape` | `document_id`; `slide_index`; `shape_index`; `offset_x?`; `offset_y?` | Clone a text-capable shape on the same slide. |
| Shapes | `ppt.resize_shape` | `document_id`; `slide_index`; `shape_index`; `width`; `height` | Resize a shape to a new width and height. |
| Shapes | `ppt.add_hyperlink` | `document_id`; `slide_index`; `shape_index`; `url` | Attach hyperlink to all text runs in a text shape. |
| Slide styling | `ppt.set_slide_background` | `document_id`; `slide_index`; `color` | Set a solid slide background color. |
| Markdown import | `ppt.import_markdown_outline` | `markdown_text`; `output_path?` | Create a presentation from markdown headings and bullets. |
| Transactions | `ppt.transaction_begin` | `document_id` | Create a transaction snapshot for rollback. |
| Transactions | `ppt.transaction_commit` | `document_id` | Commit changes and discard transaction snapshot. |
| Transactions | `ppt.transaction_rollback` | `document_id` | Roll back document state to transaction snapshot. |
| Metrics | `ppt.get_slide_metrics` | `document_id`; `slide_index` | Analyze slide composition and text density. |
| Document lifecycle | `ppt.save_document` | `document_id`; `output_path?`; `format?` | Save to disk in PPTX or PDF format. |
| Rendering | `ppt.render_slide_image` | `document_id`; `slide_index`; `output_path`; `width?`; `height?` | Render one slide as PNG/JPG image. |
| Rendering | `ppt.render_all_slides_image` | `document_id`; `output_dir`; `format?`; `file_name_pattern?`; `width?`; `height?` | Render all slides as PNG/JPG images. |
| Rendering | `ppt.render_slide_svg` | `document_id`; `slide_index`; `output_path`; `width?`; `height?` | Render one slide to SVG. |
| Search | `ppt.find_text` | `document_id`; `query`; `case_sensitive?` | Find text occurrences across all slides. |
| Templates | `ppt.upload_template` | `source_path`; `template_name?`; `make_default?` | Copy/upload a template into the template store. |
| Templates | `ppt.set_default_template` | `template_path` | Set default template used by create/generate operations. |
| Templates | `ppt.get_default_template` | (none) | Return current default template configuration. |
| Presentation generation | `ppt.generate_presentation` | `title?`; `slide_titles?`; `template_path?`; `output_path?` | Generate a presentation from titles and optional template. |
| Shapes | `ppt.delete_shape` | `document_id`; `slide_index`; `shape_index` | Remove a shape from a slide by index. |
| Shapes | `ppt.get_shape_properties` | `document_id`; `slide_index`; `shape_index` | Return detailed shape properties (type, anchor, text). |
| Shapes | `ppt.set_shape_style` | `document_id`; `slide_index`; `shape_index`; `fill_color?`; `border_color?`; `border_width?`; `text_align?` | Set fill, border, and text alignment style on a shape. |
| Metadata | `ppt.set_document_metadata` | `document_id`; `title?`; `author?`; `subject?`; `keywords?` | Set core document metadata fields. |
| Layout | `ppt.set_slide_layout` | `document_id`; `slide_index`; `layout_type` | Apply a layout type to a slide. |
| Text formatting | `ppt.set_text_formatting` | `document_id`; `slide_index`; `shape_index`; `text_align?`; `line_spacing?`; `left_margin?`; `indent?` | Apply paragraph-level text formatting. |
| Shapes | `ppt.set_shape_z_order` | `document_id`; `slide_index`; `shape_index`; `position` | Move shape in z-order (front/back/forward/backward). |

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

## Testing with GitHub Copilot

If your VS Code build supports MCP servers for Copilot Chat, add the server to your workspace MCP config and point it at the Docker image you already validated above.

```json
{
  "servers": {
    "ppt-mcp": {
      "type": "stdio",
      "command": "docker",
      "cwd": "${workspaceFolder}/skaile-platform/mcpo/ppt",
      "args": [
        "run",
        "--rm",
        "-i",
        "--user",
        "1000:1000",
        "-v",
        "${workspaceFolder}/skaile-platform/mcpo/ppt/resources:/workspace/resources:rw",
        "ppt-mcp:dev"
      ]
    }
  }
}
```

Use these prompts in Copilot Chat after the server is connected:

1. Create a new presentation called `Copilot Smoke Deck` with four slides named `Intro`, `Problem`, `Solution`, and `Next Steps`, then save it to `/workspace/resources/generated/copilot-smoke-deck.pptx`.
2. Open `/workspace/resources/generated/copilot-smoke-deck.pptx`, add a textbox to slide 0, set the text to `Hello from Copilot`, and then render that slide as PNG.
3. Reorder the slides so `Solution` comes first, then return the updated slide order and slide metrics for slide 0.
4. Insert an image from `/workspace/resources/logo.png` onto slide 0, then export the presentation to PDF.
5. Import a short markdown outline from a file in `/workspace/resources`, generate slides from it, and confirm the new slide count.

Keep the `/workspace/resources` mount in place so Copilot can work against the same files the Docker smoke tests use.

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
- Tool surface: 56 tools across document lifecycle, slide/content management, shape/text formatting, rendering (PNG/JPEG/SVG), metadata, layout updates, templating, search, markdown import, and transaction workflows. No native animation, transitions, embedded media timelines, or VBA source inspection.
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
