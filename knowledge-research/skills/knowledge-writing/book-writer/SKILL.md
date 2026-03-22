---
name: "book-writer"
description: "Autonomous multi-agent skill for writing professional medical textbooks in child and adolescent psychiatry. Simulates a 5-agent team (Architect, Researcher, Pedagogue, Writer, Critic) to produce evidence-based, guideline-grounded content using a layered file architecture. Concept, research, scaffolds, and prose are stored in separate files. Triggers: \"Write a book about...\", \"Start a book...\", \"/start_book\", \"/plan_chapter\", \"/write_chapter\".\n"
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "knowledge-writing-contract"
---

# Book Writer: Autonomous Medical Authoring System

Simulate a team of 5 specialized agents to produce a high-quality medical textbook.

- Agent personas → [agents.md](references/agents.md)
- Writing guidelines → [writing-guide.md](references/writing-guide.md)
- Default templates → [templates/](templates/) (used as fallback when no book-specific templates exist)

## Architecture Overview

```
User Request
    ↓
┌──────────────────────────────────────────────────────┐
│  ARCHITECT — Book structure, concept, TOC            │
│    ↓                                                 │
│  RESEARCHER — Evidence gathering → research/ files   │
│    ↓                                                 │
│  PEDAGOGUE — Learning design, vignettes              │
│    ↓                                                 │
│  WRITER — Prose generation (German, academic)        │
│    ↓                                                 │
│  CRITIC — Consistency check, fact verification       │
│    ↕ (loop back to WRITER if issues found)           │
└──────────────────────────────────────────────────────┘
    ↓
  Layered Output → Separate files per concern
```

## Book Directory Structure

Every book lives in its own folder under the workspace. The agent creates this structure automatically.

```
books/
└── <book_slug>/
    ├── index.yaml                  # Global state: progress, chapter list, cross-refs
    ├── concept/
    │   ├── book_bible.md           # Audience, scope, guiding principles
    │   ├── toc.md                  # Table of Contents with chapter IDs
    │   └── style_decisions.md      # Voice, terminology, formatting choices
    ├── research/
    │   ├── sources/
    │   │   ├── <source_slug>.md    # One file per guideline/meta-analysis/textbook
    │   │   └── ...
    │   └── syntheses/
    │       ├── <topic_slug>.md     # Cross-cutting evidence synthesis
    │       └── ...
    ├── templates/                  # Book-specific templates (OPTIONAL, override skill defaults)
    │   └── ...
    ├── chapters/
    │   ├── <ch_id>/
    │   │   ├── scaffold.md         # Knowledge Scaffold (preserved permanently)
    │   │   ├── draft.md            # Writer's draft
    │   │   ├── review.md           # Critic's feedback
    │   │   └── final.md            # Approved prose
    │   └── ...
    └── exports/
        └── full_book.md            # Assembled final book (optional)
```

## Template Resolution

Templates define the structural skeleton for each chapter type. Resolution order:

1. **Book-specific**: `books/<slug>/templates/<template_name>.md` — if the book defines its own
2. **Skill default**: `<skill_dir>/templates/<template_name>.md` — fallback from skill's built-in templates

Available default templates:

| Template | File | Use Case |
|----------|------|----------|
| Base | `_base.md` | All chapters inherit from this |
| Diagnosis | `diagnosis.md` | Chapters on specific disorders (ADHS, ASD, Angststörungen) |
| Neurotransmitter | `neurotransmitter.md` | Neurobiological mechanism chapters |
| Psychotherapy | `psychotherapy.md` | CBT, family therapy, etc. |
| Pharmacotherapy | `pharmacotherapy.md` | Medication chapters |
| Etiology | `etiology.md` | Cause/mechanism chapters |
| Epidemiology | `epidemiology.md` | Prevalence, risk factors |
| Comorbidity | `comorbidity.md` | Comorbidity chapters |

Each domain template **extends** `_base.md` by adding domain-specific sections while keeping the common frame (Lernziele, Fallvignette, Kernaussagen).

## index.yaml — Global State

The `index.yaml` file is the single source of truth for project state. It is read at the start of every command and updated after every phase.

```yaml
title: "ADHS im Kindes- und Jugendalter"
slug: adhd
language: de
status: in_progress  # planning | in_progress | complete
created: 2025-02-19
audience: "Fachärzte KJP, Psychologische Psychotherapeuten"
chapters:
  ch01_grundlagen:
    title: "Grundlagen und Klassifikation"
    status: final         # planned | scaffolded | drafted | reviewed | final
    template: diagnosis
    sources: [awmf_s3_adhd, nice_adhd_2024]
    summary: "Kapitel definiert ADHS nach ICD-11 und DSM-5..."
  ch02_aetiologie:
    title: "Ätiologie und Pathogenese"
    status: scaffolded
    template: etiology
    sources: [meta_genetics_2024]
    summary: ""
```

---

## Workflow

Three phases, each producing distinct file artifacts. Every phase reads `index.yaml` first.

### Phase 1: `/start_book [topic]` → Architect + Researcher

1. **Analyze** scope and target audience
2. **Research** — conduct web search for AWMF/NICE guidelines, standard textbooks
3. **Propose** a snake_case book slug → **wait for user approval**
4. **Create directory structure** as defined above
5. **Write `concept/book_bible.md`**:
   - Topic, target audience, scope boundaries
   - Guiding principles and pedagogical approach
   - Key references and authorities
6. **Write `concept/style_decisions.md`**:
   - Terminology conventions (e.g., ICD-11 vs DSM-5 preference)
   - Language register, formatting rules
7. **Populate `research/sources/`** — one markdown file per source found during initial research. Each source file has frontmatter:
   ```yaml
   ---
   type: guideline | meta-analysis | textbook | review | expert-opinion
   year: 2024
   topics: [adhd, therapy, stimulants]
   url: "https://..."
   confidence: established | emerging | controversial
   ---
   ```
   Followed by structured notes: key findings, relevant data, quotes.

8. **Write `concept/toc.md`** — complete hierarchical TOC:

   **Completeness rules:**
   - **Minimum 6 main chapters** (H1), covering at minimum:
     1. Grundlagen & Klassifikation (Definition, Historie, Epidemiologie)
     2. Ätiologie & Pathogenese (Bio-psycho-soziale Faktoren)
     3. Diagnostik (Klinische Erfassung, Differenzialdiagnosen)
     4. Komorbiditäten (häufige Begleiterkrankungen)
     5. Therapie & Intervention (evidenzbasierte Verfahren)
     6. Verlauf & Prognose (Langzeitentwicklung)
   - Each main chapter must have **at least 2 subsections** (H2)
   - Each section/subsection includes a `<planned_content>` block:
     ```xml
     <planned_content>
     Ziel: [Pedagogical goal in German]
     Leitlinien: [Relevant AWMF/NICE/ICD-11 references]
     Vorlage: [template name, e.g. diagnosis]
     Inhalt:
     - [ ] [Planned content point 1 (German)]
     - [ ] [Planned content point 2 (German)]
     - [ ] [Planned content point 3 (German)]
     </planned_content>
     ```

9. **Write `index.yaml`** with all chapters set to `status: planned`

> **CRITICAL: Phase 1 produces ONLY structure and research files. Do NOT write any prose. Every section in `toc.md` must contain ONLY a heading + a `<planned_content>` block.**

---

### Phase 2: `/plan_chapter [chapter_id]` → Researcher + Pedagogue

> **SCOPE**: Work ONLY on the target chapter. Do NOT modify other chapters.

1. **Read** `index.yaml` to identify the chapter and its assigned template
2. **Resolve template**: check `books/<slug>/templates/` first, then fall back to skill's `templates/` directory. Read the template to understand required sections.
3. **Deep Research** (Researcher):
   - Search for specific guidelines, meta-analyses (last 5 years)
   - **Create or update `research/sources/`** files for each new source found
   - **Create `research/syntheses/<topic>.md`** if the chapter topic warrants a cross-cutting evidence synthesis (e.g., evidence map for pharmacotherapy)
4. **Instructional Design** (Pedagogue):
   - Define 3–4 **Lernziele**
   - Design a **clinical vignette**
   - Plan tables, Merksätze, Praxistipps
5. **Write `chapters/<ch_id>/scaffold.md`** — a German Knowledge Scaffold following the template structure:
   ```markdown
   ---
   chapter_id: ch01_grundlagen
   template: diagnosis
   sources: [awmf_s3_adhd, nice_adhd_2024]
   status: scaffolded
   ---

   # Grundlagen und Klassifikation

   **Ziel**: [From original plan or refined]
   **Leitlinien**: [From research]
   **Lernziele**:
   1. [Lernziel 1]
   2. [Lernziel 2]
   3. [Lernziel 3]

   ## Fallvignette (Entwurf)
   > [Draft clinical vignette]

   ## [Section from template]
   **Inhalt**:
   - [x] [Essential fact / Core content — include in prose]
   - [x] [Another essential fact]
   - [ ] [Optional/Additional context — skip unless user marks with [x]]

   ## [Next section from template]
   ...
   ```

   > **IMPORTANT**: Mark core content with `[x]` and optional content with `[ ]`.

6. **Update `index.yaml`**: set chapter status to `scaffolded`, list sources used
7. **STOP HERE**. Do NOT write the full chapter prose yet.

---

### Phase 3: `/write_chapter [chapter_id]` → Writer + Critic

1. **Read** `index.yaml` for chapter summaries of prior chapters (context)
2. **Read** `chapters/<ch_id>/scaffold.md`
3. **Read** `concept/book_bible.md` for voice and scope
4. **Drafting** (Writer):
   - **FILTER**: Process **ONLY** items marked with `[x]`. Ignore items marked with `[ ]`.
   - Transform selected points into German academic prose following the template structure
   - Follow medical logic: Etiology → Symptoms → Diagnosis → Therapy
   - Integrate vignettes, Praxistipps, tables seamlessly
   - Write output to **`chapters/<ch_id>/draft.md`**
5. **Review Loop** (Critic):
   - Check if all `[x]` items from scaffold are covered
   - Check definitions, ICD codes, guideline references
   - Check consistency with prior chapter summaries
   - Write feedback to **`chapters/<ch_id>/review.md`**
   - If issues found → return to Writer for corrections
6. **Final Output**:
   - Write approved prose to **`chapters/<ch_id>/final.md`**
   - Add "Kernaussagen" (Key Takeaways) table at end
   - **Update `index.yaml`**: set status to `final`, write 2-sentence summary

> **NOTE**: The scaffold is **never overwritten**. It remains as a permanent reference alongside the final prose.

---

## Context Window Management

Long books exceed any context window. Apply these strategies strictly:

| Strategy | Implementation |
|----------|---------------|
| **Chapter Summaries** | Stored in `index.yaml`. Feed summaries of prior chapters to Writer/Critic. |
| **Sequential Processing** | Only load: current scaffold + `book_bible.md` + summaries from `index.yaml`. Never load the entire book. |
| **Just-in-Time Research** | Load only the `research/sources/` and `research/syntheses/` files referenced in the scaffold's frontmatter `sources` list. |
| **Template-Guided Loading** | Load only the resolved template for the current chapter type. |
| **Cross-Reference Check** | Before finalizing, grep `chapters/*/final.md` for terms that appear in the current chapter to catch contradictions. |

## Research File Format

Every file in `research/sources/` follows this structure:

```markdown
---
type: guideline
year: 2023
topics: [adhd, diagnostik, therapie]
url: "https://register.awmf.org/..."
confidence: established
---

# AWMF S3-Leitlinie ADHS im Kindes- und Jugendalter

## Kernempfehlungen
- ...

## Relevante Daten
- Prävalenz: 5–7 % (Metaanalyse, k=12)
- ...

## Zitate
> "Methylphenidat ist Mittel der ersten Wahl..." (S. 47)

## Offene Fragen
- [ ] Langzeitdaten zu Guanfacin bei <6-Jährigen
```

## Commands

| Command | Phase | Agents | Output |
|---------|-------|--------|--------|
| `/start_book [topic]` | 1 | Architect + Researcher | Directory, concept/, research/sources/, toc.md, index.yaml |
| `/plan_chapter [ch_id]` | 2 | Researcher + Pedagogue | research/ updates, chapters/\<ch_id\>/scaffold.md |
| `/write_chapter [ch_id]` | 3 | Writer + Critic | chapters/\<ch_id\>/draft.md, review.md, final.md |
