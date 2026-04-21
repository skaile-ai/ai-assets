---
name: "ppt-mcp-server"
description: "Use when an agent needs to author, modify, review, render, or export PowerPoint (.pptx / .pptm) decks — create/open/close documents, add/duplicate/delete/reorder slides, edit text (with run/paragraph/shape scope + styling), insert images with picture effects, add/edit tables (incl. merge + per-cell borders), draw shapes (solid/gradient/pattern fills), update chart data in existing charts, render slides to PNG/JPG/SVG at low or high fidelity, and export to PPTX/PDF/HTML/PNG-batch/JPG-batch/SVG-batch/outline-text. Invoke via the `ppt-mcp:dev` Docker MCP server over stdio."
metadata:
  stage: "stable"
  version: "1.0.0"
  tags: [powerpoint, pptx, mcp, rendering, apache-poi, libreoffice]
  env_vars:
    MCPO_ALLOWED_ROOT: "Required inside Docker. Sandbox root under which every path argument must resolve. Default in the shipped image: /workspace/resources."
    MCPO_TEMPLATE_DIR: "Optional. Template store for ppt.upload_template. Default: <allowed_root>/.mcpo-ppt/templates."
    MCPO_DEFAULT_TEMPLATE_CONFIG: "Optional. Persisted default-template pointer. Default: <allowed_root>/.mcpo-ppt/default-template.json."
    MCPO_MAX_OPEN_DOCS: "Optional. Concurrent open-session cap. Default: 100."
    SOFFICE_PATH: "Optional. LibreOffice binary path. Default: /usr/bin/soffice (set in the shipped image). If missing, soffice-dependent tools return SOFFICE_UNAVAILABLE."
    LOG_LEVEL: "Optional. Logback root level: ERROR | WARN | INFO | DEBUG. Default: INFO."
---

# PPT MCP Server

## Overview

Stateful MCP server that exposes **52 tools** for manipulating PowerPoint decks through JSON-RPC over stdio. An agent opens or creates a `.pptx`, receives a `document_id` handle, drives mutations through that handle, and exports the result — all within one container session.

Backed by **Apache POI 5.5.x** for PPTX authoring and **LibreOffice (`soffice`)** for high-fidelity rendering and non-PPTX export formats. Ships as the `ppt-mcp:dev` Docker image with fonts (Noto CJK + color emoji + Liberation) baked in.

## When to Use

Invoke this skill when the agent's task involves any of:

- Creating a new PowerPoint deck from titles, a template, or a markdown outline.
- Opening an existing `.pptx` / `.pptm` and making programmatic edits.
- Adding or editing slides, shapes, text, tables, images, or charts within a deck.
- Rendering slides to PNG / JPG / SVG for display in chat UIs.
- Exporting a deck to PDF / HTML / image batch / markdown outline.
- Inspecting deck structure (slide list, shape metrics, text search, chart enumeration).

## When NOT to Use

- **Animations, transitions, slide-master / theme editing, SmartArt, comments, embedded video/audio, VBA inspection** — not in scope for v1.
- **Creating charts from scratch** — only `ppt.list_charts` + `ppt.update_chart_data` are supported in v1 (re-data an existing chart). Authoring new charts is v2.
- **Legacy `.ppt` (binary) or `.odp` input** — not supported.
- **Batch document processing without session affinity** — this is a stateful, one-agent-per-process server. For parallel processing of many decks, run multiple containers.

## Prerequisites

- The `ppt-mcp:dev` Docker image must be built and available on the host (`docker build -t ppt-mcp:dev skaile-platform/mcpo/ppt/`).
- The agent's MCP config must register this server and bind-mount a host directory into `/workspace/resources` so paths in tool arguments resolve inside the sandbox.
- The host user must have write permission to the bind-mounted directory. The recommended invocation uses `--user 1000:1000` so `ppt.export_document` can atomically replace files in the mount.

## Invocation

### MCP client descriptor

```json
{
  "mcpServers": {
    "ppt": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--user", "1000:1000",
        "-v", "/host/workspace:/workspace/resources:rw",
        "ppt-mcp:dev"
      ]
    }
  }
}
```

### Typical session flow

1. Call `ppt.capabilities` once to read the server's versions, feature flags, and safety limits. Branch on flags (`high_fidelity_render`, `charts_update`) if the task needs capabilities that may be absent.
2. Call `ppt.create_document` (with optional `title` / `template_path`) or `ppt.open_document` (with `path` resolved inside `MCPO_ALLOWED_ROOT`). Save the returned `document_id`.
3. Drive mutations through the handle: `ppt.add_slide`, `ppt.add_textbox`, `ppt.set_text`, `ppt.insert_image`, `ppt.add_table`, `ppt.edit_table`, `ppt.set_shape_style`, `ppt.update_chart_data`, etc.
4. (Optional) Use `ppt.transaction_begin` before a multi-step change and `ppt.transaction_commit` / `ppt.transaction_rollback` to keep the deck atomic from the agent's perspective.
5. Export with `ppt.export_document` (`format` = `pptx` | `pdf` | `html` | `png_batch` | `jpg_batch` | `svg_batch` | `outline_text`) or render with `ppt.render_slide` / `ppt.render_all_slides` (`fidelity` = `low` | `high`, `format` = `png` | `jpg` | `svg`).
6. Call `ppt.close_document` to release the handle. Handles don't persist across container restarts — exit is always safe.

## Tool families

See `README.md` for the authoritative catalog with argument schemas. The 52 tools group as:

- **Document lifecycle (5):** create, open, close, get info, export.
- **Page setup (1):** preset + custom dimensions.
- **Slide management (6):** list, add, duplicate, delete, reorder, merge from another deck.
- **Slide content (7):** get content, update/replace text, textbox, unified `set_text`, notes.
- **Media (3):** insert/replace image, picture effects (crop/alpha/recolor).
- **Tables (3):** add, get, unified edit (set cell, insert/delete row/col, row height/col width, header style, merge cells, cell border).
- **Charts (2):** list, update data (series + categories). Creation is v2.
- **Shapes & styling (10):** add, move, resize, clone any shape, delete, get properties, set style (solid/gradient/pattern/none fills), set z-order, add hyperlink, set slide background.
- **Layout & metadata (2):** set slide layout, set document metadata.
- **Rendering (2):** render single slide, render all. Dual-fidelity.
- **Search & metrics (2):** find text, slide metrics.
- **Templates & generation (5):** upload, set/get default, generate from titles, import markdown outline.
- **Transactions (3):** begin, commit, rollback.
- **Capabilities (1):** self-describe — versions, formats, installed fonts, feature flags, safety limits.

## Response envelope

Every tool returns a uniform structured envelope:

```json
{
  "status": "success" | "error",
  "code": "VALIDATION_ERROR" | "PATH_NOT_ALLOWED" | "SOFFICE_UNAVAILABLE" | "...",
  "error": "Human-readable error message (only when status=error)",
  "retriable": false,
  "correlation_id": "<json-rpc-request-id>",
  "tool_name": "ppt.open_document",
  "data": { ... tool-specific payload ... }
}
```

`retriable` is `false` for every v1 code — the server does not signal transient failures in v1. External callers should not auto-retry.

### Key error codes to handle

| Code | Meaning |
|---|---|
| `VALIDATION_ERROR` | Argument fails JSON-schema validation or handler-level input check. |
| `PATH_NOT_ALLOWED` | Path argument resolves outside `MCPO_ALLOWED_ROOT`. Sandbox-escape attempts land here. |
| `DOCUMENT_NOT_FOUND` | `document_id` doesn't refer to an open session. |
| `SLIDE_INDEX_OUT_OF_RANGE` / `SHAPE_INDEX_OUT_OF_RANGE` | Index arguments out of bounds. |
| `SHAPE_NOT_PICTURE` / `SHAPE_NOT_CHART` | Tool targets wrong shape type. |
| `SOFFICE_UNAVAILABLE` | LibreOffice not on PATH. Affects `fidelity=high` rendering and non-`pptx` export formats. Branch on `ppt.capabilities.feature_flags.high_fidelity_render` at session start to avoid this. |
| `LIMIT_MAX_*` | Safety limit hit (`OPEN_DOCS`, `SLIDES`, `SHAPES`, `IMAGE_BYTES`, `RENDER_DIMENSION`). |
| `MERGE_CONFLICT` | `edit_table merge_cells` range overlaps an existing merge. |
| `SERIES_COUNT_MISMATCH` / `CATEGORY_COUNT_MISMATCH` / `EMBEDDED_WORKBOOK_MISSING` | `update_chart_data` precondition failures. |
| `TOOL_EXECUTION_ERROR` | Catch-all for unexpected handler failure. |

See `README.md` → "Error/Response Model" for the complete table.

## Sandbox and paths

Every path argument is resolved inside `MCPO_ALLOWED_ROOT` (set to `/workspace/resources` in the shipped image). Paths outside the sandbox return `PATH_NOT_ALLOWED, retriable=false`. This applies to all of:

- `ppt.open_document` (input `path`)
- `ppt.export_document` (`output_path`)
- `ppt.render_slide` / `ppt.render_all_slides` (`output_path` / `output_dir`)
- `ppt.insert_image` / `ppt.replace_image` (`image_path`)
- `ppt.upload_template` / `ppt.set_default_template` (`source_path` / `template_path`)
- `ppt.merge_presentations` (`merge_path`)
- `ppt.import_markdown_outline` / `ppt.generate_presentation` (`output_path`)

Agents must therefore convert any host-side paths into container-local paths under the bind-mount before calling tools.

## Safety limits

Enforced server-side and surfaced via `ppt.capabilities.limits`:

| Limit | Default | Error code |
|---|---|---|
| `max_open_docs` | 100 | `LIMIT_MAX_OPEN_DOCS` |
| `max_slides_per_deck` | 2000 | `LIMIT_MAX_SLIDES` |
| `max_shapes_per_slide` | 500 | `LIMIT_MAX_SHAPES` |
| `max_image_bytes` | 52 428 800 (50 MiB) | `LIMIT_MAX_IMAGE_BYTES` |
| `max_render_dimension` | 10 000 px | `LIMIT_MAX_RENDER_DIMENSION` |

## Example: generate a simple branded deck

```json
{"jsonrpc":"2.0","id":"1","method":"tools/call","params":{"name":"ppt.create_document","arguments":{"title":"Q1 review"}}}
{"jsonrpc":"2.0","id":"2","method":"tools/call","params":{"name":"ppt.add_textbox","arguments":{"document_id":"doc_abc123","slide_index":0,"text":"Revenue is up 42%","x":50,"y":50,"width":400,"height":80}}}
{"jsonrpc":"2.0","id":"3","method":"tools/call","params":{"name":"ppt.set_text","arguments":{"document_id":"doc_abc123","slide_index":0,"shape_index":1,"bold":true,"font_size":28,"font_color":"#003366"}}}
{"jsonrpc":"2.0","id":"4","method":"tools/call","params":{"name":"ppt.export_document","arguments":{"document_id":"doc_abc123","output_path":"/workspace/resources/q1.pdf","format":"pdf"}}}
{"jsonrpc":"2.0","id":"5","method":"tools/call","params":{"name":"ppt.close_document","arguments":{"document_id":"doc_abc123"}}}
```

## Common mistakes

| Rationalization | Reality |
|---|---|
| "I'll pass the host path directly to `ppt.open_document`" | Paths are evaluated inside the container. Host `./resources/demo.pptx` is mounted at `/workspace/resources/demo.pptx`. |
| "I can `save_document` the deck" | The tool is **`ppt.export_document`** since v1. `ppt.save_document` no longer exists. |
| "I'll call `ppt.set_text_style` for bold" | Four Phase-0 text tools were merged into the single `ppt.set_text` with `scope=shape|run|paragraph`. |
| "I'll render at `fidelity=high` for fast previews" | High-fidelity routes through LibreOffice and takes seconds per slide. Use `fidelity=low` (POI + Batik) for fast previews. |
| "The document handle persists across runs" | Handles live only for the container's lifetime. A second `docker run` starts fresh. Always save to disk with `ppt.export_document` before the process exits. |

## Integration

- **Called from:** any Skaile agent that produces or consumes PowerPoint artefacts.
- **Pairs with:** file-upload / download skills on the agent side (the agent places input `.pptx` files under the bind-mount and reads exported output from the same location).
- **Does not call:** other Skaile skills. The server is self-contained.

## Troubleshooting

- **Every path-bearing call returns `PATH_NOT_ALLOWED`:** the bind-mount isn't wired through or `MCPO_ALLOWED_ROOT` doesn't match the mount. Verify `-v` maps to `/workspace/resources` and that the in-tool path starts with `/workspace/resources/`.
- **PDF/HTML/image-batch export returns `SOFFICE_UNAVAILABLE`:** the image was built without LibreOffice, or `SOFFICE_PATH` points at a missing binary. Check `ppt.capabilities.soffice_available`.
- **High-fidelity CJK or emoji text renders as tofu:** fonts weren't baked into the image. The shipped `Dockerfile` installs `fonts-noto fonts-noto-cjk fonts-noto-color-emoji fonts-liberation` — verify the image was built from the shipped Dockerfile.
- **`ppt.export_document format=pptx` can't overwrite a host file:** the container isn't running as a user that can replace the file. Add `--user 1000:1000` (or match your host UID).
- **Concurrent calls on the same `document_id` appear to serialize:** by design. POI's `XMLSlideShow` DOM is not thread-safe, so a per-session `ReentrantLock` guards every call. Concurrent calls on *different* `document_id` values run in parallel.
