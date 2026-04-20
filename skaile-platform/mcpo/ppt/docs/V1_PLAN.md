# PPT MCP Server — v1 Production-Ready Plan

Execution plan for taking the PPT MCP server from its current state to a v1 production-ready release. Phases are sequential; one executor agent per phase.

## Scope decisions (locked)

- **Production-ready** means (a) feature completeness against Apache POI 5.5.x + LibreOffice in the existing Docker image, and (b) defect fixes. Out of scope: authn/authz, Prometheus/OTel telemetry, rate limiting, per-tool timeouts, session persistence (Redis/DB), multi-session-per-process scaling.
- **Charts**: data-update-only for v1. Create-from-scratch is v2.
- **Transitions/animations, slide master/theme editing, group shapes/connectors, comments, video/audio embed, SmartArt**: not in v1.
- **Included for v1**: gradient/pattern fills, picture effects (crop, alpha, recolor), table cell merge + per-cell borders, text strikethrough/rotation/auto-fit.
- **Rendering**: dual-fidelity. `fidelity: "low"` = POI (fast, crude); `fidelity: "high"` = LibreOffice (slow, accurate).
- **Export formats** (for a business chat-agent use case): `pptx`, `pdf`, `html` (single file with embedded images), `png_batch`, `jpg_batch`, `svg_batch`, `outline_text` (markdown-like). No ODP.
- **Fonts in Docker image**: free/commercial-use only — `fonts-noto`, `fonts-noto-cjk`, `fonts-noto-color-emoji`, `fonts-liberation`. No MS Core Fonts.
- **Tool surface**: not frozen. Consolidation encouraged. Net target: 56 → ~42 tools.
- **New tool**: `ppt.capabilities` (self-describe).
- **No `ppt.batch_operations`.**
- **Safety limits (sensible defaults)**: 100 open docs, 2000 slides/deck, 500 shapes/slide, 50 MB/image, 10000×10000 px render, 1 concurrent soffice invocation per container.
- **Per-tool timeouts**: not for v1.
- **Coverage**: formal, enforced via jacoco in `mvn verify`. Gates: 70% after Phase 0, 80% after Phase 1, 85% after Phase 5.
- **Docker smoke**: must exercise every tool family end-to-end by Phase 5.
- **Observability**: logs-only.
- **Release / CI**: no changes to `.github/`, no image publishing. Image is company-internal, dev-built (`docker build -t ppt-mcp:dev .`). Enforcement is local `mvn verify` + the Docker smoke test.
- **Process**: single executor agent, sequential. One PR per phase, merged to `main` before the next phase starts.

## Current-state summary (audit baseline)

All 56 registered tools have real handlers — no STUB/MISSING. Only `clone_shape` is PARTIAL (text-capable shapes only, `PptAdvancedMutationOperations.java:923-968`). 30 unit tests cover happy paths; transactions, error paths, and advanced styling lack coverage.

**Production-blocking defects** (fixed in Phase 0):
- `XMLSlideShow` mutation is not thread-safe; `SessionStore` has no per-session lock — corruption risk on concurrent calls with same `document_id`.
- PDF export writes to system `/tmp` via `Files.createTempFile`, bypassing `MCPO_ALLOWED_ROOT`.
- Silent catches: `PptToolService.loadDefaultTemplatePath` (~1567), `SessionStore.closeAll` (49-51), `exportPdfWithSoffice` stderr not captured (~1631).
- No `soffice` startup probe — PDF fails at first call if soffice absent.
- Dockerfile ships only `fonts-dejavu-core`; non-Latin and emoji render as tofu.
- Doc/source drift: `README.md` references `./mvnw` (not committed) and Java 21 (`pom.xml` targets 17); Spotless/AssertJ in README but absent from pom.

**Architectural smells** (not blocking, addressed opportunistically during phases): `PptToolService` at ~1790 lines; operations classes are lambda pass-through facades.

---

# Phase 0 — Foundation (defects + Docker hardening + test infra)

**Goal**: fix what's wrong and establish the test/coverage floor before any feature work.

**Scope**:

1. **Per-session lock**. Add `ReentrantLock` field on `PptDocumentSession`. Acquire in a single dispatch decorator inside `PptToolService.call()`: look up session by `document_id` argument, lock, invoke handler, release in `finally`. Read-only tools (`get_document_info`, `list_slides`, `get_slide_content`, `find_text`, `get_slide_metrics`, `get_shape_properties`, `get_slide_notes`, capability queries) take the same lock — POI's DOM is not thread-safe even on reads. Tools without a `document_id` (`capabilities`, template management, `generate_presentation`, `import_markdown_outline`) bypass the decorator.

2. **Sandbox-local temp files**. Add `PptPathResolver.createSandboxTempFile(prefix, suffix)` that writes under `${MCPO_ALLOWED_ROOT}/.mcpo-ppt/tmp/` when allowedRoot is set, falling back to `Files.createTempFile` only when unset. Refactor every `Files.createTempFile` call in `PptToolService` (PDF export path and any other) to use it. Clean up the temp directory on session close.

3. **Remove silent catches**.
   - `PptToolService.loadDefaultTemplatePath`: log the caught exception to stderr (SLF4J), then continue with `null`.
   - `SessionStore.closeAll`: aggregate per-session failures into a returned count + log each via SLF4J.
   - `PptToolService.exportPdfWithSoffice`: capture stderr (currently only stdout is redirected) and include it verbatim in the error payload's `error` field.

4. **`soffice` availability probe**. New `tooling/infra/SofficeAvailability.java` singleton. On `McpServer` startup, run `soffice --headless --version` with 5s timeout, cache `{available: boolean, version: String}`. Called from `saveDocument` PDF branch (Phase 0) and high-fidelity render / non-PPTX export paths (Phase 2). When unavailable, return `{status:"error", code:"SOFFICE_UNAVAILABLE", retriable:false}`.

5. **Dockerfile fonts**: add `fonts-noto fonts-noto-cjk fonts-noto-color-emoji fonts-liberation` to the `apt-get install` line, followed by `fc-cache -fv` in the same RUN layer.

6. **Fix doc/source drift**:
   - Commit Maven Wrapper pinned to **3.9.9** (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `.mvn/wrapper/maven-wrapper.jar`) so `./mvnw verify` (per README) actually works.
   - Standardize on Java **17** as the compile target (`maven.compiler.release=17` stays); keep Java 21 runtime unchanged. Update README's "Java 21 LTS" tech-stack reference to "Java 17 bytecode, Java 21 runtime".
   - Remove Spotless and AssertJ mentions from README (they are not in pom.xml).

7. **Test infra**:
   - Add `jacoco-maven-plugin` to `pom.xml`, report generation in `verify` phase.
   - Coverage gate: **70%** line coverage on `ai.skaile.mcpo.ppt.tooling.*` and `ai.skaile.mcpo.ppt.session.*`. Build fails if below.
   - Gate is enforced from pom — no CI dependency.

**Files touched**: `session/PptDocumentSession.java`, `session/SessionStore.java`, `server/McpServer.java`, `tooling/PptToolService.java`, `tooling/infra/PptPathResolver.java`, new `tooling/infra/SofficeAvailability.java`, `Dockerfile`, `README.md`, `pom.xml`, new `mvnw` + `.mvn/` wrapper files. **No changes to `.github/`.**

**Acceptance**:
- `./mvnw verify` runs and enforces the 70% gate.
- New unit test demonstrates concurrent `update_text` on the same `document_id` serializes without corruption.
- Deck containing CJK + emoji text renders without tofu (rendering path tested in Phase 2, but fonts must be in place now so soffice finds them).
- With soffice missing, `ppt.save_document format=pdf` returns `code=SOFFICE_UNAVAILABLE, retriable=false`.
- All `Files.createTempFile` call sites now route through `PptPathResolver.createSandboxTempFile`.
- README drift resolved; `./mvnw verify` works from a clean checkout.

---

# Phase 1 — Tool surface consolidation + `ppt.capabilities`

**Goal**: collapse overlapping tools into a smaller, better-shaped API; add self-description; raise coverage gate. Net change: **56 → ~42 tools**.

**Removes & replacements**:

- **Text (4 → 1)**. Drop `ppt.set_text_style`, `ppt.set_text_run_style`, `ppt.set_text_formatting`, `ppt.set_list_formatting`. Add `ppt.set_text`:
  - Required: `document_id`, `slide_index`, `shape_index`.
  - Selector: `scope: "shape"` (default) | `"run"` | `"paragraph"`. For `run`: `target_text`, optional `occurrence`, optional `case_sensitive`. For `paragraph`: `paragraph_index`.
  - Run-style block: `bold?, italic?, underline?, strikethrough?, font_size?, font_color?, font_family?, rotation?, auto_fit?`.
  - Paragraph-style block (ignored when scope="run"): `text_align?, line_spacing?, space_before?, space_after?, left_margin?, indent?, bullet_enabled?, numbered?, bullet_character?, bullet_level?`.
  - `rotation`, `auto_fit`, `strikethrough` accept the schema in Phase 1 but implementation lands in Phase 3.

- **Tables (6 → 2)**. Drop `ppt.get_table_cell`, `ppt.set_table_cell`, `ppt.modify_table_structure`, `ppt.set_table_row_height`, `ppt.set_table_column_width`, `ppt.set_table_header_style`. Add:
  - `ppt.get_table` → returns `{rows, cols, cells: [[{text, row_span, col_span, is_merge_anchor}]], row_heights, col_widths, merged_regions}`.
  - `ppt.edit_table` with `operation: "set_cell" | "insert_row" | "delete_row" | "insert_col" | "delete_col" | "set_row_height" | "set_col_width" | "set_header_style" | "merge_cells" | "set_cell_border"`. Each operation only requires its own args. (`merge_cells` + `set_cell_border` implementation lands in Phase 3; schema is defined now.)

- **Render (3 → 2)**. Drop `ppt.render_slide_svg`. Extend `ppt.render_slide` and `ppt.render_all_slides` with:
  - `format: "png" | "jpg" | "svg"` (default `png`).
  - `fidelity: "low" | "high"` (default `low`). High-fidelity implementation lands in Phase 2; schema is defined now.

- **Save (renamed & extended)**. Rename `ppt.save_document` → `ppt.export_document`. Add `format: "pptx" | "pdf" | "html" | "png_batch" | "jpg_batch" | "svg_batch" | "outline_text"`. In Phase 1, only `pptx` and `pdf` are implemented (unchanged behavior); other formats return `{code:"FORMAT_NOT_YET_IMPLEMENTED", retriable:false}` until Phase 2 lands them.

**Adds**:

- **`ppt.capabilities`** (no args) →
  ```
  {
    server_version, poi_version, soffice_version?, soffice_available,
    java_version,
    supported_input_formats: ["pptx", "pptm"],
    supported_export_formats: [...],
    supported_render_formats: [...],
    installed_fonts: [string, ...]  // first 50 via `fc-list :family`, cached at startup
    feature_flags: {
      charts_update: false,           // true after Phase 4
      high_fidelity_render: false,    // true after Phase 2
      gradients: false,               // true after Phase 3
      picture_effects: false,         // true after Phase 3
      table_borders: false,           // true after Phase 3
      table_merge: false              // true after Phase 3
    },
    limits: {
      max_open_docs: 100,
      max_slides_per_deck: 2000,
      max_shapes_per_slide: 500,
      max_image_bytes: 52428800,
      max_render_dimension: 10000
    }
  }
  ```

**Keeps unchanged**: document lifecycle (5), slide management (6), `get_slide_content`, `update_text`, `replace_text_globally`, `add_textbox`, `get_slide_notes`, `set_slide_notes`, `add_table`, `add_shape`, `move_shape`, `resize_shape`, `delete_shape`, `get_shape_properties`, `add_hyperlink`, `insert_image`, `replace_image`, `find_text`, `get_slide_metrics`, `set_shape_style` (extended in Phase 3), `set_shape_z_order`, `set_slide_background`, templates (4), `generate_presentation`, `import_markdown_outline`, `set_slide_layout`, `set_document_metadata`, transactions (3). `clone_shape` stays PARTIAL until Phase 3.

**Enforcement of limits**: `PptToolService` rejects creates/adds beyond the limits from `capabilities.limits` with structured codes: `LIMIT_MAX_OPEN_DOCS`, `LIMIT_MAX_SLIDES`, `LIMIT_MAX_SHAPES`, `LIMIT_MAX_IMAGE_BYTES`, `LIMIT_MAX_RENDER_DIMENSION`. All non-retriable.

**Files touched**: `tooling/PptToolDefinitions.java`, `tooling/PptToolService.java`, all `tooling/operations/*.java` as needed, `tooling/infra/ToolArgumentValidator.java` if new coercions are needed, `README.md` (tool catalog rewrite).

**Acceptance**:
- `ppt.capabilities` returns a populated object with accurate `poi_version` and `soffice_version` (if available).
- All old behaviors are reachable via the new API (documented in README).
- Smoke + unit tests updated to new tool names and passing.
- Coverage gate raised to **80%**.

---

# Phase 2 — Dual-fidelity rendering + full export format set

**Goal**: fast-crude render stays; slow-accurate becomes available; every chat-useful export format is wired up.

**Scope**:

1. **`SofficeRenderer`** (new `tooling/operations/SofficeRenderer.java`). Service that, for any `fidelity="high"` render or any non-PPTX export:
   - Writes the current in-memory `XMLSlideShow` to a sandbox temp `.pptx` via `createSandboxTempFile`.
   - Shells to `soffice --headless --convert-to <fmt> --outdir <sandbox_tmp> <temp.pptx>` with the 90-second timeout already used by `exportPdfWithSoffice`.
   - For single-slide high-fidelity render: extract the target slide into a single-slide temp deck first (duplicate show, delete all other slides) before converting, avoiding post-filter of N files.
   - Concurrency: guard with a shared `Semaphore(1)` — one soffice invocation per container at a time. LibreOffice headless is not safe for concurrent invocations in one user profile.
   - Stderr captured on non-zero exit and surfaced in the error payload.
   - Checks `SofficeAvailability` first; returns `SOFFICE_UNAVAILABLE` if not available.

2. **`ppt.export_document` completed** for all formats:
   - `pptx` — existing POI atomic write (temp-then-move) stays.
   - `pdf` — existing soffice path, now routed through `SofficeRenderer`.
   - `html` — `soffice --convert-to html` (produces single `.html` with embedded base64 images).
   - `png_batch` / `jpg_batch` / `svg_batch` — `soffice --convert-to png` (etc.) into `output_dir` (one file per slide, soffice handles batching natively). If `output_path` is a file path instead of a dir, reject with `VALIDATION_ERROR`.
   - `outline_text` — POI traversal: for each slide, emit `# <title>\n\n- <body bullet>\n- …\n\n`. Deterministic, no soffice.

3. **`ppt.render_slide` / `ppt.render_all_slides`** honor `fidelity="high"` via `SofficeRenderer`.

4. **Temp-file cleanup**: `SofficeRenderer` cleans up its sandbox temp files after each invocation, regardless of success.

5. **Feature flag flip**: `capabilities.feature_flags.high_fidelity_render = true`.

6. **Unit tests**: each export format; skip soffice-dependent tests via `Assumptions.assumeTrue(SofficeAvailability.get().available())` when soffice is missing locally.

7. **Docker smoke additions**: export `pdf`, `html`, `outline_text`; high-fidelity render of a slide containing CJK text (expect it visually correct — assert non-blank PNG with pixel sampling).

**Files touched**: new `tooling/operations/SofficeRenderer.java`, new `tooling/operations/PptExportService.java` (or extend existing document operations), `tooling/PptToolDefinitions.java`, `tooling/PptToolService.java`.

**Acceptance**:
- All seven export formats round-trip in tests.
- High-fidelity render of CJK+emoji text produces visually correct output (Phase 0 fonts in effect).
- Semaphore prevents parallel soffice races (tested with two concurrent `export_document format=pdf` calls).
- Coverage gate still ≥80%.

---

# Phase 3 — Authoring expansion

**Goal**: land everything on the "moderate effort, high value" row of the feature matrix.

**Scope**:

1. **Gradient & pattern fills** on `ppt.set_shape_style`:
   - New fields:
     - `fill_type: "solid" | "gradient" | "pattern" | "none"`.
     - `fill_gradient: { type: "linear" | "radial", angle?: number (degrees, linear only), stops: [{color: "#hex", position: 0.0-1.0}] }`. Minimum 2 stops.
     - `fill_pattern: { preset: "horizontal" | "vertical" | "diagonal_up" | "diagonal_down" | "cross" | "dotted", fg_color, bg_color }`.
   - Implementation via `XSLFSimpleShape` → underlying `CTShapeProperties` → `a:gradFill` / `a:pattFill` XML.
   - `fill_color` remains as shorthand for `fill_type=solid`.

2. **Picture effects** — new tool `ppt.set_picture_effects`:
   - Args: `document_id, slide_index, shape_index` + at least one of:
     - `crop: { left, top, right, bottom }` (fractions in [0,1]; 0 = no crop; values sum-on-axis must be < 1).
     - `alpha: 0.0-1.0` (0 = fully transparent).
     - `recolor: { mode: "grayscale" | "sepia" | "duotone" | "washout", color?: "#hex" }` (color required for `duotone`).
   - Validates shape is `XSLFPictureShape`; else `SHAPE_NOT_PICTURE, retriable=false`.
   - Implementation via direct XML: `a:srcRect` (crop), `a:alphaModFix val=<pct × 1000>` (alpha), `a:lum` / `a:duotone` blip effects (recolor).

3. **Table extensions** via `ppt.edit_table`:
   - `operation: "merge_cells"` — args: `start_row, start_col, end_row, end_col`. Sets `gridSpan`/`rowSpan` on the anchor cell; `hMerge="1"`/`vMerge="1"` on covered cells. Rejects overlapping merges with `MERGE_CONFLICT, retriable=false`.
   - `operation: "set_cell_border"` — args: `row, col, sides: ("top"|"bottom"|"left"|"right"|"all")[], color: "#hex", width: points, dash_style?: "solid"|"dash"|"dot"|"dashdot"`. Manipulates `a:lnT/B/L/R` with `a:prstDash`.

4. **Text extensions** — wire the implementations behind the Phase-1 `ppt.set_text` schema:
   - `strikethrough: true|false` → `ctTextCharPR.setStrike(STStrikeType.SNG_STRIKE)` / `NO_STRIKE`.
   - `rotation` (degrees, −90 to 90 typical) → `a:bodyPr/@rot` where units are 60000 per degree.
   - `auto_fit: "none" | "normal" | "shrink"` → `XSLFTextShape.setTextAutofit(TextAutofit.NONE | NORMAL | SHRINK)`.

5. **Fix `clone_shape` PARTIAL → FULL**: generalize to any `XSLFShape` by cloning the underlying `XmlObject` (deep XML copy), regenerating shape id, applying offset to anchor. Remove the text-shape-only restriction at `PptAdvancedMutationOperations.java:940-941`.

6. **Feature flag flips**: `gradients`, `picture_effects`, `table_borders`, `table_merge` all → `true`.

7. **Unit tests** per capability. Edge cases: gradient with 1 stop (reject), crop values outside [0,1] (reject), overlapping merges (reject), `auto_fit` on a non-text shape (reject), cloning a picture / table / group shape (expect success).

8. **Docker smoke** exercises each new path.

**Files touched**: `tooling/operations/PptAdvancedMutationOperations.java` (main surface), `tooling/PptToolDefinitions.java`, `tooling/PptToolService.java`.

**Acceptance**:
- PPTX written with new effects opens in desktop PowerPoint showing effects correctly (visual check by the executor; no automated PowerPoint test).
- Coverage gate still ≥80%.

---

# Phase 4 — Charts (data update only)

**Goal**: agents can re-data existing charts (authored in PowerPoint or shipped in templates).

**Scope**:

1. **`ppt.list_charts`** (new):
   - Args: `document_id`, optional `slide_index`.
   - Returns: per-chart `{slide_index, shape_index, chart_type: "bar"|"column"|"line"|"pie"|"scatter"|"area"|"unknown", series: [{name, category_count, value_count}], categories: string[], has_embedded_workbook: boolean}`.

2. **`ppt.update_chart_data`** (new):
   - Args: `document_id, slide_index, shape_index, series: [{name?: string, values: number[]}], categories?: string[]`.
   - Behavior: locate `XSLFChart` via the shape. Update chart XML (`c:ser/c:val/c:numRef/c:numCache` numeric cache and `c:cat/c:strRef/c:strCache` category cache). Write matching values into the embedded XLSX workbook (open via `chart.getWorkbook()` / load `XSSFWorkbook` from the chart package relationship) so PowerPoint's "Edit Data" button continues to work.
   - Structured errors: `SHAPE_NOT_CHART`, `SERIES_COUNT_MISMATCH` (when the number of series provided ≠ chart series count), `EMBEDDED_WORKBOOK_MISSING` (older file format), `CATEGORY_COUNT_MISMATCH`. All `retriable=false`.
   - Returns: `{updated_series: n, categories_updated: bool}`.

3. **Test fixture**: add `src/test/resources/fixtures/chart-sample.pptx` — a one-slide deck with a clustered column chart, 3 series × 4 categories. (Create manually in PowerPoint/LibreOffice; commit the binary.)

4. **Unit tests**: `list_charts` on fixture; `update_chart_data` happy path; round-trip assertion on both the `c:numCache` cache nodes and embedded workbook cell values; all error paths.

5. **Docker smoke**: open fixture, update chart, high-fidelity render, assert PNG differs from a baseline PNG rendered before the update (simple byte-length diff or pixel sampling in the chart region — whichever is most reliable).

6. **Feature flag flip**: `charts_update = true`.

**Files touched**: new `tooling/operations/PptChartOperations.java`, `tooling/PptToolDefinitions.java`, `tooling/PptToolService.java`, new `src/test/resources/fixtures/chart-sample.pptx`, new unit tests.

**Explicitly out of scope (future v2)**: chart creation from scratch, new chart types, 3D charts, chart formatting (colors / legends / axes).

**Acceptance**:
- Fixture opens in PowerPoint after update; "Edit Data" workflow shows new values.
- Smoke shows render change after update.
- Coverage gate still ≥80%.

---

# Phase 5 — Ironclad Docker smoke + v1 release

**Goal**: every tool exercised end-to-end in a Docker smoke; final README; v1 declared done.

**Scope**:

1. **Ironclad Docker smoke**: split `DockerPptMcpServerSmokeTest` into `@Nested` classes per family (`Document`, `Slides`, `Text`, `Tables`, `Shapes`, `Images`, `Render`, `Export`, `Charts`, `Templates`, `Transactions`, `Capabilities`). Each nested class:
   - Shares the container (start once per outer class).
   - Invokes every tool in its family with representative args.
   - Asserts `status=success` and a family-specific invariant (e.g., slide count, non-blank rendered PNG, exported file size > 0, table cell text round-trip).
   - Localizes failure to the smallest possible family.

2. **Coverage gate**: raise jacoco threshold to **85%** on `tooling.*`.

3. **README rewrite**: regenerate tool catalog table against the final 42-tool API; add `capabilities` reference; add feature-flags appendix documenting what each flag gates; remove all drift notes carried over from earlier phases; ensure every example JSON matches the new schemas.

4. **`CHANGELOG.md`**: create with v1 entry summarizing all phases' contributions.

**Files touched**: `src/test/java/ai/skaile/mcpo/ppt/DockerPptMcpServerSmokeTest.java`, `pom.xml` (jacoco threshold), `README.md`, new `CHANGELOG.md`. **No changes to `.github/`.**

**Acceptance**:
- `./mvnw verify` passes locally including jacoco 85% gate.
- `docker build -t ppt-mcp:dev .` succeeds.
- Docker smoke exercises every tool family end-to-end and passes.
- README matches actual tool surface exactly.

---

## Phase dependencies

```
Phase 0 ── prerequisite for all others (locking, sandbox, fonts, soffice probe, test infra)
Phase 1 ── prerequisite for 2, 3, 4 (renames the surface they extend)
Phase 2 ── independent of 3
Phase 3 ── independent of 2
Phase 4 ── depends on 2 (high-fidelity render is the chart-update assertion)
Phase 5 ── last
```

Sequential execution order: **0 → 1 → 2 → 3 → 4 → 5**. One PR per phase, merged to `main` before starting the next.
