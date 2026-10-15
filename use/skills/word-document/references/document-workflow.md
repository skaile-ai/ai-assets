# Document workflow

This reference owns the generic procedure for creating and editing an
organization's Word documents. The Word MCP lifecycle, tool contracts and
limitations are materialized at `.skaile/assets/mcp-server/word/MCP.md`; their
source is the [Word MCP catalog entry](../../../../mcp/docx/MCP.md). Prose rules
live in [writing-rules.md](writing-rules.md).

When this reference and the installed Word MCP tool descriptions differ, follow
the installed tool descriptions.

## Handles and Block ids

- `document.create_from` and `document.create_blank` return a **detached
  handle**: it has no save target, so `document.save` needs an explicit path and
  a template can never be overwritten by a targetless save.
- `document.open` returns a handle bound to the opened file.
- Block ids come from `outline.get` (or `text.find`, `header.get`,
  `footer.get`) for one handle. Never assume fixed ids and never reuse a handle
  or Block id across documents.
- `document.close` discards unsaved edits. Save first if they matter.

## Choose the starting point

1. Call `template.find`. It searches for `.docx` files under `DOCX_MCP_ROOT`;
   `DOCX_MCP_TEMPLATE_GLOB` narrows the search only where the session sets it,
   so the result may be every document in the workspace rather than a curated
   template list.
2. Select the organization's template:
   - One clear candidate: use it.
   - Several candidates: prefer the one the user or the project names; if the
     choice is still ambiguous, ask the user rather than guess.
   - Many unrelated documents and no file that is recognizably a template: do
     not pick one. Ask the user which file is the template; if there is none,
     treat it as no template found.
   - None: fall back to `document.create_blank`.
   Never derive a new document from an ordinary draft or deliverable.
3. A blank document is a neutral baseline (A4, Arial defaults, baseline Named
   styles, single-level number and bullet definitions, bordered `TableGrid`). It
   has no branding, headers, footers, organization metadata, automatic heading
   numbering or multilevel lists. Tell the user so.

## Create a new document

1. Call `document.create_from` with the template, or `document.create_blank`.
2. Call `outline.get`. Locate the placeholder or anchor Blocks from the
   response.
   - Template: find its title placeholder and the paragraph where content
     begins.
   - Blank: the response names the initial paragraph (`p-1`). It is visible
     content: insert the first real Block after it, then remove it with
     `block.delete`.
3. Call `style.list` to learn the document's real Named styles. Use `style.get`
   when you need a style's resolved properties.
4. Replace template placeholder text with block-scoped `text.replace`. There is
   no whole-document replace. `text.replace` refuses a paragraph that contains
   a field. If the placeholder is a `DOCPROPERTY` field, change the property
   with `document.set_properties` (step 5) instead; otherwise leave the
   paragraph intact and report the placeholder as manual Word work. Never
   regenerate the paragraph or imitate the text with direct formatting.
5. Set `title` and `creator` (the person responsible) with
   `document.set_properties`, then call `document.mark_fields_dirty`.
6. Compose content with `block.insert` and a real `style_name`. Use each
   returned Block as the next anchor, or re-read the outline.
7. Continue a numbered or bulleted list with `copy_numbering_from`. Set
   `page_break_before` with `block.set_format` where a new page must start.
8. Prefer `style.apply` for an existing Block. Use `text.set_direct_format` only
   for a property no Named style provides.
9. Add fields with `field.insert` (supported `DOCPROPERTY` values, `PAGE`,
   `NUMPAGES`, formatted `DATE`/`SAVEDATE`/`CREATEDATE`). Preserve the
   template's existing header and footer fields.
10. Re-read the outline and verify order, styles and computed numbers (the
    `number` field from `outline.get`).
11. Save to an explicit new `.docx` path under `DOCX_MCP_ROOT`.
12. Render a PDF when asked, or before handing over a new document.

## Edit an existing document

1. Use `document.open`, never `document.create_from` or `document.create_blank`.
2. Read `outline.get` before body edits. Call `header.get` or `footer.get` when
   the request touches them; a shared header or footer Part can affect several
   Layout sections, and the mutation response says so.
3. Scope every change with a Block id from this handle. Preserve Named styles
   and every Part the request does not address.
4. If `title` or `creator` changes, update it with `document.set_properties`
   and call `document.mark_fields_dirty`.
5. Save, render when asked, and verify. Save to a new path when the original
   must stay unchanged.

## Fields: Word is the authority

`document.mark_fields_dirty` asks Word to refresh fields when the file is next
opened; it does not compute results. `field.insert` stores the cached text you
supply. LibreOffice rendering has tested refresh for `DOCPROPERTY Title`; other
property and date fields are converter-dependent, and cached TOC or index
entries stay stale. Final field refresh, pagination and pixel-level sign-off
belong to Microsoft Word.

## Tables

- `table.get` reads rows, cells and merges as structure.
- `table.edit` sets cell text, deletes a row, or inserts a row cloned from an
  existing one so its formatting is kept.
- `table.insert` has two modes:
  - Clone mode: `after_block_id` plus a real `clone_block_id` copies an existing
    table. Prefer it when the template already has a styled table.
  - Grid mode: `rows` and `columns` (at most 1,000 rows, 63 columns, 10,000
    cells) with an optional real table-style id. Omitting the style uses the
    document's declared default table style.
- Tables need a body anchor; they cannot be inserted in headers, footers or text
  boxes.
- `text.find` and `text.replace` do not reach table-cell text; use `table.edit`.

## Page setup

The Word MCP documents no tool for page size, margins, orientation or Layout
section breaks. Page setup therefore comes from the starting point:

- A document created from the organization's template keeps the template's page
  size, margins, orientation and sections.
- A blank document uses the neutral A4 baseline.
- `block.set_format` controls `page_break_before` (body only), `keep_with_next`
  and `keep_lines`.

If the user asks for a different paper size, margins, orientation or a new
Layout section, say the server cannot set it and list it as manual Word work.

## PDF export

Call `document.render` to produce a PDF of the live handle, including unsaved
edits. It does not save the document or change its save target. An explicit
target must end in `.pdf` and sit inside `DOCX_MCP_ROOT` or scratch in an
existing directory; otherwise the server returns a scratch path. The PDF is a
LibreOffice approximation: fonts, line breaks and pagination can differ from
Word.

## Disclose house-rule gaps

The server cannot author every property a style guide may require. Examples:

- Italics for foreign terms, unless the text already carries suitable
  formatting.
- Hyperlinks for web or intranet references that are not already links.
- Page size, margins, orientation and Layout section changes.

List each such item as remaining manual Word work rather than returning a
document that looks fully compliant.
