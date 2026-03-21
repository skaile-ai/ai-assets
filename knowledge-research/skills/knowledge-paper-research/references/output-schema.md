# Output Schema — Knowledge Paper Research

`STEM` = paper filename stem (e.g. `my_paper` for `my_paper.pdf`).
All paths are relative to `PAPER_DIR`.

---

## `<stem>.meta.md` — Paper Metadata

Location: **next to the source file** (not inside `_research/`).

```markdown
# <Title>

> Source: <Venue>, <Year> | DOI: <DOI>

## Authors
<Author 1> (<Affiliation>), <Author 2> (<Affiliation>), …

## Abstract
<Abstract verbatim from paper>

## Keywords
<keyword 1>, <keyword 2>, …

## Publication Info
- **Venue**: <conference / journal full name>
- **Year**: <year>
- **DOI**: <doi>
- **Received**: <date if available>
- **Accepted**: <date if available>
- **Open Access**: <yes / no / unknown>
```

---

## `<stem>_research/<stem>_paper_description.md` — Paper Analysis (Internal)

```markdown
# Paper Analysis — <Title>

> Source: <paper title> (<year>)

## From the Paper

### Study Design & Methodology
<Detailed prose chapter describing the study type (RCT, observational, computational, etc.), rationale, sample/dataset details, and procedure.>

### Results & Findings
<Detailed prose chapter describing key quantitative results, main claims, and figures/tables context derived directly from the paper.>

### Limitations Stated by Authors
<From the paper's own limitations / discussion section.>

## Gaps & Open Questions
- <What is unclear or unverifiable about the methodology from the paper text alone>
- <Unresolved questions the authors explicitly defer to future work>
```

---

## `<stem>_research/<stem>_similar_work.md` — External Context & Competitors

```markdown
# External Context & Similar Work — <Title>

> Source: <paper title> (<year>)

## From the Paper
### Related Work Overview
<Brief summary of how the authors position themselves relative to existing literature.>

## Web Research

### Similar Experiments & Studies
<Analysis of comparable studies, datasets or approaches found in the web, with [Title](URL) citations.>

### Replications & Follow-ups
<Known replication attempts, extensions or subsequent field progress, with [Title](URL) citations.>

### Competing Methods
<Other approaches to the same task or problem domain, with [Title](URL) citations.>

### Sources
- [<Title>](<URL>)

## Gaps & Open Questions
- <Missing links between this paper and the broader field>
- <Contradictory findings in similar studies>
```

---

## `<stem>_research/<stem>_background.md` — Theoretical Foundations

```markdown
# Theoretical Foundations — <Title>

> Source: <paper title> (<year>)

## From the Paper
### Problem Formulation & Core Assumptions
<How the paper frames its theoretical base and what it assumes to be true.>

## Web Research

### Theoretical Frameworks & Models
<Detailed explanation of 2-3 suitable theoretical foundations or models the paper relies on, with [Title](URL) citations.>

### Theoretical Lineage
<Where these theories come from; seminal papers and evolution, with [Title](URL) citations.>

### Sources
- [<Title>](<URL>)

## Gaps & Open Questions
- <Theoretical gaps or unresolved conceptual debates in the broader literature.>
```

---

## `<stem>_research/<stem>_clinical_implications.md` — Clinical Utility

```markdown
# Clinical Utility — <Title>

> Source: <paper title> (<year>)

## From the Paper
### Practical Recommendations
<What the authors suggest for practice/application based on their findings.>

## Web Research

### Clinical Application
<Analysis of how these findings can be integrated into medical, psychiatric, or therapeutic practice, with [Title](URL) citations.>

### Field Integration
<Current guidelines or standard practices that this paper might influence or challenge, with [Title](URL) citations.>

### Sources
- [<Title>](<URL>)
```

---

## `<stem>_research/authors/author_<AuthorName>.md` — Per-Author Profile

**Filename:** prefix with `author_`, underscores for spaces.
Example: `author_Nadia_Micali.md`

**No `## Gaps & Open Questions` section in author files.**

```markdown
# <Author Full Name>

> Source: <paper title> (<year>)

## From the Paper
- **Affiliation**: <institution as stated in paper>
- **Contact**: <email if listed in paper>
- **Role in study**: <e.g. "Lead author; conceived the project; wrote first draft">

## Web Research

### Current Affiliation
<Institution, department, and position — with source URL if found>

### Research Focus
<1–2 sentences describing their primary domain and methods>

### Notable Publications
1. [<Title>](<URL>) — <Journal / Conference>, <Year>
2. …
(up to 5; use `(URL unavailable)` if DOI/URL not confirmed)

### Citation Metrics
- **h-index**: <value or "not found">
- **Total citations**: <value or "not found">
- **Google Scholar**: [Profile](<URL>) (or "not found")
- **ORCID**: [<id>](<https://orcid.org/<id>>) (or "not found")

### Recent Activity
<Grants, new positions, press coverage, invited talks — last 12 months.
Omit this subsection entirely if nothing found.>

### Sources
- [<Title>](<URL>)
```
