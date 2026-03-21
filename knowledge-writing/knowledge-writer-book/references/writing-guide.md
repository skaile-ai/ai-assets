# Writing Guide & Templates

Read this file when drafting or refining chapter content.

## Template Resolution

Templates define the structural skeleton for each chapter. Resolution order:

1. **Book-specific**: `books/<slug>/templates/<name>.md` — if the book defines custom templates
2. **Skill default**: `<skill_dir>/templates/<name>.md` — built-in fallback templates

When the Architect assigns `template: diagnosis` to a chapter, the Writer:
1. Checks if `books/<slug>/templates/diagnosis.md` exists → use it
2. Otherwise uses `<skill_dir>/templates/diagnosis.md`
3. All templates inherit common frame from `_base.md`: Lernziele, Fallvignette, Praxistipps, Kernaussagen

## Style Reference (German Medical Prose)

### Good Example

> Die Aufmerksamkeitsdefizit-/Hyperaktivitätsstörung (**ADHS**, ICD-11: 6A05) zählt mit einer Prävalenz von 5–7 % im Kindes- und Jugendalter zu den häufigsten neuropsychiatrischen Erkrankungen. Die Ätiologie ist multifaktoriell: Genetische Faktoren erklären etwa 70–80 % der Varianz, wobei Polymorphismen im Dopamin-Transporter-Gen (DAT1) und im Dopamin-Rezeptor-Gen (DRD4) am besten repliziert sind. Umweltfaktoren wie pränatale Nikotinexposition und niedriges Geburtsgewicht modulieren das Risiko zusätzlich.

**Why it works**: Precise, data-rich, uses ICD-11 code, bold for key terms, no filler.

### Bad Example

> ADHS ist eine sehr häufige Störung, die viele Kinder betrifft. Es gibt verschiedene Ursachen, die zu der Entstehung beitragen können. Es ist allgemein bekannt, dass sowohl genetische als auch umweltbedingte Faktoren eine Rolle spielen.

**Why it fails**: Vague, no data, filler phrases, no ICD code, no bold terms.

## Formatting Rules

| Element | Format |
|---------|--------|
| Key term (first use) | **bold** |
| Disorder name | Full German name + (ICD-11: code) |
| Drug dosage | "Methylphenidat 0,5–1,0 mg/kg KG/Tag" |
| Guideline reference | "Die AWMF S3-Leitlinie empfiehlt..." |
| Study reference | "In einer Metaanalyse (k=12, n=3400) zeigte sich..." |
| Clinical pearl | `> **Praxistipp**: ...` blockquote |
| Cross-reference | "→ siehe Kapitel [X]" |

## Recursive Refinement Protocol

When drafting, apply this cycle internally:

1. **Draft**: Write the section from the scaffold, following the template structure
2. **Critique**: Read the draft as the Critic. Ask:
   - Are claims substantiated? Can they be traced to a `research/sources/` file?
   - Is the ICD-11 code included?
   - Any filler phrases to remove?
   - Is it too long/too short?
   - Does it follow the template's section structure?
3. **Refine**: Apply corrections
4. **Output**: Only produce the refined version to `draft.md`

Never output first drafts. The user should only see the refined result.

## Context Feeding Strategy

When writing Chapter N:

| Load | From | Purpose |
|------|------|---------|
| Knowledge Scaffold | `chapters/<ch_id>/scaffold.md` | Content to transform |
| Book Bible | `concept/book_bible.md` | Voice, scope, principles |
| Style Decisions | `concept/style_decisions.md` | Terminology, formatting |
| Prior Summaries | `index.yaml` → `chapters.*.summary` | Cross-chapter consistency |
| Assigned Template | Resolved via template resolution | Section structure |

**Do NOT load**:
- Full text of prior chapters
- All research source files (only those listed in scaffold frontmatter `sources`)
- Templates for other chapter types

**If a cross-reference is needed**: Use `grep` to search `chapters/*/final.md` for the relevant term to locate the exact prior section.

## Research-to-Scaffold Pipeline

The Researcher produces two types of files that feed into the scaffold:

### 1. Source Files (`research/sources/`)
Individual evidence documents — one per guideline, meta-analysis, or textbook. Each has structured YAML frontmatter with `type`, `year`, `topics`, `url`, and `confidence` fields.

### 2. Syntheses (`research/syntheses/`)
Cross-cutting evidence aggregations. These are created when multiple sources contribute to a single topic (e.g., "all evidence on stimulant pharmacotherapy" compiled from 4 individual source notes). Syntheses include evidence tables, open questions, and summary conclusions.

The **scaffold** then references specific sources and syntheses in its frontmatter `sources` field, creating a traceable chain: `source → synthesis → scaffold → draft → final`.

## Reference TOC Template (Medical Textbook)

Use this as structural guidance. Adapt to the specific disorder.

```markdown
# 1. Grundlagen und Klassifikation
## 1.1 Definition und Historie
## 1.2 Epidemiologie
## 1.3 Klassifikation (ICD-11 / DSM-5)

# 2. Ätiologie und Pathogenese
## 2.1 Genetische und biologische Faktoren
## 2.2 Psychologische und soziale Faktoren
## 2.3 Integrative Modelle

# 3. Klinik und Symptomatik
## 3.1 Leitsymptome
## 3.2 Alters- und geschlechtsspezifische Besonderheiten

# 4. Diagnostik
## 4.1 Klinische Erfassung und Instrumente
## 4.2 Differenzialdiagnosen

# 5. Komorbiditäten
## 5.1 Häufige Begleiterkrankungen
## 5.2 Klinische Implikationen

# 6. Therapie und Intervention
## 6.1 Psychotherapeutische Verfahren
## 6.2 Pharmakotherapie
## 6.3 Multimodales Konzept

# 7. Verlauf und Prognose
## 7.1 Langzeitentwicklung
## 7.2 Prognostische Faktoren

# 8. Sonderthemen (optional)
## 8.1 [Themenspezifisch, z.B. Schule/Familie/Kultur]
```
