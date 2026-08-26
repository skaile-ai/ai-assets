---
name: word
description: "A stateful Word (.docx) engine for editing a real document in place instead of regenerating it. Its 37 tools expose identity-bound Blocks across body, headers, footers and text boxes; Named styles and resolved style audits; scoped text, paragraph and table composition; live PDF previews; core properties and Word fields; PNG/JPEG replacement and insertion; and complete Revision/Comment collaboration. document.save backs up, verifies the OOXML Part inventory and content, and atomically replaces the target. Use it when preserving a Template, numbering, review markup, media and package fidelity matter."
version: 0.5.0 # mcp-catalog-version
transport: stdio
recipe:
  attr: mcps.word
command: ${recipe:word:bin}/java
args:
  - -jar
  - ${recipe:word:lib}/word-mcp.jar
env:
  # DOCX_MCP_ROOT is the path-sandbox root; the server validates it exists at
  # startup and refuses to start unbound (ADR-0006, ADR-0002 in skaile-ai/word-mcp).
  # Unlike the excel-mcp sibling this is a real per-session path, not `/` — word-mcp's
  # own design explicitly rejects an "effectively open" default root.
  DOCX_MCP_ROOT: /skaile/workspace
  DOCX_MCP_SOFFICE_PATH: ${recipe:word:bin}/soffice
  JAVA_HOME: ${recipe:word}
keywords:
  - word
  - docx
  - document
  - ms-word
  - office
  - mcp
  - poi
---

# Word MCP Server

Docker/Nix-based MCP server for Word document operations, built on Apache POI XWPF 5.5.1.

> **Source code:** the server source, build (`pom.xml`, `flake.nix`, `Dockerfile`,
> `mvnw`), fixture-suite tests, ADRs and implementation docs live in their own repo,
> [`skaile-ai/word-mcp`](https://github.com/skaile-ai/word-mcp) (a submodule at the
> workspace root as `word-mcp/`). This directory is the **catalog entry only** —
> `MCP.md`. Versioning/PRs/issues happen in that repo; bump `version:` here when
> adopting a new release.

## When to reach for this

- The user asks to read, inspect, summarize, edit, or verify a Word document (`.docx`).
- The task is a **correction to an existing document** or composition from a real Template — a
  heading's text, one table's contents, a paragraph's style, a new Section or an inserted image.
  This server edits the OOXML package in place; regenerating the whole file is the data-loss
  pattern it exists to stop.
- The task needs a document to inherit an org Template's look (fonts, palette, header/footer,
  numbering) rather than have it re-typed as literal formatting.
- The task needs the document's real structure — its heading hierarchy with computed section
  numbers, or a table's actual row/column/merge shape — not text with the layout fused away.
- The task needs to compose a Template-derived deliverable by adding/removing paragraphs,
  Sections, table rows/tables, images, properties, or fields while preserving the house document.
- The task needs a reviewable workflow: authored or resolved Word Revisions, threaded Comments,
  field refresh-on-open, or a PDF preview of unsaved changes before the final save.

## When NOT to reach for this

- **A genuinely blank document with no source or Template to build on.**
  `document.create_from(<org Template>)` plus structural edits is the intended composition path;
  there is no blank-package generator by design.
- **The file isn't a `.docx`** (`.xlsx`, `.pptx`, `.pdf`, `.odt`, `.rtf`) — use the matching
  sibling MCP (`excel`, `ppt`) or `use-anydoc` / `use-docling` for read-only extraction of
  other formats.
- **OOXML Strict documents** (rare; usually produced by non-Microsoft tooling) can't be opened
  at all without the `allow_convert` fallback — see Limitations.
- **Quick prose reading with no edit intent** — `use-anydoc` is cheaper for "what does this
  document say" when nothing needs to change.
- **Pixel-perfect final visual approval or guaranteed TOC/index refresh.** `document.render` is a
  LibreOffice approximation; final Word pagination and field/index sign-off still happen in Word.

## Capabilities

37 tools over stdio, grouped by area:

- **Document lifecycle, properties and freshness (8)** — `document.open` (handle plus optional
  OOXML Strict conversion), `document.save` (backup → temp → Part/content verification → atomic
  rename, with an exact clean-handle no-op), `document.render` (validated PDF of the live unsaved
  handle), `document.create_from` (byte-copy a Template into scratch), `document.close` (discard
  unsaved state and free the slot), `document.get_properties`, `document.set_properties` (patch
  title/subject/creator/description/keywords/category only), and `document.mark_fields_dirty`
  (`w:updateFields` for Word refresh-on-open)
- **Outline and structural composition (4)** — `outline.get` returns every body/text-box Block with
  identity-bound id, kind, Named style, literal label and computed number; `block.insert` adds one
  Named-style paragraph after an explicit flow Block, optionally continuing numbering;
  `block.set_format` sets/clears `page_break_before`, `keep_with_next`, and `keep_lines`; and
  `block.delete` removes one Block or an explicit heading Section while reporting retired ids and
  last-reference media Parts. Paragraph operations reach body, header, footer and text-box flows;
  page-break-before remains body-only.
- **Text (3)** — `text.find` is literal by default and optionally bounded Java-regex pattern mode,
  returning visibly escaped context for exact follow-up placement; `text.replace` is literal and
  explicitly scoped to one Block or heading Section, never the whole document; and
  `text.set_direct_format` is the one bold/size/color/font escape hatch.
- **Named styles (3)** — `style.list`, `style.get` (effective properties resolved through the
  explicit `basedOn` chain without guessing Word defaults), and `style.apply`. Missing names fail
  `STYLE_NOT_FOUND` with the document's real inventory; styles are never invented or fuzzy-matched.
- **Tables (3)** — `table.get` exposes rows/cells/merges as structure; `table.edit` sets cell text,
  deletes rows, or inserts a formatting-preserving row clone; `table.insert` either clones a real
  exemplar without duplicating pictures/media or creates a bounded empty rows×columns grid using
  a real declared table Named style and no synthesized direct formatting.
- **Headers and footers (2)** — `header.get` / `footer.get` enumerate default/first/even variants
  as ordinary Blocks. Existing scoped text/style/format/block/field tools write those ids; responses
  disclose when several Layout-section references share the mutated Part.
- **Fields (1)** — `field.insert` appends or exact-context-replaces a placeholder with a validated
  `w:fldSimple`: supported core `DOCPROPERTY` values, `PAGE`, `NUMPAGES`, and bounded formatted
  `DATE`/`SAVEDATE`/`CREATEDATE`. The caller supplies the cached display text; the server never
  fabricates a result.
- **Media and images (3)** — `media.list` inventories Parts and references; `media.replace` swaps
  an existing PNG/JPEG payload while keeping Part identity, relationship, anchor and extent;
  `image.insert` byte-detects a root-contained PNG/JPEG and authors one aspect-locked inline image
  in a fresh body paragraph, with natural DPI sizing or an explicit width.
- **Revisions (5)** — `revision.list` reports ids, kinds, authors and timestamps in XML order;
  `revision.track(handle, author)` makes supported later agent writes real Word Revisions;
  `revision.enable` sets Word's Track Changes mode for future human edits; `revision.accept` and
  `revision.reject` resolve one exact id or an explicit `all:true` atomically.
- **Comments (4)** — `comment.list`, `comment.add` (whole Block or one exact literal anchor),
  `comment.reply`, and `comment.resolve`. Replies use Word threads, resolving a reply resolves its
  root thread, re-resolving is a no-op, and a later reply reopens it.
- **Templates (1)** — `template.find` discovers `.docx` files under the sandbox root via a neutral
  glob for `document.create_from`.

Highlights: Block ids bound to POI object identity (stable across an edit elsewhere in the
document, never written into the file); Number resolution across both literal-heading-text and
style-chain-numbered documents, `null` rather than a guess when a format is unsupported; a
save-time Part-loss guard that only a tool's own declared removals can satisfy — no
`allow_part_loss` escape hatch exists anywhere in the design.

## Limitations

- **No blank-package, Named-style, or numbering-definition authoring.** New deliverables start
  from a real `.docx` Template. An undefined style is always an error with the real inventory,
  never invented or fuzzy-matched; numbering can be continued from an existing reference but no
  new numbering definition is synthesized.
- **OOXML Strict documents cannot be opened directly** (POI bug #57699). `document.open` fails
  `OOXML_STRICT_UNSUPPORTED` unless `allow_convert: true` opens a LibreOffice-converted scratch
  copy. That handle has no save target and can never overwrite the Strict original.
- **Rendering is an approximation.** LibreOffice fonts, line breaks and pagination can differ
  from Word. Tested renders refresh `DOCPROPERTY Title`, but other property/date fields are
  converter-version dependent and cached TOC/INDEX entries stay stale. Use Word for final field,
  pagination and pixel-level sign-off.
- **Field caches are explicit, not computed.** `field.insert` stores `cached_text` verbatim and
  refuses arbitrary field instructions. `document.mark_fields_dirty` asks Word to refresh fields
  on open; it neither saves nor calculates results. Field insertion while `revision.track` is
  active is refused until a proven interoperable tracked-field shape exists.
- **Review safety is conservative.** A write that would rebuild unresolved Revisions, Comment
  anchors, fields, hyperlinks, drawings, content controls or other protected inline structures is
  refused rather than relocating markup. `revision.reject` atomically refuses legacy
  `numberingChange` because its display cache cannot reconstruct prior numbering properties;
  accepting it is supported.
- **Some structural operations remain body-only.** Both `table.insert` modes and `image.insert`
  require a body anchor; `page_break_before` is body-only. Header/footer/text-box paragraphs still
  support scoped text, Named style, keep properties, block composition/deletion and fields.
- **Text and media have narrow format boundaries.** `text.find`/`text.replace` do not address
  table-cell text (`table.edit` does), and ordinary `text.replace` refuses a matched paragraph
  containing a simple or complex field. Media mutation supports byte-detected PNG/JPEG only;
  `media.replace` must keep the existing Part's format family and displayed extent.
- **Handle registry is process-local, with no idle TTL.** A handle lives until `document.close`
  or process exit; beyond `DOCX_MCP_MAX_OPEN_HANDLES` (default 8), another open fails
  `HANDLE_LIMIT_REACHED` until a slot is freed.

## Runtime

Built and pinned by the platform Nix flake (`platform/nix/flake.nix`'s `mcps.word`
derivation, which sources `word-mcp`'s own self-contained `flake.nix`). At session start the
runner resolves `${recipe:word}` to the closure's `/nix/store` path — the closure bundles the
JRE and a `soffice` binary (LibreOffice) for OOXML Strict conversion and PDF previews. No
`docker build` step is required for platform-deployed sessions.

For local standalone testing without the platform: clone
[`skaile-ai/word-mcp`](https://github.com/skaile-ai/word-mcp), build the docker image there
(`docker build -t word-mcp:dev .`), and override `command`/`args` in `skaile.yaml`'s
`mcp_servers:` block.

## Override examples

Override command and workspace root in `skaile.yaml` for standalone use:

```yaml
dependencies:
  - mcp:word

mcp_servers:
  - id: word
    command: docker
    args: [run, --rm, -i, -v, "/projects:/workspace:rw", -e, DOCX_MCP_ROOT=/workspace, word-mcp:dev]
```

## Core flow

A typical session looks like:

```
document.open (path, or allow_convert:true for OOXML Strict)
  → outline.get                          # every Block: id, style, label, computed number
  → text.find / table.get / style.get    # locate and inspect exact targets
  → text.replace / style.apply /         # write, fenced to an explicit Block or Section
    block.* / table.* / image.insert
  → document.render                      # preview live unsaved state; optional but recommended
  → document.save                        # backup, temp, verify, atomic rename
  → document.close                       # optional — frees the handle before session end
```

Producing a fresh document from an org Template instead of editing an upload:

```
template.find                            # discover the org Template via DOCX_MCP_TEMPLATE_GLOB
  → document.create_from (source)        # byte-for-byte copy into scratch, no save target
  → outline.get / style.list / table.get # learn the Template's real reusable structures
  → document.set_properties / block.insert / table.insert / image.insert / field.insert
  → document.mark_fields_dirty           # ask Word to refresh authored fields on open
  → document.render                      # approximate visual check before persistence
  → document.save (path: <new destination>)  # required — a create_from handle has none
```

Authoring and completing a review loop are deliberately separate choices:

```
revision.track (author)                  # later supported agent writes become Revisions
  → text.replace / block.* / table.edit / image.insert
  → revision.list                        # inspect exact ids and kinds
  → revision.accept or revision.reject   # one id, or explicit all:true

comment.list
  → comment.add / comment.reply
  → comment.resolve                      # thread state; does not change document content
```

## Non-obvious gotchas the agent must respect

- **Regeneration is not a tool.** There is no "rewrite this document" call. A from-scratch
  document is always `document.create_from(<a real source, usually a Template>)` plus edits —
  never hand-assembled or produced by another library and handed to this server to "fix up."
- **A Number is computed, never text.** A heading whose visible number is `"13.2"` may carry
  that literal text in `label`, or carry bare text with the number resolved from
  `numbering.xml` via the style chain — `outline.get`'s `number` field is `null`, not a guess,
  when the format isn't one of the resolved set (`decimal`, `lowerLetter`, `upperLetter`,
  `lowerRoman`, `upperRoman`, bullets).
- **`text.replace` needs an explicit scope, always.** There is no whole-document replace; a
  `block_id` (from `outline.get` or `text.find`) is required, and `include_subtree` only
  widens to a heading's Section when `block_id` names a heading. Pattern matching is read-only:
  `text.find(pattern:true)` accepts bounded Java regex, but replacement remains literal.
- **Use `escaped_context` as an exact optimistic-lock token.** `text.find` visibly escapes
  non-ASCII whitespace. `field.insert` placeholder replacement requires the current returned value
  together with literal `find`; stale or ambiguous contexts fail without mutation.
- **Style application never invents or fuzzy-matches.** `style.apply` with an undefined
  `style_name` fails `STYLE_NOT_FOUND` and hands back the document's actual style inventory —
  retry with a real name from that list, don't guess a close one.
- **Direct formatting is one call, on purpose.** `text.set_direct_format` is the only place
  bold/size/color/font can be set directly; every other text operation goes through Named
  styles, so bypassing the house style is always a deliberate, visible choice.
- **A `create_from` (or Strict-converted) handle can't overwrite its source.** `document.save`
  against one without an explicit destination fails `SAVE_TARGET_MISSING` — it can never
  resolve to the Template or the Strict original.
- **`table.insert` has two distinct signatures.** Clone mode needs `after_block_id` plus a real
  `clone_block_id`; grid mode needs `rows`, `columns`, and optionally a real table-style id. Grid
  dimensions are capped at 1,000 rows, 63 columns and 10,000 cells. Omitted style means the
  Template's declared default table style, not an invented `TableNormal` fallback.
- **Container-aware does not mean every operation is container-neutral.** Paragraph text, styles,
  keep properties, insertion/deletion and fields reach headers, footers and text boxes. Table and
  image insertion plus `page_break_before` remain body-only. Text-box `AlternateContent` branches
  are validated and staged together so old/new Word renderers cannot see divergent structure.
- **A shared header/footer Part has a wider blast radius.** Several Layout sections can reference
  the same Part; mutation responses report the Part, reference count/types and shared status.
- **`revision.track` and `revision.enable` are not synonyms.** `revision.track(handle, author)`
  authors supported agent operations as Revisions during this handle. `revision.enable` only sets
  Word's Track Changes mode for future human edits. Existing review markup is never silently
  reassigned to the agent.
- **Revision resolution is explicit and atomic.** Select one exact `revision.list` id or
  `all:true`; duplicate ids resolve as one logical selection and missing-id legacy marks are
  reachable only through `all:true`. Rejecting `numberingChange` is refused because its cached
  display value is not a restorable property snapshot.
- **Comments are not Revisions.** `comment.add`/`reply`/`resolve` annotate or change thread state
  without changing content. A reply to a resolved thread reopens it; resolving any reply resolves
  the root thread.
- **A field's displayed cache is not a freshness guarantee.** `field.insert` stores the explicit
  cache; `document.mark_fields_dirty` requests a Word refresh on open. LibreOffice preview refresh
  varies by field and version, and does not rebuild cached TOC/INDEX entries.
- **Core-property writes are patches.** `document.set_properties` changes only supplied values;
  omitted/JSON-null fields remain untouched, a same-value patch is an exact save no-op, and custom
  properties plus created/modified timestamps are never changed. Empty string stores an empty
  value; removal back to an absent property is not supported.
- **`PART_LOSS` has no override.** An unexplained missing Part after a save is always a hard
  refusal; only a tool's own declared removal (e.g. `table.edit`'s `delete_row` dropping an
  image it owns) is accounted for. There is no flag to suppress this check.
- **Sandbox is fail-closed.** The server refuses to start without `DOCX_MCP_ROOT` unless
  `DOCX_MCP_ALLOW_UNSANDBOXED=true` is explicitly set — an opt-in with no default, logged
  loudly at startup when active, for local development only.
- **A picture in the body can be reported two ways, and both are correct.** `media.list`
  reports it under the referencing paragraph's Block id (`p-N`); `outline.get` also emits a
  separate drawing Block (`d-N`) for the same shape as the honesty marker for unreadable
  content. They're naming the same picture from two different Block-model angles, not
  disagreeing. `image.insert` returns the nested drawing id; deleting it leaves its newly-created
  containing paragraph as an empty `p-N` Block.
- **Media operations trust bytes, not extensions.** Replacement and insertion decode PNG/JPEG
  payloads inside `DOCX_MCP_ROOT`. `media.replace` preserves the drawing extent, so a different
  pixel aspect ratio succeeds with an explicit stretch warning; `image.insert` derives an
  aspect-locked extent from DPI or an optional width.
- **`document.render` previews the live handle without saving it.** It neither changes the save
  target nor arms/clears dirty state. Its PDF is structural/layout evidence, not a promise of Word
  pagination or field freshness. Explicit targets must be `.pdf`, inside root or scratch, with an
  already-existing parent directory; otherwise a fresh scratch path is returned.
- **`document.close` never saves.** Closing a handle with unsaved edits discards them — always
  stated in the tool's response, never silent. Call `document.save` first if the edits matter.
- **Backups land in `.docx-backups/` at the root of `DOCX_MCP_ROOT`**, not next to the file
  being saved — last 3 per file, timestamped, openable. Deliberate (ADR-0005: a backup that
  dies with the container can't serve the recovery case it exists for), but visible as
  workspace-root clutter in a synced workspace if not anticipated.
- **`document.open`'s returned path can differ from the input path.** Symlink-safe
  canonicalization (`Path#toRealPath()`), not a bug — expect it before reporting a path back to
  a user verbatim.

## Reference documents (in the [`word-mcp`](https://github.com/skaile-ai/word-mcp) repo)

- `README.md` — full tool inventory, env-var reference, error-code table, run instructions.
- `CONTEXT.md` — the domain glossary (Block, Section vs. Layout section, Named style vs.
  Direct formatting, Revision vs. Comment vs. Track-changes mode) the tool surface is named
  after.
- `docs/adr/0001`–`0006` — the six hard-to-reverse design decisions (engine choice, edit vs.
  regeneration, named styles vs. direct formatting, Block id identity, save protocol,
  deployment neutrality).
- `docs/testing.md` — what `mvn verify` gates and the fixture-suite convention.
- `_devlog/` — one dated entry per landed change, each linking its issue/PR.
