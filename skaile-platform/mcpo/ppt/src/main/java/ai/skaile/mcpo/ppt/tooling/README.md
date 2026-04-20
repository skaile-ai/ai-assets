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

- `PptDocumentOperations.java` — document lifecycle, export, transactions,
  merge/generate.
- `PptSlideOperations.java` — slide content + text (add/duplicate/delete,
  update_text, notes, get_slide_content).
- `PptShapeMutationOperations.java` — shape add/move/resize/clone/delete,
  style, z-order, hyperlink, replace_image.
- `PptTableOperations.java` — table add/get/edit + `set_text` styling.
- `PptPageOperations.java` — set_page_setup, set_slide_background,
  set_slide_layout, set_document_metadata.
- `PptRenderOperations.java` — render_slide, render_all_slides, find_text,
  get_slide_metrics.
- `PptTemplateOperations.java` — insert_image, template upload/default,
  import_markdown_outline. Owns the mutable default-template pointer.
- `PptCapabilitiesOperations.java` — the `ppt.capabilities` self-describe
  handler (versions, feature flags, limits, font probe).
- `PptTransactionManager.java` — per-session transaction snapshots.

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

Structured error codes the dispatcher emits automatically:

| Exception thrown by handler                      | Response code                |
|--------------------------------------------------|------------------------------|
| `ColorParser.InvalidColorException`              | `INVALID_COLOR`              |
| `PptShapeFinder.DocumentNotFoundException`       | `DOCUMENT_NOT_FOUND`         |
| `PptShapeFinder.SlideIndexOutOfRangeException`   | `SLIDE_INDEX_OUT_OF_RANGE`   |
| `PptShapeFinder.ShapeIndexOutOfRangeException`   | `SHAPE_INDEX_OUT_OF_RANGE`   |
| `IllegalArgumentException` (any other)           | `VALIDATION_ERROR`           |
| any other `Exception`                            | `TOOL_EXECUTION_ERROR`       |

Handlers that emit a limit-related error use `PptLimits.enforce*` which returns
a prebuilt `ToolCallResult` with the corresponding `LIMIT_*` code.
