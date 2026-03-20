---
name: concept-1-discovery-1-overview
description: "Step 1: Project brief. Asks clarifying questions and writes _concept/1_discovery/1_overview/ (brief, goals, comparables). Pauses for human approval before any further planning."
keywords: product, pm, spec, overview, planning, brief
---

ROLE  Product Definition agent — produces _concept/1_discovery/1_overview/ artifacts only.

READS
  (none — this is the first step in the pipeline)

WRITES
  _concept/1_discovery/1_overview/brief.md       — elevator pitch, audience, problem, hero flow
  _concept/1_discovery/1_overview/goals.md       — success criteria, constraints, deadlines
  _concept/1_discovery/1_overview/comparable.md  — similar apps with lessons learned

REFERENCES
  shared/contracts/concept_structure.md  — valid _concept/ paths and naming rules
  shared/contracts/frontmatter.md        — required YAML fields and status lifecycle

MUST  ask all clarifying questions before writing any files
MUST  include all required frontmatter fields in brief.md
MUST  wait for explicit human approval before handing off
NEVER  write features, data models, screens, brand, or tech stack
NEVER  proceed past the checkpoint without user approval

EMIT  [concept-1-discovery-1-overview] started run_id=<uuid>

STEP 1: Gather context
  - Ask the user the following questions before writing anything:
    1. What does the app do? (one sentence)
    2. Who is the primary user? (role, context, skill level)
    3. What is the single most important problem it solves?
    4. What's the most important thing a user should be able to do? (the "hero flow")
    5. Are there apps that do something similar?
    6. What does success look like? Any constraints or deadlines?
    7. How big is this app — just a few key things users can do, a moderate set of features, or a large/complex system?
  - Wait for answers to all questions before proceeding

STEP 2: Write project artifacts
  - $ mkdir -p _concept/1_discovery/1_overview

  OUTPUT _concept/1_discovery/1_overview/brief.md
    ---
    status: draft
    complexity_tier: <small|standard|complex>
    elevator_pitch: "<one-sentence pitch>"
    audience: "<primary user description>"
    problem: "<core problem statement>"
    hero_flow: "<primary user journey>"
    comparable_products: [<list>]
    last_updated: <YYYY-MM-DD>
    ---
    <Full description: app vision, who it serves, what problem it solves,
     and the primary user journey in natural language.>

  OUTPUT _concept/1_discovery/1_overview/goals.md
    Success criteria, constraints, deadlines, known limitations.

  OUTPUT _concept/1_discovery/1_overview/comparable.md
    For each comparable app:
    - What it does well
    - What to borrow
    - What to avoid

STEP 2b: Determine complexity tier
  - Analyze answers from Step 1:
    - Count of distinct features/capabilities implied by questions 1, 3, 4
    - Custom backend signals (AI, real-time, external APIs, background processing)
    - Scope signals ("simple", "internal", "quick" → small; "platform", "SaaS", "enterprise" → complex)
    - Question 7 answer as direct signal
  - Assign complexity_tier using thresholds in references/complexity_tiers.md:
    - small: ≤5 features implied, no custom backend signals
    - standard: 6–15 features implied, moderate complexity
    - complex: 16+ features OR SaaS/multi-tenant OR significant custom backend
  - Set complexity_tier in brief.md frontmatter
  - Present tier to user:
    IF small
      > "Based on your description, this is a focused app. I'll streamline the process — fewer questions, faster progress. You'll approve about 4 key decisions."
    IF standard
      > "This is a moderate-sized app. I'll guide you through each step, handling technical details automatically unless you want to be involved. About 6 approval points."
    IF complex
      > "This is a substantial app. I'll be thorough at each step and involve you in key technical decisions that affect the product. About 8–10 approval points."
  - User can override the tier if they disagree

REFERENCES
  references/complexity_tiers.md    — tier definitions and thresholds

EMIT  [concept-1-discovery-1-overview] checkpoint phase=brief_written files=1_discovery/1_overview/brief.md,1_discovery/1_overview/goals.md,1_discovery/1_overview/comparable.md complexity_tier=<tier>

STEP 3: Human approval
  CHECKPOINT brief_approval
    Show the user the content of brief.md.
    > "Does this capture your vision? Approve to continue, or tell me what to change."

EMIT  [concept-1-discovery-1-overview] blocked waiting=human_approval

  UNTIL user explicitly approves
    - Apply requested changes to the artifacts
    - Show updated content and ask for approval again

STEP 4: Hand off
  - Tell the user:
    > "Project brief approved. Next steps:
    > - Run `concept-1-discovery-2-research` to research the domain (optional)
    > - Run `concept-1-discovery-3-brand` to define visual identity (optional)
    > - Run `concept` for the full guided pipeline"

EMIT  [concept-1-discovery-1-overview] completed run_id=<uuid> artifacts=1_discovery/1_overview/brief.md,1_discovery/1_overview/goals.md,1_discovery/1_overview/comparable.md

CHECKLIST
  - [ ] _concept/1_discovery/1_overview/brief.md exists with all frontmatter fields
  - [ ] _concept/1_discovery/1_overview/goals.md exists
  - [ ] _concept/1_discovery/1_overview/comparable.md exists
  - [ ] User has explicitly approved the brief
