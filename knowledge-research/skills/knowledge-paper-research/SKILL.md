---
name: "knowledge-paper-research"
description: "Given a scientific paper (PDF or Markdown), run a full structured-knowledge extraction and web-research pipeline. Produces one metadata file next to the paper and a dedicated <stem>_research/ folder containing four thematic research files plus one per-author profile file (prefixed author_). Use whenever you need to deeply understand a paper, prepare a research briefing, or enrich a paper's content with external context.\n"
metadata:
  tags:
    - "research"
    - "paper"
    - "science"
    - "knowledge-extraction"
    - "web-research"
    - "authors"
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "contract:knowledge-research-contract"
  env_vars:
    SEARXNG_URL: "Optional. The base URL of your SearXNG instance (default: http://localhost:8080)."
---

# Knowledge Paper Research

Given a scientific paper as a PDF or Markdown file, this skill executes a
multi-phase pipeline that extracts structured knowledge, performs web research
across five dimensions, and writes enriched markdown files alongside the source.

## Output layout

```
episodes/<episode_name>/
├── 10_source/
│   ├── <stem>.pdf                          ← source (never modified)
│   ├── <stem>.md                           ← docling output or source md (never modified)
│   ├── <stem>_artifacts/                   ← docling images (never modified)
│   └── <stem>.meta.md                      ← Phase 2: bibliographic metadata
└── 20_research/                            ← all web-research output
    ├── <stem>_paper_description.md     ← Phase 4: internal analysis (methods & results)
    ├── <stem>_similar_work.md          ← Phase 4: external context/competitors
    ├── <stem>_background.md            ← Phase 4: theoretical foundations
    ├── <stem>_clinical_implications.md ← Phase 4: practical/medical transfer
    └── authors/                        ← Phase 5
        ├── author_<FirstLast>.md       ← one file per author
        └── …
```

---

## Prerequisites

| Tool | Install |
|---|---|
| `docling` | `uv tool install docling` or `pip install docling` |
| `uv` | https://docs.astral.sh/uv/getting-started/installation/ |
| SearXNG | Running instance; set `SEARXNG_URL` (default: `http://localhost:8080`) |

### Locate `SKILL_DIR`

`SKILL_DIR` is the directory containing this `SKILL.md` file.

Inside Antigravity / Oh My Pi:
```
skill://knowledge-paper-research/<path>
```
resolves any file within the skill. In plain shell, substitute the absolute
path to this directory for `$SKILL_DIR`.

---

## Phase 0 — Input Resolution

1. Determine whether input is `.pdf` or `.md`.
2. **If PDF**, convert with docling:
   ```bash
   docling \
     --to md \
     --image-export-mode referenced \
     --output <paper_dir> \
     <paper.pdf>
   ```
   This writes `<stem>.md` and `<stem>_artifacts/` next to the PDF.
   The `<stem>.md` becomes the working document for all subsequent phases.
3. **If already `.md`**: use as-is.
4. Set `PAPER_DIR` = directory containing the working markdown file (which should be `10_source/`).
5. Set `STEM` = filename stem without extension.
6. Set `RESEARCH_DIR` = `../20_research/` relative to `PAPER_DIR`.
7. Create output directories:
   ```bash
   mkdir -p "$RESEARCH_DIR/authors"
   ```

---

## Phase 1 — Structural Extraction

Run the bundled extraction script against the working markdown:

```bash
uv run $SKILL_DIR/scripts/extract_structure.py <paper.md>
```

The script prints a JSON object to stdout:

```json
{
  "title":      "<string | null>",
  "authors":    ["<name>", "…"],
  "abstract":   "<string | null>",
  "year":       "<string | null>",
  "doi":        "<string | null>",
  "venue":      "<string | null>",
  "sections":   [{"heading": "<string>", "text": "<string>"}],
  "references": ["<string>", "…"]
}
```

If parsing fails or a field is missing, fill what you can from the raw markdown
text — **do not abort the pipeline**.

Capture the JSON into a variable or file for use in subsequent phases:
```bash
EXTRACTED=$(uv run $SKILL_DIR/scripts/extract_structure.py "$PAPER_DIR/$STEM.md")
```

---

## Phase 2 — Write Meta File

Write `$PAPER_DIR/$STEM.meta.md` using the template in
[Output Schema § meta](references/output-schema.md).

**This file lives next to the source paper, not inside `$RESEARCH_DIR`.**

Fields:
- `# <title>` as the H1
- Blockquote: `> Source: <venue>, <year> | DOI: <doi>`
- `## Authors` — comma-separated, each with affiliation in parentheses
- `## Abstract` — verbatim from paper
- `## Keywords` — from paper keywords section
- `## Publication Info` — venue, year, DOI, received date, open-access status

---

## Phase 3 — Web Research

Run searches across all five dimensions **in parallel** using the bundled script:

```bash
uv run $SKILL_DIR/scripts/search.py "<query>" \
  [--categories <cat>] [--time_range <range>] [--results <n>]
```

The script reads `SEARXNG_URL` from the environment (default `http://localhost:8080`).
If SearXNG is unavailable, substitute any web-search tool available to the agent —
the query strings in [Search Strategies](references/search-strategies.md) are
platform-agnostic.

### Research dimensions

| Dimension | Target file | What to research |
|---|---|---|
| Paper Analysis | `$RESEARCH_DIR/${STEM}_paper_description.md` | Detailed prose on methods, study design, and internal results |
| External Context | `$RESEARCH_DIR/${STEM}_similar_work.md` | Similar experiments, comparable studies, and replications |
| Theoretical Foundations | `$RESEARCH_DIR/${STEM}_background.md` | Core theories, models, and scientific grounding |
| Clinical Utility | `$RESEARCH_DIR/${STEM}_clinical_implications.md` | Transfer to medical/psychiatric practice |
| Authors | `$RESEARCH_DIR/authors/author_<Name>.md` | One file per author: profile, publications, metrics |

See [Search Strategies](references/search-strategies.md) for exact query templates
per dimension.

**Collect all search results before writing any files.**

---

## Phase 4 — Write Thematic Research Files

Write the four thematic files inside `$RESEARCH_DIR/`.
Follow the templates in [Output Schema](references/output-schema.md) exactly.

Each thematic file **must**:
- Open with `> Source: <paper title> (<year>)` blockquote
- Contain `## From the Paper` — verbatim or close paraphrase from extracted text
- Contain `## Web Research` — synthesised from search results, URLs cited inline as `[Title](URL)`
- Close with `## Gaps & Open Questions` — what is unclear or not covered

When a source URL is unknown, write `(URL unavailable)` rather than omitting
the citation entirely.

---

## Phase 5 — Write Per-Author Files

For every author listed in the paper, create one file at:

```
$RESEARCH_DIR/authors/author_<AuthorName>.md
```

**Filename rules:**
- Prefix with `author_`
- Replace spaces with underscores
- Use the author's full name as listed in the paper
- Example: `author_Nadia_Micali.md`

Each author file **must**:
- Open with `> Source: <paper title> (<year>)` blockquote
- Contain `## From the Paper` with affiliation, email/contact, and role in study
  as stated in the paper
- Contain `## Web Research` with these subsections:
  - `### Current Affiliation` — institution and position, with source URL
  - `### Research Focus` — 1–2 sentences on their domain and methods
  - `### Notable Publications` — up to 5 key prior works with DOI/URL
  - `### Citation Metrics` — h-index, total citations, Google Scholar URL, ORCID
  - `### Recent Activity` — grants, new positions, press (last 12 months); omit
    subsection entirely if nothing found
  - `### Sources` — URLs used

Author files do **not** contain a `## Gaps & Open Questions` section.

If a specific fact cannot be verified from available sources, state
`not found` explicitly — never invent metrics or publications.

---

## Phase 6 — Auto-Improvement

After all files are written:

1. Assess: were any searches unsuccessful? Any sections thin, empty, or relying
   on invented content?
2. Note concrete improvement ideas (missing queries, schema gaps, parsing issues).
3. Ask the user: *"Should I save improvement notes for this skill?"*
4. If yes, append to `$SKILL_DIR/resources/improvement_ideas.md`.

---

## Output Summary

Print a completion table:

```
| File | Status | Notes |
|---|---|---|
| <stem>.meta.md | ✓ written | next to paper |
| <stem>_research/<stem>_paper_description.md | ✓ written | internal analysis |
| <stem>_research/<stem>_similar_work.md | ✓ written | external context |
| <stem>_research/<stem>_background.md | ✓ written | theoretical foundations |
| <stem>_research/<stem>_clinical_implications.md | ✓ written | clinical transfer |
| <stem>_research/authors/author_<Name1>.md | ✓ written | … |
| <stem>_research/authors/author_<Name2>.md | ✓ written | … |
…
```

---

## Constraints

- **Never overwrite** the source `.pdf` or `.md`.
- `<stem>.meta.md` is co-located with the source paper in `10_source/`; all other output lives
  inside `20_research/`.
- **Never invent** citations, metrics, or results not found in the paper or web
  searches. Use `not found` / `(URL unavailable)` as explicit markers.
- **Author files have no Gaps section.**
- Cite all web sources inline as `[Title](URL)`.

---

## References

- [Output Schema](references/output-schema.md) — Exact file templates
- [Search Strategies](references/search-strategies.md) — Query templates per dimension
- [Example run](examples/example.md) — Annotated walkthrough with the Viggers et al. 2026 paper
