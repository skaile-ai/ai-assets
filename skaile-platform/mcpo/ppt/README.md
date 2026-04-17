# PPT MCP Server (Apache POI)

Stateful MCP server for PowerPoint automation over stdio JSON-RPC, implemented in Java with Apache POI.

## Current Status

This module is functionally complete for PowerPoint-focused MCP workflows:

- Stateful open/create/edit/close using `document_id` handles.
- Read/query slide and document content.
- Mutate slide content (text, text boxes, images, notes, tables, text style).
- Save/export (`pptx`, `pdf`).
- Render full slide or selected shapes (`png/jpg`, `svg`) for chat previews.
- Runtime argument/schema validation and structured error payloads.

This module is not yet fully enterprise-hardened (see "Known Gaps").

## Architecture at a Glance

- `McpServer`: JSON-RPC request handling (`initialize`, `tools/list`, `tools/call`).
- `JsonRpcIO`: newline-delimited stdio transport (with legacy framed-input compatibility).
- `PptToolService`: tool registry, validation, and implementation logic.
- `SessionStore`: in-memory document sessions (`doc_<uuid>` handles).
- `PptDocumentSession`: session metadata (`openedAt`, `updatedAt`, `dirty`, `sourcePath`).

Design choices:

- Tools are registered with JSON schema and validated at runtime before execution.
- All mutating operations mark the document session dirty.
- Server shutdown attempts best-effort cleanup of all open sessions.

## Tools Exposed

- `ppt.create_document`
- `ppt.open_document`
- `ppt.close_document`
- `ppt.get_document_info`
- `ppt.list_slides`
- `ppt.get_slide_content`
- `ppt.add_slide`
- `ppt.update_text`
- `ppt.add_textbox`
- `ppt.insert_image`
- `ppt.get_slide_notes`
- `ppt.set_slide_notes`
- `ppt.add_table`
- `ppt.get_table_cell`
- `ppt.set_table_cell`
- `ppt.set_text_style`
- `ppt.save_document`
- `ppt.render_slide_image`
- `ppt.render_slide_svg`
- `ppt.render_selection_image`
- `ppt.render_selection_svg`
- `ppt.find_text`
- `ppt.upload_template`
- `ppt.set_default_template`
- `ppt.get_default_template`
- `ppt.generate_presentation`

## Error/Response Model

Tool payloads include:

- `status`: `success` or `error`
- `code`: structured error code for failures (for example `VALIDATION_ERROR`)
- `error`: message for failures
- `retriable`: whether retry is expected to help
- `correlation_id`: propagated from JSON-RPC request id
- `tool_name`: called tool name

## Build and Run

Build/test:

```bash
mvn test
mvn package
```

Run:

```bash
java -jar target/ppt-mcp-server-all.jar
```

Artifact:

- `target/ppt-mcp-server-all.jar`

## Test With MCP Inspector (Docker)

Run this from the `ai-assets` repository root:

```bash
npx @modelcontextprotocol/inspector \
  docker run --rm -i \
  -v "$PWD/skaile-platform/mcpo/ppt/resources:/workspace/resources" \
  ppt-mcp-server:local
```

Document storage behavior:

- `ppt.create_document` creates a presentation in memory only.
- New documents are written to disk only when you call `ppt.save_document` with `output_path`.
- If a document was opened from disk (`ppt.open_document`), `ppt.save_document` without `output_path` overwrites the original file path.

## External Dependencies

- Apache POI: PPTX manipulation.
- Apache Batik: SVG rendering.
- LibreOffice (`soffice`): PDF conversion. The Docker image installs it and sets `SOFFICE_PATH=/usr/bin/soffice`.

If `soffice` is missing, PDF export fails with a tool error.

## Template Workflow

- Upload a template with `ppt.upload_template`.
- Select default template with `ppt.set_default_template`.
- Inspect active default using `ppt.get_default_template`.
- `ppt.create_document` and `ppt.generate_presentation` can use explicit `template_path` or fallback default.

## Test Coverage

`PptToolServiceTest` currently validates:

- create/open/add/save/reopen flow
- text update and image render flow
- text search + selection render flow
- template upload/default/generate flow
- path-like template rejection
- image insertion
- notes set/get
- table set/get
- text style mutation
- runtime schema rejection on unexpected fields

## Known Gaps (Important)

These do not block core functionality but matter for production deployment:

- Session state is in-memory only (no persistence across restart).
- No multi-instance distributed locking/versioning for collaboration conflicts.
- No integrated authn/authz policy hooks yet.
- No full telemetry stack (metrics/tracing dashboarding).
- Limited operational controls for queueing/backpressure and per-tool time budgets.

## Next Dev Priorities

1. Add persistent session backend (Redis or DB) with restart recovery.
2. Add optimistic versioning and conflict detection per document.
3. Add policy hooks and audit event sink for regulated environments.
4. Add worker queue/backpressure and configurable tool timeouts.
5. Add health/readiness/metrics integration and deployment runbook.

## Quick Example Calls

Open a file:

```json
{
  "name": "ppt.open_document",
  "arguments": {
    "path": "C:/data/demo.pptx"
  }
}
```

Insert image:

```json
{
  "name": "ppt.insert_image",
  "arguments": {
    "document_id": "doc_...",
    "slide_index": 0,
    "image_path": "C:/data/logo.png",
    "x": 80,
    "y": 80,
    "width": 240,
    "height": 120
  }
}
```

Render slide as SVG:

```json
{
  "name": "ppt.render_slide_svg",
  "arguments": {
    "document_id": "doc_...",
    "slide_index": 0,
    "output_path": "C:/data/slide-1.svg",
    "width": 1920,
    "height": 1080
  }
}
```
