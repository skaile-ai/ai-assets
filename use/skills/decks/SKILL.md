---
name: decks
description: "Create, customize, batch-produce, or rebuild Skaile presentations as self-contained HTML and PDF with the installed @skaile/decks runtime. Use for pitch decks, consulting reports, reusable slide libraries, storyline-to-slide production, and edits to existing decks artifacts."
metadata:
  version: "0.2.0"
---

# Decks

Use the installed `@skaile/decks` runtime for Skaile presentations. This skill
routes to the authoring instructions shipped with that runtime; those files
own the schemas, supported compositions, design rules, and editing workflow.

## Locate and verify the runtime

Run:

```sh
command -v decks
decks paths --json
```

Read the returned `skill` file before authoring. It routes to the component
reference and worked examples. `packageRoot` identifies the runtime package;
`componentExample` and `componentApi` locate its example and public API. The
platform also exposes `SKAILE_DECKS_PACKAGE_ROOT`; its
`skills/decks/SKILL.md` is the same entry point. Use the paths from the installed
CLI instead of assuming a checkout or a project-local `node_modules`.

Verify that `packageRoot/package.json` names `@skaile/decks` at version `0.2.0`.
This wrapper targets that version. If the CLI, package, matching version, or
required browser capability is unavailable, report the missing platform
runtime and the failed command. Do not install an unpinned package with
`npx`, `bunx`, or a package manager, substitute another renderer, or silently
omit requested PDF/check outputs.

## Author, compose, and preserve

For new Skaile presentations and customized batches, follow the installed
component-deck instructions. Map the approved storyline into validated
`storyline.json`; this is structured component input, not arbitrary prose or
Markdown. Preserve factual claims, evidence, speaker notes, and stable IDs.
Use `skaile-studio` for the Skaile brand and `consulting-report` for its report
style when requested. Let the installed reference define component fields and
capacity limits; do not guess the schema or write freeform HTML/CSS.

Compose a new deck after writing valid input:

```sh
mkdir -p build
decks compose storyline.json -o build/deck.html --pack skaile-studio --check --pdf --json
```

`--pdf` includes the layout check and writes the outputs; `--check` without
`--pdf` validates without writing files. Omit both for plain HTML production.
Keep the generated `build/deck.saved.json` alongside the HTML and PDF. It
records selected layouts, edits, assets, and dependency locks. Rebuild an
existing approved artifact from that saved file:

```sh
decks rebuild build/deck.saved.json -o build/deck.html --check --pdf --json
```

Rebuilding preserves the saved choices and checks the installed renderer,
pack, and asset hashes. A lock mismatch requires the matching runtime or an
explicitly approved upgrade; do not clear locks or recompose to disguise it.
Read the installed editing API guidance when changing a saved deck. Content
that exceeds a composition's capacity requires a deliberate content/layout
decision, not clipping or shrinking text.

For batch production, give each customized storyline its own output directory
and saved artifact. Start from approved content and compositions, run the same
validation for every deck, and surface failures for review. Check the rendered
result visually when establishing or changing the design; automated checks
do not establish design quality or verify factual claims.

For an existing `deck.md`, follow the installed skill's Markdown route and
`decks render --help`. Do not convert formats merely to rebuild a deck.

Deliver links to the produced artifacts and report validation failures. The
HTML is self-contained at viewing time. PDF reproducibility requires the same
pinned browser/platform environment; consult the installed documentation for
the exact guarantees and CLI exit/reporting contracts.
