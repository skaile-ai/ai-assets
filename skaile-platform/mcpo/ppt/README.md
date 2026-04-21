# PPT MCP Server

MCP server (stdio) that gives an AI agent first-class ability to **create, modify, review, and render PowerPoint presentations** with full fidelity (stateful document sessions, slide manipulation, text styling, image insertion, table editing, PDF/image export).

## Quick Start

Build and run locally:

```bash
./mvnw verify                              # Compile, test, coverage gate, produces fat jar
java -jar target/ppt-mcp-server-all.jar    # Run the server
```

Or test via Docker:

```bash
docker build -t ppt-mcp:dev .
docker run --rm -i ppt-mcp:dev
```

For interactive testing with the MCP Inspector, see "Manual testing with the MCP Inspector" below.

## Tech stack

- **Java 17 bytecode, Java 21 runtime** (Eclipse Temurin). `maven.compiler.release=17` in `pom.xml`; the Docker runtime stage uses `eclipse-temurin:21-jre-jammy`.
- **Maven** with the wrapper (`./mvnw`) pinned to 3.9.9 via `.mvn/wrapper/maven-wrapper.properties`.
- **Apache POI 5.5.x** (`poi`, `poi-ooxml`) — in-process, Apache-2.0 licensed, for PPTX manipulation.
- **Apache Batik** — SVG rendering and PDF conversion fallback.
- **LibreOffice** (`soffice`) — high-fidelity PDF export. The Docker image installs it and sets `SOFFICE_PATH=/usr/bin/soffice`.
- **Official MCP Java SDK** over stdio. No HTTP / SSE in v1.
- **Jackson** for JSON; logging writes to stderr only — stdout is reserved for the MCP protocol.
- **JUnit 5** for tests; smoke and regression tests included.
- Packaged as a single fat jar via `maven-shade-plugin`, shipped in a `eclipse-temurin:21-jre-jammy` Docker image.

## How it's structured

The module is organized by responsibility. `PptToolService` is a thin dispatch facade; every tool lives in an operations class and registers itself via a `handlers()` map:

```
src/main/java/ai/skaile/mcpo/ppt/
  McpServerMain.java            # executable entrypoint
  server/                       # MCP protocol loop and JSON-RPC framing
    McpServer.java
    JsonRpcIO.java
  tooling/                      # tool registry, schemas, dispatch
    PptToolService.java         # dispatch facade (~200 lines); no business logic
    PptToolDefinitions.java     # JSON-schema tool catalog
    contracts/                  # ToolHandler, ToolDefinition, ToolCallResult
    infra/                      # shared primitives
      PptServerConfig.java      # the only file reading System.getenv
      PptPathResolver.java      # sandbox-aware path resolution, temp files
      PptShapeFinder.java       # (doc, slide, shape) resolver with typed exceptions
      PptLimits.java            # safety-limit constants + enforce* helpers
      PptSlideBuilder.java      # shared slide-construction helpers
      ColorParser.java          # the single hex-color parser
      PptUnits.java             # EMU <-> points <-> pixels conversions
      SofficeAvailability.java  # cached LibreOffice probe
      ToolArgumentValidator.java
      ToolResponseFactory.java
    operations/                 # tool implementations, grouped by family
      PptDocumentOperations.java       # lifecycle, export, merge, generate
      PptSlideOperations.java          # slide content + text
      PptShapeMutationOperations.java  # shape add/move/resize/clone/delete/style
      PptTableOperations.java          # add/get/edit_table + set_text
      PptPageOperations.java           # page setup, background, layout, metadata
      PptRenderOperations.java         # render, find_text, metrics
      PptTemplateOperations.java       # insert_image, templates, markdown import
      PptCapabilitiesOperations.java   # ppt.capabilities self-describe
      PptTransactionManager.java       # per-session transaction snapshots
  session/                      # in-memory document lifecycle and handle store
    PptDocumentSession.java
    SessionStore.java
```

Testing follows the same structure:

```
src/test/java/ai/skaile/mcpo/ppt/
  DockerPptMcpServerSmokeTest.java
  server/JsonRpcIOTest.java
  session/SessionStoreTest.java
  tooling/PptToolServiceTest.java
```

Maintenance conventions:

- Keep protocol concerns inside `server` only.
- Keep tool schemas inside `PptToolDefinitions` and execution behavior inside `operations/`.
- Keep document state and store behavior inside `session`.
- Route all hex-color parsing through `ColorParser`, all EMU/point/pixel math through `PptUnits`, all (doc, slide, shape) resolution through `PptShapeFinder`, and all path resolution through `PptPathResolver`. Never touch `Files.createTempFile` directly — use `pathResolver.createSandboxTempFile`.
- **Adding a new tool is a two-file edit**: declare the schema in `PptToolDefinitions`, then add a handler method + a `Map.of("ppt.your_tool", this::yourTool)` entry in the relevant operations class's `handlers()` map. Do not edit `PptToolService` — it is a pure dispatch facade.

## Tools

Current build ships **50 tools** across document lifecycle, slide management, shape/text mutation, rendering, metadata, layout control, templating, markdown import, and transaction workflows. All tools are supported by Apache POI 5.5.x and/or LibreOffice. Call `ppt.capabilities` at runtime for the authoritative list of versions, feature flags, and safety limits.

Supported tool families:
- Document lifecycle: `ppt.create_document`, `ppt.open_document`, `ppt.close_document`, `ppt.export_document`, `ppt.get_document_info`
- Page setup: `ppt.set_page_setup`
- Slide management: `ppt.list_slides`, `ppt.add_slide`, `ppt.duplicate_slide`, `ppt.delete_slides`, `ppt.reorder_slides`, `ppt.merge_presentations`
- Slide content: `ppt.get_slide_content`, `ppt.update_text`, `ppt.replace_text_globally`, `ppt.add_textbox`, `ppt.set_text`, `ppt.get_slide_notes`, `ppt.set_slide_notes`
- Media: `ppt.insert_image`, `ppt.replace_image`, `ppt.set_picture_effects`
- Tables: `ppt.add_table`, `ppt.get_table`, `ppt.edit_table`
- Search: `ppt.find_text`
- Shapes & styling: `ppt.add_shape`, `ppt.move_shape`, `ppt.clone_shape`, `ppt.resize_shape`, `ppt.delete_shape`, `ppt.get_shape_properties`, `ppt.set_shape_style`, `ppt.set_shape_z_order`, `ppt.add_hyperlink`, `ppt.set_slide_background`
- Layout & metadata: `ppt.set_slide_layout`, `ppt.set_document_metadata`
- Rendering: `ppt.render_slide`, `ppt.render_all_slides`
- Metrics: `ppt.get_slide_metrics`
- Templates: `ppt.upload_template`, `ppt.set_default_template`, `ppt.get_default_template`, `ppt.import_markdown_outline`
- Presentation generation: `ppt.generate_presentation`
- Transactions: `ppt.transaction_begin`, `ppt.transaction_commit`, `ppt.transaction_rollback`
- Capabilities / self-describe: `ppt.capabilities`

For exact argument schemas, rely on `tools/list` output at runtime or the tool definitions in `src/main/java/ai/skaile/mcpo/ppt/tooling/PptToolDefinitions.java`.

| Category | Tool | Parameters | Description |
|---|---|---|---|
| Document lifecycle | `ppt.create_document` | `title?`; `template_path?` | Create a new in-memory presentation and return a document handle. |
| Document lifecycle | `ppt.open_document` | `path` | Open an existing presentation into memory and return a document handle. |
| Document lifecycle | `ppt.close_document` | `document_id` | Close a document handle and release resources. |
| Document lifecycle | `ppt.get_document_info` | `document_id` | Return document metadata, dirty state, page size, and timestamps. |
| Document lifecycle | `ppt.export_document` | `document_id`; `output_path?`; `format?` | Export to disk. `format` = `pptx` (POI) \| `pdf` \| `html` (LibreOffice) \| `png_batch` \| `jpg_batch` \| `svg_batch` (LibreOffice — `output_path` must be a directory) \| `outline_text` (POI). |
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
| Slide content | `ppt.set_text` | `document_id`; `slide_index`; `shape_index`; `scope?` (`shape`\|`run`\|`paragraph`); `target_text?`; `occurrence?`; `case_sensitive?`; `paragraph_index?`; run-style: `bold?`, `italic?`, `underline?`, `strikethrough?`, `font_size?`, `font_color?`, `font_family?`; shape-body: `rotation?`, `auto_fit?` (`none`\|`normal`\|`shrink`); paragraph-style: `text_align?`, `line_spacing?`, `space_before?`, `space_after?`, `left_margin?`, `indent?`, `bullet_enabled?`, `numbered?`, `bullet_character?`, `bullet_level?`. | Unified text mutation (replaces `ppt.set_text_style`, `ppt.set_text_run_style`, `ppt.set_text_formatting`, `ppt.set_list_formatting`). |
| Shapes | `ppt.add_shape` | `document_id`; `slide_index`; `shape_type`; `x`; `y`; `width`; `height`; `text?`; `fill_color?`; `border_color?`; `border_width?` | Add a primitive shape (rectangle/ellipse/line/arrow). |
| Media | `ppt.insert_image` | `document_id`; `slide_index`; `image_path`; `x`; `y`; `width`; `height` | Insert an image into a slide. |
| Media | `ppt.replace_image` | `document_id`; `slide_index`; `shape_index`; `image_path`; `keep_size?` | Replace a picture shape while preserving placement/size. |
| Notes | `ppt.get_slide_notes` | `document_id`; `slide_index` | Get speaker notes text for a slide. |
| Notes | `ppt.set_slide_notes` | `document_id`; `slide_index`; `notes_text` | Set speaker notes text for a slide. |
| Tables | `ppt.add_table` | `document_id`; `slide_index`; `rows`; `cols`; `x`; `y`; `width`; `height` | Add a table to a slide. |
| Tables | `ppt.get_table` | `document_id`; `slide_index`; `shape_index` | Return `{rows, cols, cells, row_heights, col_widths, merged_regions}`. |
| Tables | `ppt.edit_table` | `document_id`; `slide_index`; `shape_index`; `operation`; op-specific fields. | Single operation per call. Operations: `set_cell` (`row`/`col`/`text`), `insert_row`/`delete_row` (`index`), `insert_col`/`delete_col` (`index`), `set_row_height` (`row_index`/`height`), `set_col_width` (`col_index`/`width`), `set_header_style` (`row_index?`/`fill_color?`/`font_color?`/`bold?`), `merge_cells` (`start_row`/`start_col`/`end_row`/`end_col` — overlapping merges → `MERGE_CONFLICT`), `set_cell_border` (`row`/`col`/`sides`/`color`/`width`/`dash_style?`). |
| Shapes | `ppt.move_shape` | `document_id`; `slide_index`; `shape_index`; `x`; `y` | Move a shape to new coordinates. |
| Shapes | `ppt.clone_shape` | `document_id`; `slide_index`; `shape_index`; `offset_x?`; `offset_y?` | Clone any shape on the same slide via deep XML copy and apply offset. |
| Shapes | `ppt.resize_shape` | `document_id`; `slide_index`; `shape_index`; `width`; `height` | Resize a shape to a new width and height. |
| Shapes | `ppt.add_hyperlink` | `document_id`; `slide_index`; `shape_index`; `url` | Attach hyperlink to all text runs in a text shape. |
| Slide styling | `ppt.set_slide_background` | `document_id`; `slide_index`; `color` | Set a solid slide background color. |
| Markdown import | `ppt.import_markdown_outline` | `markdown_text`; `output_path?` | Create a presentation from markdown headings and bullets. |
| Transactions | `ppt.transaction_begin` | `document_id` | Create a transaction snapshot for rollback. |
| Transactions | `ppt.transaction_commit` | `document_id` | Commit changes and discard transaction snapshot. |
| Transactions | `ppt.transaction_rollback` | `document_id` | Roll back document state to transaction snapshot. |
| Metrics | `ppt.get_slide_metrics` | `document_id`; `slide_index` | Analyze slide composition and text density. |
| Rendering | `ppt.render_slide` | `document_id`; `slide_index`; `output_path`; `format?` (`png`\|`jpg`\|`svg`, default `png`); `fidelity?` (`low`\|`high`, default `low`); `width?`; `height?` | Render one slide. `low` = POI + Batik (fast/crude); `high` = LibreOffice (slow/accurate). |
| Rendering | `ppt.render_all_slides` | `document_id`; `output_dir`; `format?`; `fidelity?`; `file_name_pattern?`; `width?`; `height?` | Render every slide as PNG/JPG/SVG files in `output_dir`. |
| Search | `ppt.find_text` | `document_id`; `query`; `case_sensitive?` | Find text occurrences across all slides. |
| Templates | `ppt.upload_template` | `source_path`; `template_name?`; `make_default?` | Copy/upload a template into the template store. |
| Templates | `ppt.set_default_template` | `template_path` | Set default template used by create/generate operations. |
| Templates | `ppt.get_default_template` | (none) | Return current default template configuration. |
| Presentation generation | `ppt.generate_presentation` | `title?`; `slide_titles?`; `template_path?`; `output_path?` | Generate a presentation from titles and optional template. |
| Shapes | `ppt.delete_shape` | `document_id`; `slide_index`; `shape_index` | Remove a shape from a slide by index. |
| Shapes | `ppt.get_shape_properties` | `document_id`; `slide_index`; `shape_index` | Return detailed shape properties (type, anchor, text). |
| Shapes | `ppt.set_shape_style` | `document_id`; `slide_index`; `shape_index`; `fill_type?` (`solid`\|`gradient`\|`pattern`\|`none`); `fill_color?` (solid shorthand); `fill_gradient?` (`type`, `angle?`, `stops[]` ≥2); `fill_pattern?` (`preset`, `fg_color`, `bg_color`); `border_color?`; `border_width?`; `text_align?` | Set fill (solid/gradient/pattern/none), border, and text alignment on a shape. |
| Media | `ppt.set_picture_effects` | `document_id`; `slide_index`; `shape_index`; one or more of `crop` (`left`/`top`/`right`/`bottom` fractions, sum-on-axis < 1), `alpha` (0-1), `recolor` (`mode` ∈ `grayscale`\|`sepia`\|`duotone`\|`washout`, `color?` required for `duotone`). | Apply crop / alpha / recolor effects to a picture shape. Non-picture shapes → `SHAPE_NOT_PICTURE`. |
| Metadata | `ppt.set_document_metadata` | `document_id`; `title?`; `author?`; `subject?`; `keywords?` | Set core document metadata fields. |
| Layout | `ppt.set_slide_layout` | `document_id`; `slide_index`; `layout_type` | Apply a layout type to a slide. |
| Shapes | `ppt.set_shape_z_order` | `document_id`; `slide_index`; `shape_index`; `position` | Move shape in z-order (front/back/forward/backward). |
| Capabilities | `ppt.capabilities` | (none) | Return server / POI / soffice versions, supported formats, installed fonts, feature flags, and safety limits. |

### Feature flags

`ppt.capabilities` reports six flags that gate advanced authoring features. The boolean reflects current build:

| Flag | Default | Gates |
|---|---|---|
| `high_fidelity_render` | dynamic — `true` iff soffice is on PATH | `ppt.render_slide`/`ppt.render_all_slides` with `fidelity=high`, and non-PPTX `ppt.export_document` formats. |
| `gradients` | `true` | `fill_type=gradient` / `pattern` on `ppt.set_shape_style`. |
| `picture_effects` | `true` | `ppt.set_picture_effects` — crop, alpha, recolor. |
| `table_borders` | `true` | `ppt.edit_table` `operation=set_cell_border`. |
| `table_merge` | `true` | `ppt.edit_table` `operation=merge_cells`. |
| `charts_update` | `false` | `ppt.list_charts` / `ppt.update_chart_data` (Phase 5). |

### Safety limits

Enforced server-side and surfaced via `ppt.capabilities.limits`:

| Limit | Default | Error code |
|---|---|---|
| `max_open_docs` | 100 | `LIMIT_MAX_OPEN_DOCS` |
| `max_slides_per_deck` | 2000 | `LIMIT_MAX_SLIDES` |
| `max_shapes_per_slide` | 500 | `LIMIT_MAX_SHAPES` |
| `max_image_bytes` | 52 428 800 (50 MiB) | `LIMIT_MAX_IMAGE_BYTES` |
| `max_render_dimension` | 10 000 px | `LIMIT_MAX_RENDER_DIMENSION` |

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
| `./mvnw verify` | Before declaring a change done. Runs compile + tests + the jacoco coverage gate; also produces the fat jar. |
| `./mvnw -DskipTests package` | Only when you want the fat jar but want to skip tests. |
| `./mvnw clean` | For a truly from-scratch host rebuild. Rarely needed. |

The output is a single fat jar at `target/ppt-mcp-server-all.jar`, runnable with `java -jar`.

## Run

The server speaks MCP over **stdio only** in v1. No HTTP. Launched via the shaded jar:

```bash
java -jar target/ppt-mcp-server-all.jar
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
- Tool surface: 50 tools across document lifecycle, slide/content management, shape/text formatting (incl. gradient/pattern fills, picture effects, table merge/borders), rendering (PNG/JPEG/SVG, dual-fidelity), metadata, layout updates, templating, search, markdown import, and transaction workflows. No native animation, transitions, embedded media timelines, or VBA source inspection.
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
