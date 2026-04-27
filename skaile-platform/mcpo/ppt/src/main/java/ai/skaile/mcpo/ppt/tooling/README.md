# tooling package layout

This directory is organized by role. The entry point is `PptToolService` — a
thin dispatch facade that wires together operations classes, enforces the
per-session lock, and translates typed exceptions into structured error codes.
It holds no tool business logic.

## top level
- `PptToolService.java` — tool registry, dispatch, typed-exception → error-code
  translation, per-session lock decorator. Target: under 300 lines.
- `PptToolDefinitions.java` — JSON-schema tool catalog consumed by
  `tools/list`. The single source of truth for the tool surface shape.

## operations/
Each operations class is self-contained: real dependencies injected via
constructor (no lambda-reference pass-through), owns its handler methods
directly, and exposes a `handlers()` map from tool name to `ToolHandler`.
`PptToolService` merges every class's `handlers()` map at construction.

- `PptDocumentOperations.java` — document lifecycle, `export_document` (pptx
  native + pdf/html/*_batch/outline_text via `SofficeRenderer`), transactions,
  merge/generate.
- `PptSlideOperations.java` — slide content (add/duplicate/delete,
  `update_text`, `replace_text_globally`, `add_textbox`, notes,
  `get_slide_content`).
- `PptShapeMutationOperations.java` — shape add/move/resize/clone (any shape
  via deep XML copy)/delete, style (solid/gradient/pattern/none fills),
  `set_picture_effects` (crop/alpha/recolor), z-order, hyperlink,
  `replace_image`.
- `PptTableOperations.java` — table add/get/edit including `merge_cells` +
  `set_cell_border`.
- `PptTextOperations.java` — `ppt.set_text` only. Split out of
  `PptTableOperations` in Phase 4 so the table class stays under the
  800-line cap when `strikethrough` / `rotation` / `auto_fit` were added.
- `PptPageOperations.java` — `set_page_setup`, `set_slide_background`,
  `set_slide_layout`, `set_document_metadata`.
- `PptRenderOperations.java` — `render_slide`, `render_all_slides` (both
  honour `fidelity` + `format`), `find_text`, `get_slide_metrics`.
- `PptTemplateOperations.java` — `insert_image`, template upload/default,
  `import_markdown_outline`. Owns the mutable default-template pointer
  consulted by `PptDocumentOperations` on session creation.
- `PptChartOperations.java` — Phase 5: `list_charts` + `update_chart_data`.
  Writes both the chart's numCache/strCache XML and the embedded XLSX
  workbook cells so PowerPoint's "Edit Data" dialog stays consistent.
  Chart creation is v2.
- `PptCapabilitiesOperations.java` — the `ppt.capabilities` self-describe
  handler (versions, feature flags, limits, font probe).
- `PptTransactionManager.java` — per-session transaction snapshots (no
  tools of its own; drives `ppt.transaction_{begin,commit,rollback}` via
  `PptDocumentOperations`).
- `SofficeRenderer.java` — single entry point for all `soffice` shell-outs
  (PDF/HTML/batch export, `fidelity=high` render). Semaphore-guarded to 1
  concurrent invocation, 90-second timeout, temp-file cleanup on every
  exit path. Throws `SofficeUnavailableException` which
  `PptToolService.call()` translates to `SOFFICE_UNAVAILABLE`. No tools
  of its own.

## contracts/
- `ToolHandler.java` — functional handler interface.
- `ToolDefinition.java` — tool metadata/schema contract.
- `ToolCallResult.java` — standardized tool-call result record.

## infra/
Shared utilities. Nothing here knows about tool semantics; each file is a
narrow primitive reusable across operations classes.

- `PptServerConfig.java` — the only file that reads `System.getenv`. Holds
  allowed-root, templates dir, default-template config path, max open docs,
  soffice path, java version.
- `PptPathResolver.java` — allowed-root-aware path resolution, sandbox temp
  files, image-format inference, template-name sanitization.
- `PptShapeFinder.java` — resolves `(document_id, slide_index, shape_index)` to
  typed objects; throws `DocumentNotFoundException` /
  `SlideIndexOutOfRangeException` / `ShapeIndexOutOfRangeException` which
  `PptToolService.call()` translates into uniform error codes.
- `PptLimits.java` — safety limits (max slides, shapes-per-slide, image bytes,
  render dimensions) and the `enforce*` helpers that emit `LIMIT_*` errors.
- `PptSlideBuilder.java` — shared slide-construction helpers (createDefaultSlide,
  setSlideTitle, addBodyText, collectSlideText).
- `ColorParser.java` — the single hex-color parser. Malformed input surfaces as
  `INVALID_COLOR`.
- `PptUnits.java` — EMU ↔ points ↔ pixels conversions. The only place raw
  `914400 EMU/inch` appears.
- `PptShapeXml.java` — Phase 4 helpers for XML-level things POI doesn't
  expose at its facade: gradient/pattern fills on `CTShapeProperties`,
  picture crop/alpha/recolor on `CTBlipFillProperties`, shape-id
  management for clone.
- `SofficeAvailability.java` — one-shot LibreOffice probe, cached process-wide.
- `ToolArgumentValidator.java` — argument normalization and JSON-schema checks.
- `ToolResponseFactory.java` — success/error payload construction.

## Adding a new tool

Adding a tool is a **two-file edit**:

1. Declare the tool schema in `PptToolDefinitions.create(...)`.
2. Add a handler method and a `Map.of("ppt.your_tool", this::yourTool)` entry
   inside the relevant operations class's `handlers()` map.

You should **not** need to edit `PptToolService` to register the new tool.

## Error codes

Structured error codes the central dispatcher emits automatically when a
handler throws a typed exception:

| Exception thrown by handler                      | Response code                |
|--------------------------------------------------|------------------------------|
| `PptPathResolver.PathNotAllowedException`        | `PATH_NOT_ALLOWED`           |
| `ColorParser.InvalidColorException`              | `INVALID_COLOR`              |
| `PptShapeFinder.DocumentNotFoundException`       | `DOCUMENT_NOT_FOUND`         |
| `PptShapeFinder.SlideIndexOutOfRangeException`   | `SLIDE_INDEX_OUT_OF_RANGE`   |
| `PptShapeFinder.ShapeIndexOutOfRangeException`   | `SHAPE_INDEX_OUT_OF_RANGE`   |
| `SofficeRenderer.SofficeUnavailableException`    | `SOFFICE_UNAVAILABLE`        |
| `IllegalArgumentException` (any other)           | `VALIDATION_ERROR`           |
| any other `Exception`                            | `TOOL_EXECUTION_ERROR`       |

Handlers can also emit codes directly by returning
`responseFactory.error(code, message, retriable)`. The domain-specific codes
in current use:

| Code                         | Emitted when                                                         | Retriable |
|------------------------------|----------------------------------------------------------------------|-----------|
| `VALIDATION_ERROR`           | Argument fails schema or handler-level input check                   | no        |
| `MERGE_CONFLICT`             | `edit_table merge_cells` overlaps an existing merge                  | no        |
| `SHAPE_NOT_PICTURE`          | `set_picture_effects` target isn't an `XSLFPictureShape`             | no        |
| `SHAPE_NOT_CHART`            | `update_chart_data` target isn't a chart graphic frame               | no        |
| `SERIES_COUNT_MISMATCH`      | `update_chart_data` series count ≠ chart's                           | no        |
| `CATEGORY_COUNT_MISMATCH`    | `update_chart_data` values/categories count ≠ chart's                | no        |
| `EMBEDDED_WORKBOOK_MISSING`  | Chart has no embedded XLSX (legacy format)                           | no        |
| `LIMIT_MAX_OPEN_DOCS`        | `MCPO_MAX_OPEN_DOCS` reached                                         | no        |
| `LIMIT_MAX_SLIDES`           | Deck would exceed `PptLimits.MAX_SLIDES`                             | no        |
| `LIMIT_MAX_SHAPES`           | Slide would exceed `PptLimits.MAX_SHAPES_PER_SLIDE`                  | no        |
| `LIMIT_MAX_IMAGE_BYTES`      | Image larger than `PptLimits.MAX_IMAGE_BYTES`                        | no        |
| `LIMIT_MAX_RENDER_DIMENSION` | Render width/height above `PptLimits.MAX_RENDER_DIMENSION`           | no        |

The single-arg `responseFactory.error(message)` shortcut emits
`TOOL_EXECUTION_ERROR` — the same code the dispatcher uses for uncaught
exceptions — so callers see one "unexpected handler failure" code regardless
of path. Prefer a specific code when one applies.

Handlers that emit a limit-related error use `PptLimits.enforce*` which
returns a prebuilt `ToolCallResult` with the corresponding `LIMIT_*` code.
