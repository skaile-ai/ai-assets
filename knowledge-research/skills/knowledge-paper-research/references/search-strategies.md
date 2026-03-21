# Search Strategies — Knowledge Paper Research

Query templates for each research dimension.
Replace `{X}` placeholders with values extracted from the paper's JSON metadata.

---

## Invocation

```bash
uv run $SKILL_DIR/scripts/search.py "<query>" \
  [--categories <cat>] \
  [--time_range <day|week|month|year>] \
  [--results <n>]
```

`SEARXNG_URL` is read from the environment (default: `http://localhost:8080`).
If SearXNG is unavailable, use any web-search tool with the same query strings.

---

## Author Research — one query block per author

Run all four queries for **each** author. Results feed `author_<Name>.md`.

```bash
# 1. Basic profile and institutional page
uv run $SKILL_DIR/scripts/search.py \
  "{Author Full Name} researcher {field}"

# 2. Scholarly profile (Scholar, Semantic Scholar, ORCID)
uv run $SKILL_DIR/scripts/search.py \
  "{Author Full Name} site:scholar.google.com OR site:semanticscholar.org OR site:orcid.org"

# 3. Recent news and activity (last 12 months)
uv run $SKILL_DIR/scripts/search.py \
  "{Author Full Name} {affiliation}" \
  --categories news --time_range year

# 4. Recent publications
uv run $SKILL_DIR/scripts/search.py \
  "{Author Full Name} {paper title keywords} publication 2023 OR 2024 OR 2025 OR 2026"
```

---

## Paper Analysis — `<stem>_paper_description.md`

*Note: Primarily based on paper content, but minor searches can help clarify methods.*

```bash
# Clarify non-standard methodology
uv run $SKILL_DIR/scripts/search.py \
  "{method name} methodology {field} tutorial explanation"

# Clarify dataset provenance if ambiguous
uv run $SKILL_DIR/scripts/search.py \
  "{dataset name} creation provenance description"
```

---

## External Context & Similar Work — `<stem>_similar_work.md`

```bash
# Similar experiments and studies
uv run $SKILL_DIR/scripts/search.py \
  "{field} similar experiment {task} study 2022..2026"

# Replications and follow-ups
uv run $SKILL_DIR/scripts/search.py \
  "{paper title} replication follow-up extension results"

# State of the art / competing methods
uv run $SKILL_DIR/scripts/search.py \
  "{task name} SOTA benchmark comparison {field} 2024..2026"
```

---

## Theoretical Foundations — `<stem>_background.md`

```bash
# Identify suitable theoretical models
uv run $SKILL_DIR/scripts/search.py \
  "{problem domain} theoretical models frameworks review"

# Deep-dive on seminal papers of the framework
uv run $SKILL_DIR/scripts/search.py \
  "{framework name} seminal paper origin evolution survey"

# Theoretical debate / critiques
uv run $SKILL_DIR/scripts/search.py \
  "{framework name} theoretical criticism limitation debate"
```

---

## Clinical Utility — `<stem>_clinical_implications.md`

```bash
# Clinical/Practical application
uv run $SKILL_DIR/scripts/search.py \
  "{key findings} clinical application psychiatric medical practice"

# Integration into existing field guidelines
uv run $SKILL_DIR/scripts/search.py \
  "{field} clinical guidelines {treatment/diagnosis} update 2024..2026"

# Practical implementation barriers
uv run $SKILL_DIR/scripts/search.py \
  "implementing {method/finding} in clinical setting barriers benefits"
```

---

## Strategy Notes

- **Parallelise** all queries across dimensions simultaneously before writing
  any file — this avoids context loss between phases.
- Use `--time_range year` for news and reception queries; omit for seminal/foundational searches.
- Use `--results 15` when you need more signal than the default 10.
- If a query returns zero results, drop the most specific term and retry once.
- Always record the URL of every source used; cite inline as `[Title](URL)`.
- Mark unavailable URLs explicitly as `(URL unavailable)`.
