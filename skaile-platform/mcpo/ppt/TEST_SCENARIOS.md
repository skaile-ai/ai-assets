# ppt-mcp Manual Test Scenarios

End-to-end scenarios that cover every tool registered by
`src/main/java/ai/skaile/mcpo/ppt/tooling/PptToolDefinitions.java` (52 tools).
Each section exercises one tool family; each tool appears at least once.

These scenarios complement the JUnit suite — they drive the server the way an
MCP client would against the Docker image with `/workspace/resources`
bind-mounted.

Conventions:
- Paths use `/workspace/resources/...` (the Docker sandbox root).
- Every scenario starts from a fresh `initialize` handshake; scenarios do not
  share document handles.
- "Verify" lines are what to inspect after the call returns — envelope fields,
  on-disk artifacts, or a follow-up read tool.
- Every envelope carries `status`, `code`, `error`, `retriable`,
  `correlation_id`, `tool_name`. Missing fields are a regression.

---

## 1. Capabilities & discovery

### 1.1 `ppt.capabilities`
- Call with `{}`.
- Verify: `soffice_available`, `allowed_root`, `java_version`, supported
  `export_formats`, `render_formats`, feature flags, and `limits`
  (`MAX_SLIDES`, `MAX_SHAPES_PER_SLIDE`, `MAX_IMAGE_BYTES`,
  `MAX_RENDER_DIMENSION`) are populated.

---

## 2. Document lifecycle

### 2.1 `ppt.create_document` → `ppt.add_slide` → `ppt.save_path` round-trip
- `ppt.create_document` (no args) → capture `document_id` and
  `template_path`.
- `ppt.add_slide` with `title: "Hello"`.
- `ppt.get_document_info` → expect `slide_count`, `dirty=true`,
  `page_width/page_height`.
- `ppt.export_document` with `format: "pptx"` to
  `/workspace/resources/scen2-doc.pptx`.
- `ppt.close_document`.
- Verify: file exists, opens in PowerPoint, contains "Hello".

### 2.2 `ppt.open_document` → `ppt.set_document_metadata` → export
- `ppt.open_document` on any deck in `resources/`.
- `ppt.set_document_metadata` setting `title`, `author`, `subject`,
  `keywords`.
- `ppt.export_document` `format: "pptx"` to a new path.
- Verify: source file unchanged; new file's `docProps/core.xml` reflects all
  four metadata fields.

### 2.3 `ppt.export_document` — every supported format
From one open deck, export each format in turn:
- `pptx` → file writes via POI (no soffice).
- `pdf`, `html` → writes via soffice; if soffice unavailable expect
  `SOFFICE_UNAVAILABLE`, never a 500 or soffice exit-77 leak.
- `png_batch`, `jpg_batch`, `svg_batch` → `output_path` is a directory.
- `outline_text` → deterministic POI traversal; placeholder prompts
  (`Click to (edit|add) …`) are filtered per `PptDocumentOperations`.
- Verify: each artifact exists and opens in the matching viewer. `outline_text`
  contains no placeholder prompts.

### 2.4 `ppt.merge_presentations`
- Create deck A (2 slides) and deck B (3 slides, saved to disk).
- `ppt.merge_presentations` on A with `merge_path` = B and no
  `insert_at_index` (appends at end).
- Verify: A now has 5 slides in expected order; backgrounds of B's slides are
  preserved.

### 2.5 `ppt.generate_presentation`
- Call with `title`, `slide_titles: [...]`, and `output_path`.
- Verify: returns `document_id` + on-disk file with one slide per title.

### 2.6 `ppt.list_slides`, `ppt.reorder_slides`, `ppt.duplicate_slide`,
`ppt.delete_slides`
- Create a deck, `ppt.add_slide` ×3 with distinct titles.
- `ppt.duplicate_slide` on index 1 (default `target_index`).
- `ppt.reorder_slides` to reverse.
- `ppt.delete_slides` on `[0, 2]` with `keep_at_least_one: true`.
- `ppt.list_slides` → verify final count, titles, and order match the expected
  permutation.

### 2.7 `ppt.close_document` cleans up
- Open a doc, note the `document_id`.
- `ppt.close_document`.
- Any subsequent call with that `document_id` → `DOCUMENT_NOT_FOUND`.

---

## 3. Slide body tools

### 3.1 `ppt.add_textbox`
- Add a textbox at a specific position with `font_size`.
- Verify via `ppt.get_slide_content` that the new shape appears with the given
  text.

### 3.2 `ppt.get_slide_content` structure
- Add a slide with a title, a textbox, an image, and a table.
- `ppt.get_slide_content` → verify `text` string and a `shapes[]` array where
  each entry has `shape_index`, `shape_type`; tables also report `rows`/
  `cols`; text shapes include `text`.

### 3.3 `ppt.update_text` and `ppt.replace_text_globally`
- `ppt.update_text` on a specific slide replaces an occurrence on that slide
  only.
- `ppt.replace_text_globally` replaces across every slide; `max_replacements`
  caps the total; `case_sensitive=false` by default.

### 3.4 `ppt.find_text`
- Search for a known string.
- Verify each match has `slide_index`, `shape_index`, `start`, `end`, `text`;
  `count` matches array length.

### 3.5 `ppt.get_slide_notes` / `ppt.set_slide_notes`
- `ppt.get_slide_notes` on a fresh slide → `""` (no error).
- `ppt.set_slide_notes` on the SAME fresh slide (no notes section yet) →
  server auto-creates the notes part and persists the text.
- `ppt.get_slide_notes` again → round-trips the text.

---

## 4. Text mutation — `ppt.set_text`

One unified text tool. Exercise each `scope`:

### 4.1 `scope: "shape"` (default) — run-style fields + shape-body fields
- Apply `bold`, `italic`, `underline`, `strikethrough`, `font_size`,
  `font_color`, `font_family` to every run in a text shape.
- Apply `rotation` and `auto_fit` (`none` / `normal` / `shrink`) to the
  shape body.
- Apply paragraph-style fields (`text_align`, `line_spacing`, `space_before`,
  `space_after`, `left_margin`, `indent`, `bullet_enabled`, `numbered`,
  `bullet_character`, `bullet_level`) to every paragraph.
- Verify: survives round-trip via `ppt.export_document` + reopen.

### 4.2 `scope: "run"` — one matched segment
- Pass `target_text`, `occurrence`, `case_sensitive`.
- Verify: only the targeted run changes; other runs untouched.

### 4.3 `scope: "paragraph"` — one paragraph by `paragraph_index`
- Apply `text_align: "center"` to paragraph 0 of a multi-paragraph shape.
- Verify: only paragraph 0 changes alignment.

### 4.4 Invalid color
- Pass `font_color: "not-a-color"`.
- Expect `INVALID_COLOR` with a message naming the `#RRGGBB` format.

---

## 5. Shape tools

### 5.1 `ppt.add_shape`
- Add `rectangle`, `ellipse`, `line`, `arrow` with fill/border/text.
- Verify via `ppt.get_shape_properties` that type, anchor, fill, border match.

### 5.2 `ppt.move_shape`, `ppt.resize_shape`, `ppt.get_shape_properties`
- Move, then resize, then read back — x/y/width/height match.

### 5.3 `ppt.set_shape_style` — every `fill_type`
- `solid` via `fill_color`.
- `gradient` with ≥ 2 stops (linear, angle).
- `pattern` (one of the `preset` enum values) with `fg_color`/`bg_color`.
- `none`.
- Also set `border_color`, `border_width`, `text_align`.
- Verify: gradient XML is `<a:gradFill>`; pattern XML is `<a:pattFill>`
  authored via `XmlCursor` (pattern fills are not available via the typed
  `poi-ooxml-lite` API).

### 5.4 `ppt.clone_shape`
- Clone a styled shape.
- Verify: clone appears on the slide with an offset, has an independent shape
  id (`cNvPr/@id`), and the original is untouched. Exercises
  `invalidateShapeCache` — regression would show the clone missing from
  `list_slides` until reopen.

### 5.5 `ppt.set_shape_z_order`
- Call with `front`, `back`, `forward`, `backward`.
- After EACH call, `ppt.get_slide_content` must still return the current
  shape list — cache invalidation must run for every DOM mutation path so
  subsequent `ppt.delete_shape` and `ppt.get_slide_content` work consistently.

### 5.6 `ppt.delete_shape`
- After `set_shape_z_order` ops above, delete a shape by its CURRENT index.
- `ppt.get_slide_content` and `ppt.list_slides` must continue to work.

### 5.7 `ppt.add_hyperlink`
- On an `XSLFTextBox`: hyperlink applies to every run.
- On an `XSLFAutoShape` with text (e.g. rectangle with `text` arg): must not
  NPE — either apply the hyperlink or return a clear error.

---

## 6. Picture tools

### 6.1 `ppt.insert_image`, `ppt.replace_image`
- Insert an image under the `MAX_IMAGE_BYTES` limit.
- Replace it with a different image; with `keep_size: true` the anchor is
  preserved.

### 6.2 `ppt.set_picture_effects`
- Apply `crop` (20 % off each edge, verify `left + right < 1`).
- Apply `alpha: 0.5`.
- Apply each `recolor.mode`: `grayscale`, `sepia`, `duotone` (with
  `color`), `washout`.
- Verify: each effect serializes into `<a:blipFill>` and round-trips via
  `ppt.export_document`.

### 6.3 `MAX_IMAGE_BYTES` enforcement
- Attempt to insert an image above `MAX_IMAGE_BYTES` (50 MB by default).
- Expect `LIMIT_MAX_IMAGE_BYTES` error.

---

## 7. Table tools — `ppt.add_table`, `ppt.get_table`, `ppt.edit_table`

### 7.1 Add + populate
- `ppt.add_table` 3 × 4 at a fixed position.
- `ppt.edit_table` `operation: "set_cell"` for several cells.
- `ppt.get_table` → verify structure returned: `rows`, `cols`, and a
  `cells[]` array with text.

### 7.2 `ppt.edit_table` — every `operation`
For each operation below, call once and verify with `ppt.get_table`:
- `set_cell` — text round-trips.
- `insert_row`, `delete_row` — row count changes; existing cells shift.
- `insert_col`, `delete_col` — col count changes; existing cells shift.
- `set_row_height`, `set_col_width`.
- `set_header_style` — header row gets `fill_color`/`font_color`/`bold`.
- `merge_cells` on a 2 × 2 block. A second overlapping merge must return
  `MERGE_CONFLICT` and leave the table untouched (pre-merge scan runs
  first).
- `set_cell_border` — apply `color`, `sides: ["top","bottom","left","right"]`,
  `dash_style`.

---

## 8. Page setup, background, layout

- `ppt.set_page_setup` with `preset: "standard_4_3"` → `page_width=720`,
  `page_height=540`.
- `ppt.set_page_setup` with `preset: "widescreen_16_9"` → `960 × 540`.
- `ppt.set_page_setup` with `preset: "custom"` + `width`/`height`.
- `ppt.set_slide_background` with a valid hex color; invalid color →
  `INVALID_COLOR`.
- `ppt.set_slide_layout` with each `layout_type`: `blank`, `title`,
  `title_content`, `title_only`. Verify placeholders adapt.

---

## 9. Rendering — `ppt.render_slide`, `ppt.render_all_slides`

### 9.1 `fidelity: "low"` (POI + Batik)
- `ppt.render_slide` for each `format`: `png`, `jpg`, `svg`.
- `ppt.render_all_slides` on a 3-slide deck → N distinct files in
  `output_dir`; each represents the correct slide (not all slide 1).
- `file_name_pattern` is honored; missing extension is auto-appended.

### 9.2 `fidelity: "high"` (soffice)
- `ppt.render_slide` `fidelity: "high"` with `format: "png"`.
- If soffice unavailable: `SOFFICE_UNAVAILABLE`, no exit-77 leak.

### 9.3 `MAX_RENDER_DIMENSION`
- Request `width: 20000, height: 20000`.
- Expect `LIMIT_MAX_RENDER_DIMENSION`.

### 9.4 `ppt.get_slide_metrics`
- Verify returned fields: `shape_count`, `text_shape_count`, `image_count`,
  `table_count`, `table_cells`, `text_chars`, `word_count`,
  `avg_word_length`, `complexity_score`.

---

## 10. Templates & markdown import

### 10.1 `ppt.upload_template`, `ppt.set_default_template`, `ppt.get_default_template`
- `ppt.upload_template` with `make_default: false`.
- `ppt.set_default_template` with the uploaded path.
- `ppt.get_default_template` returns that path.
- On server restart, `ppt.get_default_template` still returns it (persisted
  via `MCPO_DEFAULT_TEMPLATE_CONFIG`).

### 10.2 `ppt.create_document` / `ppt.generate_presentation` honor default template
- With a default template set, calling either tool without `template_path`
  applies that template's masters/layouts.

### 10.3 `ppt.import_markdown_outline`
- With N top-level `#` headings in the markdown, the result has exactly N
  slides — the template's masters/layouts are kept but any pre-existing
  template content slides are NOT prepended.
- `- ` / `* ` lines become bullets under the preceding `#`.
- Plain lines become paragraph body text on the current slide.
- `output_path` (optional) writes the file and sets `source_path` + clears
  `dirty`.

---

## 11. Transactions

### 11.1 Commit path
- `ppt.transaction_begin` on an open doc.
- Make 3 mutations (add slide, edit text, resize shape).
- `ppt.transaction_commit`.
- Verify: all 3 persist; subsequent `transaction_rollback` returns an error
  (no snapshot).

### 11.2 Rollback path
- `ppt.transaction_begin`.
- Mutate (delete slides, add shapes).
- `ppt.transaction_rollback`.
- Verify: deck is restored to pre-begin state; `dirty` and `source_path`
  reflect the snapshot values.

### 11.3 Cross-document isolation
- Two docs A and B. `transaction_begin` on A.
- Mutate B. `transaction_rollback` on A.
- Verify: B's mutations are NOT rolled back.

### 11.4 Single-level semantics
- `ppt.transaction_begin` twice without an intervening commit.
- Second call succeeds and REPLACES the earlier snapshot — this is the
  documented single-level contract (`PptTransactionManager`).

---

## 12. Error & edge cases

Every failure mode must return a structured envelope, not a stack trace:

| Condition | Expected `code` |
|---|---|
| Unknown `document_id` | `DOCUMENT_NOT_FOUND` |
| Out-of-range `slide_index` | `SLIDE_INDEX_OUT_OF_RANGE` |
| Out-of-range `shape_index` | `SHAPE_INDEX_OUT_OF_RANGE` |
| Malformed `#RRGGBB` | `INVALID_COLOR` |
| Path outside `MCPO_ALLOWED_ROOT` | `PATH_NOT_ALLOWED` |
| `MAX_SLIDES` via `add_slide` loop | `LIMIT_MAX_SLIDES` |
| `MAX_SHAPES_PER_SLIDE` | `LIMIT_MAX_SHAPES` |
| `MAX_IMAGE_BYTES` | `LIMIT_MAX_IMAGE_BYTES` |
| `MAX_RENDER_DIMENSION` | `LIMIT_MAX_RENDER_DIMENSION` |
| `MCPO_MAX_OPEN_DOCS` reached | `LIMIT_MAX_OPEN_DOCS` |
| `soffice` absent on a format that needs it | `SOFFICE_UNAVAILABLE` |
| `update_chart_data` on non-chart shape | `SHAPE_NOT_CHART` |
| `update_chart_data` on chart w/o workbook | `EMBEDDED_WORKBOOK_MISSING` |
| `set_picture_effects` on non-picture shape | `SHAPE_NOT_PICTURE` |
| Malformed JSON-RPC frame (raw stdio) | server survives; parse error; next frame still served |

---

## 13. Charts — `ppt.list_charts`, `ppt.update_chart_data`

### 13.1 Inspect
- Open a deck with a PowerPoint-authored bar chart (data reference-backed in
  the embedded XLSX).
- `ppt.list_charts` → each entry has `slide_index`, `shape_index`,
  `chart_type`, per-series `name` and point count, `categories[]`, and
  `has_embedded_workbook`.
- `ppt.list_charts` with a `slide_index` filter → only charts on that slide.

### 13.2 Update data
- `ppt.update_chart_data` with new numeric `series[].values` (plus optional
  `name`) and new `categories`.
- Verify: (a) slide's numCache reflects the new values; (b) PowerPoint's
  "Edit Data" dialog shows them (the embedded XLSX was updated, not just
  the cache).

### 13.3 Error paths
- `update_chart_data` on a non-chart shape → `SHAPE_NOT_CHART`.
- Series count mismatch → `SERIES_COUNT_MISMATCH` (README + tests).
- Values or categories array length mismatch → `CATEGORY_COUNT_MISMATCH`.

---

## 14. Concurrency & stdio discipline

- Two simultaneous `ppt.render_slide` calls with `fidelity: "high"` against
  the same doc.
- Expect: serialized execution (semaphore in `SofficeRenderer`), both succeed,
  no stale temp files under `MCPO_ALLOWED_ROOT/.soffice-tmp*`.
- Inspect the container's stdout — MCP frames only; all logs go to stderr.
  A stray `println` in any tool handler would corrupt the protocol stream.
