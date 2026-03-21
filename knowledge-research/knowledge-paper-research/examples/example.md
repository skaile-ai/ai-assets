# Example — Viggers et al. 2026 (Anorexia Nervosa / ICD-11)

Complete annotated walkthrough of the pipeline on a real paper.

---

## Input

```
data/Anorexia/s00787-026-02972-1.pdf
```

**Paper**: "Evaluating ICD-11 anorexia nervosa in a clinical youth sample: a comparison with ICD-10"
**Authors**: Luna Thule Viggers, Mie Sedoc Jørgensen, Anne Bryde, Bilal Ashraf,
Signe Holm Pedersen, Nadia Micali, Mette Bentz
**Venue**: European Child & Adolescent Psychiatry (Springer), 2026
**DOI**: 10.1007/s00787-026-02972-1

---

## Phase 0 — PDF Conversion

```bash
docling \
  --to md \
  --image-export-mode referenced \
  --output data/Anorexia/ \
  data/Anorexia/s00787-026-02972-1.pdf
```

Produces:
- `data/Anorexia/s00787-026-02972-1.md` — working document
- `data/Anorexia/s00787-026-02972-1_artifacts/image_000000_*.png` (×7 figures)

Set variables:
```bash
PAPER_DIR="data/Anorexia"
STEM="s00787-026-02972-1"
RESEARCH_DIR="data/Anorexia/s00787-026-02972-1_research"
mkdir -p "$RESEARCH_DIR/authors"
```

---

## Phase 1 — Structural Extraction

```bash
EXTRACTED=$(uv run $SKILL_DIR/scripts/extract_structure.py \
  data/Anorexia/s00787-026-02972-1.md)
```

Abbreviated JSON output:
```json
{
  "title": "Evaluating ICD-11 anorexia nervosa in a clinical youth sample: a comparison with ICD-10",
  "authors": [
    "Luna Thule Viggers", "Mie Sedoc Jørgensen", "Anne Bryde",
    "Bilal Ashraf", "Signe Holm Pedersen", "Nadia Micali", "Mette Bentz"
  ],
  "abstract": "Objective: The International Classification of Diseases (ICD) was recently updated…",
  "year": "2026",
  "doi": "10.1007/s00787-026-02972-1",
  "venue": "European Child & Adolescent Psychiatry",
  "sections": [
    {"heading": "Introduction", "text": "Anorexia Nervosa (AN) is a serious mental disorder…"},
    {"heading": "Method", "text": "The study was a naturalistic cross-sectional study…"},
    {"heading": "Results", "text": "Retrospective application of ICD-11 criteria…"},
    {"heading": "Discussion", "text": "This study aimed to evaluate the ICD-11 AN criteria…"}
  ],
  "references": ["1. van Eeden AE…", "2. Zipfel S…", "…46 total"]
}
```

---

## Phase 2 — Meta File

Written to: `data/Anorexia/s00787-026-02972-1.meta.md`

```markdown
# Evaluating ICD-11 anorexia nervosa in a clinical youth sample: a comparison with ICD-10

> Source: European Child & Adolescent Psychiatry, 2026 | DOI: 10.1007/s00787-026-02972-1

## Authors
Luna Thule Viggers (CEDaR, Copenhagen University Hospital),
Mie Sedoc Jørgensen (CEDaR, Copenhagen University Hospital),
Anne Bryde (CEDaR, Copenhagen University Hospital),
Bilal Ashraf (CEDaR, Copenhagen University Hospital),
Signe Holm Pedersen (CAMHS, Copenhagen University Hospital),
Nadia Micali (CEDaR, Copenhagen / UCL Great Ormond Street Institute of Child Health),
Mette Bentz (CAMHS, Copenhagen University Hospital)

## Abstract
Objective: The International Classification of Diseases (ICD) was recently updated to an
11th edition, introducing significant changes to the diagnostic criteria for Anorexia Nervosa (AN).
Little is known about how these updated criteria impact diagnostic classification in previously
diagnosed populations…

## Keywords
Anorexia nervosa, ICD-11, Atypical AN, Eating disorders, Symptom severity, Subtypes

## Publication Info
- **Venue**: European Child & Adolescent Psychiatry
- **Year**: 2026
- **DOI**: 10.1007/s00787-026-02972-1
- **Received**: 2 July 2025
- **Accepted**: 18 January 2026
- **Open Access**: Yes (Creative Commons Attribution 4.0)
```

---

## Phase 3 — Search Queries Issued

### Author queries (sample)

```bash
# Nadia Micali — most prominent, run first
uv run $SKILL_DIR/scripts/search.py "Nadia Micali researcher eating disorders UCL"
uv run $SKILL_DIR/scripts/search.py "Nadia Micali site:scholar.google.com OR site:orcid.org"
uv run $SKILL_DIR/scripts/search.py "Nadia Micali UCL" --categories news --time_range year
uv run $SKILL_DIR/scripts/search.py "Nadia Micali eating disorders publication 2024 OR 2025 OR 2026"

# Mette Bentz — PI of VIBUS
uv run $SKILL_DIR/scripts/search.py "Mette Bentz CAMHS Copenhagen eating disorders researcher"
uv run $SKILL_DIR/scripts/search.py "Mette Bentz site:semanticscholar.org"
uv run $SKILL_DIR/scripts/search.py "Mette Bentz Copenhagen" --categories news --time_range year
```

### Theory queries

```bash
uv run $SKILL_DIR/scripts/search.py "ICD-11 eating disorder diagnostic criteria broadening survey"
uv run $SKILL_DIR/scripts/search.py "anorexia nervosa atypical AN classification comparison DSM-5 ICD-11"
uv run $SKILL_DIR/scripts/search.py "Johnson-Munguia 2024 atypical anorexia nervosa meta-analysis"
uv run $SKILL_DIR/scripts/search.py "Walsh 2023 systematic review atypical anorexia nervosa"
```

### Experiment queries

```bash
uv run $SKILL_DIR/scripts/search.py "VIBUS project eating disorders Copenhagen cohort"
uv run $SKILL_DIR/scripts/search.py "Child Eating Disorder Examination ChEDE psychometric validation"
uv run $SKILL_DIR/scripts/search.py "family-based treatment anorexia nervosa adolescents effectiveness"
uv run $SKILL_DIR/scripts/search.py "robust standard errors regression heteroscedasticity explanation"
```

### Results/reception queries

```bash
uv run $SKILL_DIR/scripts/search.py \
  "Evaluating ICD-11 anorexia nervosa clinical youth sample Viggers cited"
uv run $SKILL_DIR/scripts/search.py \
  "ICD-11 anorexia nervosa youth reclassification 2025 OR 2026"
uv run $SKILL_DIR/scripts/search.py \
  "Düplois 2023 ICD-10 ICD-11 eating disorders comparison"
```

### Concept queries

```bash
uv run $SKILL_DIR/scripts/search.py "OSFED other specified feeding eating disorder definition review"
uv run $SKILL_DIR/scripts/search.py "anorexia nervosa restrictive binge-purge subtype AN-R AN-BP clinical"
uv run $SKILL_DIR/scripts/search.py "Eating Disorder Examination EDE psychometric factor structure"
uv run $SKILL_DIR/scripts/search.py "Cohen d effect size interpretation eating disorders"
```

---

## Phase 4 — Thematic Files Written

```
data/Anorexia/s00787-026-02972-1_research/s00787-026-02972-1.theory.md
data/Anorexia/s00787-026-02972-1_research/s00787-026-02972-1.experiment.md
data/Anorexia/s00787-026-02972-1_research/s00787-026-02972-1.results.md
data/Anorexia/s00787-026-02972-1_research/s00787-026-02972-1.concepts.md
```

Each file opens with the source blockquote, contains `## From the Paper`,
`## Web Research` (with inline `[Title](URL)` citations), and `## Gaps & Open Questions`.

---

## Phase 5 — Author Files Written

```
data/Anorexia/s00787-026-02972-1_research/authors/
├── author_Luna_Thule_Viggers.md
├── author_Mie_Sedoc_Jørgensen.md
├── author_Anne_Bryde.md
├── author_Bilal_Ashraf.md
├── author_Signe_Holm_Pedersen.md
├── author_Nadia_Micali.md
└── author_Mette_Bentz.md
```

No `## Gaps & Open Questions` section in any author file.
Facts not verifiable from available sources are marked `not found`.

---

## Final Output Tree

```
data/Anorexia/
├── s00787-026-02972-1.pdf                       ← untouched source
├── s00787-026-02972-1.md                        ← docling markdown
├── s00787-026-02972-1_artifacts/                ← docling images (7 files)
├── s00787-026-02972-1.meta.md                   ← Phase 2
└── s00787-026-02972-1_research/
    ├── s00787-026-02972-1.theory.md             ← Phase 4
    ├── s00787-026-02972-1.experiment.md         ← Phase 4
    ├── s00787-026-02972-1.results.md            ← Phase 4
    ├── s00787-026-02972-1.concepts.md           ← Phase 4
    └── authors/
        ├── author_Luna_Thule_Viggers.md         ← Phase 5
        ├── author_Mie_Sedoc_Jørgensen.md        ← Phase 5
        ├── author_Anne_Bryde.md                 ← Phase 5
        ├── author_Bilal_Ashraf.md               ← Phase 5
        ├── author_Signe_Holm_Pedersen.md        ← Phase 5
        ├── author_Nadia_Micali.md               ← Phase 5
        └── author_Mette_Bentz.md               ← Phase 5
```

---

## Output Summary Table

| File | Status | Notes |
|---|---|---|
| `s00787-026-02972-1.meta.md` | ✓ written | next to paper |
| `…_research/…theory.md` | ✓ written | ICD-10→11 evolution, competing frameworks |
| `…_research/…experiment.md` | ✓ written | VIBUS cohort, ChEDE, reclassification logic |
| `…_research/…results.md` | ✓ written | full regression tables, effect sizes |
| `…_research/…concepts.md` | ✓ written | 24-row concept table |
| `…/authors/author_Luna_Thule_Viggers.md` | ✓ written | early-career; metrics not found |
| `…/authors/author_Mie_Sedoc_Jørgensen.md` | ✓ written | metrics not found |
| `…/authors/author_Anne_Bryde.md` | ✓ written | publishes as Bryde Christensen |
| `…/authors/author_Bilal_Ashraf.md` | ✓ written | name typo noted in paper |
| `…/authors/author_Signe_Holm_Pedersen.md` | ✓ written | Bentz et al. 2021 confirmed |
| `…/authors/author_Nadia_Micali.md` | ✓ written | full profile; ORCID confirmed |
| `…/authors/author_Mette_Bentz.md` | ✓ written | VIBUS PI; Bentz et al. 2021 cited |

---

## Lessons Learned (for improvement_ideas.md)

- SearXNG was not running — the `search.py` script exited with connection refused.
  The web-research sections were written using the agent's built-in web knowledge
  and `web_search` fallback. Searches should detect the SearXNG failure early and
  immediately switch to the available tool.
- The extract_structure.py script successfully parsed all metadata fields.
  The DOI parsing required the space-collapsed fallback path due to docling's
  character-spaced DOI formatting.
- Male subgroup in AN-BP (n=5) was noted as underpowered — worth flagging in
  concept and results files, not just experiment.
