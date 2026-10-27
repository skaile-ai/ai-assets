---
name: ppt
description: "Use when an agent needs to author, modify, review, render, or export
  PowerPoint (.pptx / .pptm) decks — create/open/close documents, add/duplicate/delete/reorder
  slides, edit text (with run/paragraph/shape scope + styling), insert images with
  picture effects, add/edit tables (incl. merge + per-cell borders), draw shapes (solid/gradient/pattern
  fills), update chart data in existing charts, render slides to PNG/JPG/SVG at low
  or high fidelity, and export to PPTX/PDF/HTML/PNG-batch/JPG-batch/SVG-batch/outline-text.
  Stateful Apache POI 5.5.x + LibreOffice engine over stdio, 52 tools."
version: 1.2.1 # mcp-catalog-version
transport: stdio
# Start the JVM on the first tool call, not at session start.
lazy: true
recipe:
  attr: mcps.ppt
command: ${recipe:ppt:bin}/java
args:
  - -jar
  - ${recipe:ppt:lib}/ppt-mcp.jar
env:
  MCPO_ALLOWED_ROOT: /skaile/workspace
  # Must sit under MCPO_ALLOWED_ROOT: unset, the server defaults it to
  # $HOME/.mcpo-ppt/templates, outside the root, and exits at startup.
  # Keep the prefix in sync with MCPO_ALLOWED_ROOT: a mismatch is a startup
  # crash, not a fallback.
  MCPO_TEMPLATE_DIR: /skaile/workspace/.mcpo-ppt/templates
  SOFFICE_PATH: ${recipe:ppt:bin}/soffice
  JAVA_HOME: ${recipe:ppt}
keywords:
  - powerpoint
  - pptx
  - presentation
  - slides
  - libreoffice
  - mcp
  - poi
---

# PowerPoint MCP Server

Stateful PowerPoint engine over stdio: Apache POI 5.5.x for authoring, LibreOffice (`soffice`) for
PDF, HTML, image-batch export and high-fidelity renders. Source, build, run instructions, environment
variables and implementation docs: [`skaile-ai/powerpoint-mcp`](https://github.com/skaile-ai/powerpoint-mcp)
(the `powerpoint-mcp/` submodule). This directory is the catalog entry only; bump `version:` here when
adopting a release.

The agent guidance (when to use this server instead of a script, the session flow, the rules that
span tools, the design defaults) is sent by the server itself as its MCP `initialize` instructions, so
every client gets it and it always matches the running jar. Each tool's description carries that
tool's own arguments and limits. Keep this body short: the platform puts it in the prompt on every turn.

## Capabilities

52 tools over stdio, grouped by area as in the server's instructions:

<!-- mcp-catalog-tools -->
- **Documents (10)** — `ppt.create_document`, `ppt.open_document`, `ppt.generate_presentation`, `ppt.import_markdown_outline`, `ppt.merge_presentations`, `ppt.get_document_info`, `ppt.set_document_metadata`, `ppt.set_page_setup`, `ppt.export_document`, `ppt.close_document`
- **Slides (10)** — `ppt.list_slides`, `ppt.get_slide_content`, `ppt.add_slide`, `ppt.duplicate_slide`, `ppt.delete_slides`, `ppt.reorder_slides`, `ppt.set_slide_layout`, `ppt.set_slide_background`, `ppt.get_slide_notes`, `ppt.set_slide_notes`
- **Text (5)** — `ppt.set_text`, `ppt.add_textbox`, `ppt.update_text`, `ppt.replace_text_globally`, `ppt.find_text`
- **Shapes (9)** — `ppt.add_shape`, `ppt.get_shape_properties`, `ppt.move_shape`, `ppt.resize_shape`, `ppt.clone_shape`, `ppt.delete_shape`, `ppt.set_shape_style`, `ppt.set_shape_z_order`, `ppt.add_hyperlink`
- **Images (3)** — `ppt.insert_image`, `ppt.replace_image`, `ppt.set_picture_effects`
- **Tables (3)** — `ppt.add_table`, `ppt.get_table`, `ppt.edit_table`
- **Charts, update only (2)** — `ppt.list_charts`, `ppt.update_chart_data`
- **Review (3)** — `ppt.render_slide`, `ppt.render_all_slides`, `ppt.get_slide_metrics`
- **Templates (3)** — `ppt.upload_template`, `ppt.set_default_template`, `ppt.get_default_template`
- **Transactions (3)** — `ppt.transaction_begin`, `ppt.transaction_commit`, `ppt.transaction_rollback`
- **Capabilities (1)** — `ppt.capabilities`
<!-- /mcp-catalog-tools -->
