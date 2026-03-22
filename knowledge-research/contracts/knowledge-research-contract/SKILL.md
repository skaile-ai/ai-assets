---
name: "knowledge-research-contract"
description: "Shared contract for all knowledge-research skills. Describes research output folder structure, findings format, citation conventions, and how research artifacts are consumed by downstream skills."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Knowledge Research — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `knowledge-research` skills read this before operating.

## Scope

Research skills collect, process, and synthesize knowledge from external sources. Their outputs are structured for downstream consumption by writing skills or direct user delivery.

## Skills Overview

| Skill | Input | Output |
|-------|-------|--------|
| `deep-research` | research topic, scope | `_research/` folder with findings + report |
| `paper-extractor` | PDF file paths | structured metadata + extracted text |
| `paper-research` | paper metadata | multi-phase knowledge extraction |

## Output Folder Structure

```
_research/
├── meta.json               ← run metadata (topic, date, depth, sources used)
├── findings/
│   ├── overview.md         ← executive summary with key conclusions
│   ├── domain.md           ← domain/market analysis
│   ├── competitors.md      ← competitor analysis (if applicable)
│   ├── patterns.md         ← recurring design/implementation patterns
│   └── <topic-specific>.md ← additional topic-specific findings
├── sources/
│   ├── index.md            ← source registry with relevance scores
│   └── <source-id>.md      ← individual source summaries
└── synthesis/
    └── report.md           ← full synthesized report with citations
```

## meta.json Format

```json
{
  "schema_version": "1.0",
  "run_id": "<uuid>",
  "topic": "<research topic>",
  "scope": ["domain", "competitors", "patterns", "colors_fonts"],
  "depth": "light | moderate | deep",
  "started_at": "YYYY-MM-DDTHH:MM:SSZ",
  "completed_at": "YYYY-MM-DDTHH:MM:SSZ",
  "source_count": 0,
  "model": "<model-id>"
}
```

## Citation Format

In-line citations use `[^N]` footnote format. Each findings file ends with:

```markdown
## Sources
[^1]: <Title> — <URL or description> (<YYYY>)
[^2]: ...
```

## Consumption by Downstream Skills

- **dev-conceptualization** reads `_research/findings/` during concept phases
- **knowledge-writing** reads `_research/synthesis/report.md` as source material
- Research output should be **self-contained** — no external URLs required to interpret findings
