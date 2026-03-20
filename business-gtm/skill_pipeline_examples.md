# Skill Pipeline Examples — Beyond Software

## Why the Pattern Generalizes

The concept skill pipeline (idea → brief → research → features → screens → architecture → data model) follows a pattern that appears in every structured knowledge-work process:

1. A **trigger** starts the work (a question, a request, a document)
2. Work proceeds through **phases** that progressively refine understanding
3. Each phase produces **artifacts** that the next phase builds on
4. **Human judgment** is needed at key decision points
5. The final output is a **deliverable** (a presentation, an offer, a report, an app)

The only thing that changes between verticals is the **skill definitions** (what each phase does) and the **artifact schemas** (what each artifact looks like). The platform infrastructure — session containers, artifact management, checkpoint gates, git-backed state, quality validation — is universal.

Below are concrete pipeline designs for non-software verticals. Each could be implemented as a SAXE skill template.

---

## Pipeline 1: Consulting — Research & Recommendation

**Trigger:** A strategic question or hypothesis from a client or internal team
**Deliverable:** A validated recommendation with supporting evidence and presentation

| Phase             | Skill                    | Input Artifacts                   | Output Artifacts                                                                                                              | Gate                   |
| ----------------- | ------------------------ | --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| 1. Scoping        | `consult-scope`          | User conversation                 | `question.md` (refined question, scope boundaries, success criteria), `stakeholders.md`                                       | Approve scope          |
| 2. Research       | `consult-research`       | `question.md`                     | `sources/` (annotated research), `findings.md` (structured findings with evidence quality ratings)                            | Approve findings       |
| 3. Hypothesis     | `consult-hypothesis`     | `question.md`, `findings.md`      | `hypotheses.md` (ranked hypotheses with supporting/contradicting evidence, confidence levels)                                 | Approve hypotheses     |
| 4. Analysis       | `consult-analysis`       | `hypotheses.md`, `findings.md`    | `analysis.md` (deep-dive on top hypotheses, data tables, frameworks applied), `models/` (financial models, decision matrices) | Approve analysis       |
| 5. Recommendation | `consult-recommendation` | `analysis.md`, `question.md`      | `recommendation.md` (executive summary, recommendation, risks, implementation roadmap, next steps)                            | Approve recommendation |
| 6. Presentation   | `consult-presentation`   | `recommendation.md`, brand tokens | `slides.md` (Reveal.js or PowerPoint), `appendix/` (backup slides, data tables)                                               | Approve presentation   |

**Artifact workspace:**

```
_consulting/
├── 1_scope/          question.md, stakeholders.md
├── 2_research/       sources/, findings.md
├── 3_hypotheses/     hypotheses.md
├── 4_analysis/       analysis.md, models/
├── 5_recommendation/ recommendation.md
└── 6_presentation/   slides.md, appendix/
```

**Complexity tiers:**

- **Quick** (2-3 day turnaround): Scope → Research → Recommendation (skip hypothesis/analysis phases, lighter gates)
- **Standard** (1-2 weeks): Full pipeline
- **Deep** (multi-week): Extended research, multiple analysis rounds, peer review gates

---

## Pipeline 2: Consulting — Workshop Delivery

**Trigger:** A workshop request (internal or client-facing)
**Deliverable:** Complete workshop package ready for delivery

| Phase        | Skill                | Input Artifacts           | Output Artifacts                                                                                                                | Gate              |
| ------------ | -------------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------- | ----------------- |
| 1. Brief     | `workshop-brief`     | User conversation         | `brief.md` (objective, audience, duration, format, constraints), `logistics.md`                                                 | Approve brief     |
| 2. Agenda    | `workshop-agenda`    | `brief.md`                | `agenda.md` (timed agenda with objectives per block, facilitation method, group sizes), `participant_profile.md`                | Approve agenda    |
| 3. Research  | `workshop-research`  | `brief.md`, `agenda.md`   | `context.md` (industry/company context), `benchmarks.md` (relevant data points for exercises)                                   | Approve research  |
| 4. Content   | `workshop-content`   | `agenda.md`, `context.md` | `slides.md` (presentation slides), `exercises/` (exercise instructions, templates, worksheets), `facilitator_guide.md`          | Approve content   |
| 5. Materials | `workshop-materials` | Full workspace            | `handouts/` (participant handouts), `pre_work.md` (pre-workshop reading/tasks), `follow_up.md` (post-workshop actions template) | Approve materials |

**Artifact workspace:**

```
_workshop/
├── 1_brief/      brief.md, logistics.md
├── 2_agenda/     agenda.md, participant_profile.md
├── 3_research/   context.md, benchmarks.md
├── 4_content/    slides.md, exercises/, facilitator_guide.md
└── 5_materials/  handouts/, pre_work.md, follow_up.md
```

---

## Pipeline 3: Sales — Offer Generation

**Trigger:** A prospective customer request or RFP
**Deliverable:** A tailored, priced offer document

| Phase              | Skill            | Input Artifacts                                | Output Artifacts                                                                                                                              | Gate                   |
| ------------------ | ---------------- | ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| 1. Intake          | `offer-intake`   | Customer request (email, call notes, RFP)      | `request.md` (structured requirements, customer profile, urgency, budget signals)                                                             | Approve interpretation |
| 2. Solution Design | `offer-solution` | `request.md`, product catalog, capability docs | `solution.md` (proposed solution mapping: requirements → products/services, configuration choices, alternatives considered)                   | Approve solution       |
| 3. Pricing         | `offer-pricing`  | `solution.md`, pricing rules, margin targets   | `pricing.md` (line items, volume discounts, payment terms, margin analysis), `comparison.md` (vs. standard pricing, vs. competitor estimates) | Approve pricing        |
| 4. Risk & Terms    | `offer-risk`     | `solution.md`, `pricing.md`                    | `risk_assessment.md` (delivery risks, dependencies, assumptions), `terms.md` (SLA, warranty, liability, IP)                                   | Approve terms          |
| 5. Document        | `offer-document` | Full workspace, brand tokens                   | `offer.pdf` (branded offer document), `cover_letter.md`, `executive_summary.md`                                                               | Approve offer          |

**Artifact workspace:**

```
_offer/
├── 1_intake/     request.md
├── 2_solution/   solution.md
├── 3_pricing/    pricing.md, comparison.md
├── 4_risk/       risk_assessment.md, terms.md
└── 5_document/   offer.pdf, cover_letter.md, executive_summary.md
```

**Key differentiator:** The pricing skill can reference internal pricing rules, margin targets, and historical deal data — domain knowledge that would be dangerous to expose in a general-purpose AI tool but is safe inside an IT-governed SAXE session.

---

## Pipeline 4: Due Diligence (M&A / PE)

**Trigger:** A target company identified for potential acquisition or investment
**Deliverable:** A structured due diligence report with risk assessment and valuation input

| Phase                    | Skill            | Input Artifacts                             | Output Artifacts                                                                                         | Gate                |
| ------------------------ | ---------------- | ------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------- |
| 1. Target Profile        | `dd-profile`     | Company name, public info, data room access | `profile.md` (company overview, market position, key metrics), `data_room_index.md`                      | Approve profile     |
| 2. Financial Analysis    | `dd-financial`   | `profile.md`, financial statements          | `financials.md` (revenue analysis, cost structure, working capital, debt), `models/financial_model.xlsx` | Approve financials  |
| 3. Commercial Assessment | `dd-commercial`  | `profile.md`, market data                   | `commercial.md` (market size, competitive position, customer concentration, growth drivers, risks)       | Approve commercial  |
| 4. Operational Review    | `dd-operational` | `profile.md`, operational data              | `operational.md` (org structure, key personnel, technology, processes, integration complexity)           | Approve operational |
| 5. Risk Matrix           | `dd-risk`        | All prior artifacts                         | `risks.md` (categorized risk matrix with likelihood/impact, deal-breakers flagged, mitigations)          | Approve risks       |
| 6. Investment Memo       | `dd-memo`        | Full workspace                              | `memo.md` (executive summary, investment thesis, key risks, valuation considerations, recommendation)    | Approve memo        |

**Artifact workspace:**

```
_due_diligence/
├── 1_profile/     profile.md, data_room_index.md
├── 2_financial/   financials.md, models/
├── 3_commercial/  commercial.md
├── 4_operational/ operational.md
├── 5_risk/        risks.md
└── 6_memo/        memo.md
```

---

## Cross-Cutting Observations

### What Stays the Same Across Verticals

| Aspect             | Universal Pattern                                                    |
| ------------------ | -------------------------------------------------------------------- |
| Session container  | Ephemeral Docker VM, IT-approved, AI-agent-driven                    |
| State management   | Git-backed artifact workspace, resumable                             |
| Human oversight    | Checkpoint gates with review/edit/approve                            |
| Quality validation | Automated checks (completeness, cross-references, schema compliance) |
| Complexity tiers   | Light/standard/deep controls phase count and gate frequency          |
| Brand/formatting   | Corporate tokens applied to all deliverables                         |
| Pipeline state     | Durable progress tracking (survives session interruptions)           |
| IT governance      | Skill templates, AI providers, audit logging, access control         |

### What Changes Per Vertical

| Aspect                | Vertical-Specific                                                  |
| --------------------- | ------------------------------------------------------------------ |
| Skill definitions     | The business logic, constraints, quality rules per phase           |
| Artifact schemas      | What each artifact looks like (frontmatter, structure, validation) |
| Artifact editors      | Format-specific UIs for review/editing (see below)                 |
| Phase sequence        | How many phases, what order, which are optional                    |
| Domain knowledge      | Industry terminology, regulations, reference data                  |
| Output format         | Code, slides, PDFs, spreadsheets, documents                        |
| External integrations | Data sources, APIs, tools specific to the domain                   |

### Artifact Viewers & Editors

The approval/editing UI adapts to the artifact format — not the vertical:

| Vertical           | Key Artifacts                               | Natural Editor                                              |
| ------------------ | ------------------------------------------- | ----------------------------------------------------------- |
| Software (concept) | Markdown specs, JSON schemas, brand tokens  | WYSIWYG editor, table editor, color pickers                 |
| Software (screens) | Interactive mockups                         | Storybook stories                                           |
| Consulting         | Research docs, recommendations, slide decks | WYSIWYG editor, PowerPoint-like or embedded Office Online   |
| Sales/Offers       | Pricing tables, offer documents             | Excel-like grid, Word-like editor or embedded Office Online |
| Due diligence      | Financial models, risk matrices, memos      | Excel-like grid, WYSIWYG editor                             |
| Workshop           | Agendas, slides, exercise worksheets        | WYSIWYG editor, PowerPoint-like or embedded Office Online   |

The platform maintains a registry of artifact types → editor components. Adding a new vertical mostly means mapping its artifacts to existing editors. Most business verticals are well-served by: WYSIWYG markdown + Office Online embedding (Word, PowerPoint, Excel).

### Natural Extension: Open Sessions

Not every task fits a structured pipeline. SAXE's session infrastructure also supports **open sessions** where users get raw access to their IT-approved VM:

- Freeform AI conversations with access to organizational knowledge bases
- Ad-hoc analysis, research, or document drafting
- Running custom agents or scripts in a sandboxed environment
- Prototyping a workflow before it becomes a formalized skill pipeline

The progression is natural: users start with open sessions for exploratory work, patterns emerge, and IT formalizes recurring patterns into skill templates. **Open sessions are the discovery mechanism for new pipelines.**

### Pipeline Composition

Pipelines can feed into each other. Examples:

- **Research → Presentation:** Output of consulting research pipeline becomes input to a presentation-generation pipeline
- **Due Diligence → Investment Memo → Board Deck:** Three pipelines chaining
- **Customer Request → Offer → Contract:** Sales pipeline output feeds legal/contract pipeline
- **Concept → Implement → Deploy:** Already working in software vertical

This is how SAXE grows from "run one pipeline" to "orchestrate multi-step business processes."

## Market Implications

Each pipeline example above represents a distinct use case that mid-size companies currently handle through:

- Manual work by expensive professionals
- Fragmented tools (one per step, no integration)
- Tribal knowledge (the process lives in someone's head)

SAXE replaces this with: **structured AI execution, governed by IT, with human judgment where it matters.** The same platform, the same session infrastructure, the same governance model — just different skill templates.
