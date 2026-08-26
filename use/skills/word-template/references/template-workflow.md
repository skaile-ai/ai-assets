# Template mechanics and Word MCP workflow

This reference owns the bundled template's mechanics. The general Word MCP
lifecycle, tool contracts, and limitations are materialized at
`.skaile/assets/mcp-server/word/MCP.md`; their source is the
[Word MCP catalog entry](../../../../mcp/docx/MCP.md). The prose rules live in
[house-style-guide.md](house-style-guide.md).

When this reference and the installed Word MCP tool descriptions differ, follow
the installed tool descriptions while preserving the template invariants below.

## Template invariant

Create every new house document by copying
`../assets/Standard Document Template.docx` with `document.create_from`. The file
carries the Named styles, heading numbering, square bullets, logo headers, and
field-driven footers. A newly generated DOCX loses those package Parts.

## Initial structure

The body contains exactly two paragraphs:

1. A `Title` paragraph containing `Title of Document`.
2. An empty `BodyText` paragraph where content begins.

## Named styles

Use `style.list` for the full inventory and `style.get` for resolved properties.

| Style ID | Role | Resolved look |
|---|---|---|
| `Title` | Document title | Calibri Light 20 pt bold, `#2C4255` |
| `Subtitle` | Subtitle line | 11 pt, green `#A2C510` |
| `Heading1` | Level-1 heading | Calibri Light 16 pt bold, green; numbering is style-bound |
| `Heading2` / `Heading3` | Deeper headings | Same style chain; inspect with `style.get` |
| `BodyText` | Default body text | Verdana 11 pt, `#2C4255` |
| `ListBullet` | Square green bullets | Numbering is style-bound |
| `FusszeileBioCopyCH` | Footer information line | Footer-only; maintained through document properties |

Applying `Heading1` through `Heading3` or `ListBullet` supplies its number or
bullet automatically. Confirm computed numbering through the `number` field from
`outline.get`.

## Headers and protected space

The first-page header and the pages-2-and-later header contain a logo at the top
right, approximately 2.4 × 2.1 cm. Keep the space below it free. Preserve the
header Parts while changing body content.

## Footers and properties

- The first-page footer contains only the page number; its information line is
  intentionally empty.
- From page 2, the footer contains the page number and an information line built
  from `DOCPROPERTY Title`, saved date in `d MMMM yyyy` format, and `AUTHOR`.
- The even-page footer contains a page-number field and is inactive unless
  even/odd headers are enabled.

Set `title` and `creator` (the person responsible) with
`document.set_properties`, then call `document.mark_fields_dirty`. Preserve the
existing fields rather than replacing their cached text. Microsoft Word is the
authority for final field refresh. LibreOffice rendering has tested refresh for
`DOCPROPERTY Title`; other property and date fields are converter-dependent.

## Create a new document

1. Call `template.find` and select the exact bundled template named
   `Standard Document Template.docx`. Platform materialization may expose both
   the canonical asset and a driver-staged copy; prefer the path under
   `.skaile/assets/skill/word-template/assets/`. If that path is absent and
   several matches remain, verify that their SHA-256 hashes are identical before
   selecting one; stop if the file is absent or the candidates differ.
2. Call `document.create_from`. The returned detached handle has no save target,
   so the template cannot be overwritten by a targetless save.
3. Call `outline.get`. Locate the `Title` placeholder Block and empty `BodyText`
   Block from the response; do not assume fixed IDs.
4. Use block-scoped `text.replace` to replace `Title of Document`.
5. Set the same title and the responsible person as `creator` with
   `document.set_properties`, then call `document.mark_fields_dirty`.
6. Compose content after the empty `BodyText` Block with `block.insert` and a
   real `style_name`. Use each returned Block as the next anchor or re-read the
   outline.
7. Continue a list with `copy_numbering_from`. Use `page_break_before: true`
   where a new page intentionally starts.
8. Prefer `style.apply` for an existing Block. Use direct formatting only for a
   property no Named style provides.
9. Use the current Word MCP composition tools for tables, body images, or fields
   when the requested document needs them. Preserve the existing footer fields.
10. Re-read the outline and verify order, styles, and computed numbers.
11. Save to an explicit new `.docx` path under `DOCX_MCP_ROOT`.
12. Render a PDF, inspect it, and arrange final sign-off in Microsoft Word.

## Edit an existing document

1. Use `document.open`, not `document.create_from`.
2. Read `outline.get` before body edits and inspect headers or footers when the
   request touches them.
3. Scope every change with a block ID from that handle. Preserve Named styles and
   unaddressed package Parts.
4. If `title` or `creator` changes, update it through document properties and
   call `document.mark_fields_dirty`.
5. Save, render, and verify. Save to a new target when the original must remain
   unchanged.

## House-rule gaps

The current server cannot directly author every property required by the writing
guide:

- Foreign terms needing italics require manual Word completion unless the
  relevant text already has suitable formatting.
- Internet and SharePoint references that are not already hyperlinks require
  manual Word completion.

Disclose these items rather than silently returning a document that appears
fully compliant.
