---
name: "skailup-techstack"
description: "Use when the project brief exists and tech stack hasn't been chosen. Discovers available stacks from skaileup-standards/profiles/, asks plain-language questions, recommends the best match, and writes stack.md."
metadata:
  version: "1.0.0"
  tags:
    - "techstack"
    - "framework"
    - "database"
    - "hosting"
    - "architecture"
    - "profile"
    - "stack-selection"
  source: "MERGED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires:
      - id: brief
        gate: hard
    produces:
      - id: techstack
        description: "Technology stack decisions with reasoning"
    consumes:
      - id: features
        gate: soft
      - id: onboarding-decisions
        gate: soft
    seed_modes:
      techstack:
        - adopt
        - extend
      default: adopt
  prerequisites:
    files:
      - path: "_concept/discovery/brief.md"
        gate: hard
        description: "Project brief required to recommend appropriate tech stack"
    inputs_required:
      - id: platform
        label: "Platform"
        type: select
        options:
          - web
          - mobile
          - api
          - desktop
        hint: "What are you building?"
    inputs_optional:
      - id: framework_experience
        label: "Framework experience"
        type: text
        hint: "Vue, React, Python, or starting fresh?"
      - id: data_heavy
        label: "Data-heavy app?"
        type: boolean
        hint: "Lots of tables, lists, sorting, and filtering?"
      - id: managed_vs_selfhosted
        label: "Hosting preference"
        type: select
        options:
          - self-hosted
          - managed/cloud
        hint: "Manage your own server or use hosted services?"
    reads:
      - path: "_concept/experience/features"
        description: "Feature complexity and count to inform stack recommendation"
    produces:
      - path: "_concept/blueprint/techstack.md"
        description: "Chosen tech stack with profile reference for downstream skills"
  user_inputs:
    dialog:
      - id: "framework_experience"
        label: "Framework experience"
        type: "text"
        required: false
        hint: "Vue, React, Python, or starting fresh?"
      - id: "platform"
        label: "Platform"
        type: "select"
        required: true
        options:
          - "web"
          - "mobile"
          - "api"
          - "desktop"
        hint: "What are you building?"
      - id: "data_heavy"
        label: "Data-heavy app?"
        type: "boolean"
        required: false
        hint: "Lots of tables, lists, sorting, and filtering?"
      - id: "managed_vs_selfhosted"
        label: "Hosting preference"
        type: "select"
        required: false
        options:
          - "self-hosted"
          - "managed/cloud"
        hint: "Manage your own server or use hosted services?"
    files: []
---

# Tech Stack — Stack Selection

## Overview

The **techstack** skill is the Tech Stack Advisor. It helps the user choose the
right tools for their project through plain-language questions. It discovers
available stacks at runtime from `skaileup-standards/profiles/*/SKILL.md` and recommends
the best match based on the user's answers.

The `tech_stack_skill` field written to `stack.md` is the single reference all
downstream skills use to find implementation recipes.

## When to Use

- The project brief exists and no tech stack has been chosen
- The user says "tech stack", "framework", "what should we build with", "database choice"
- The user wants to change or re-evaluate the tech stack
- The orchestrator dispatches this after the project brief is approved

## When NOT to Use

- `_concept/blueprint/techstack.md` already exists and is approved
- No project brief yet — run **overview** first
- The user already has a codebase — use **audit** or **reverse-engineer** instead

## Prerequisites

**REQUIRED BACKGROUND:** Read `skaileup-shared/contracts/concept_structure.md` and
`skaileup-shared/contracts/frontmatter.md` before proceeding.

**Hard gate:** `_concept/discovery/brief.md` must exist.

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/discovery/brief.md` | Yes |
| Must read | `skaileup-standards/profiles/*/SKILL.md` | Yes (stack discovery) |
| Check if present | `_concept/experience/features/**/*.md` | No (complexity signals) |
| Check if present | `_concept/_grounding/overview/user_input.json` | No (complexity + pre-answers) |
| Check if present | `_concept/_grounding/general/onboarding.md` | No (skip pre-answered questions) |
| Never load | `_concept/blueprint/datamodel/`, `_concept/experience/screens/` | — |

## Standalone Mode

**Gate check:** `_concept/discovery/brief.md` must exist.
**On completion:** Present stack summary and suggest next steps (architecture, datamodel).

---

ROLE  Tech Stack Advisor — discovers available profiles, recommends the best match,
      and writes `_concept/blueprint/techstack.md`.

READS
  _concept/discovery/brief.md              — app name, description, audience
  skaileup-standards/profiles/*/SKILL.md                     — available stack profiles (discovered at runtime)
  ? _concept/experience/features/**/*.md            — feature requirements (complexity signals)
  ? _concept/_grounding/overview/user_input.json        — complexity field + pre-collected answers
  ? _concept/_grounding/general/onboarding.md           — pre-answered tech preferences

WRITES
  _concept/blueprint/techstack.md             — full stack definition with tech_stack_skill reference

REFERENCES
  skaileup-shared/contracts/concept_structure.md      — valid _concept/ paths
  skaileup-shared/contracts/frontmatter.md            — stack.md frontmatter fields
  references/integration_categories.md           — additional integration checklist

MUST  discover available stacks from skaileup-standards/profiles/ at runtime — never hardcode
MUST  include tech_stack_skill field in stack.md frontmatter — downstream skills depend on it
MUST  include Additional Integrations section in stack.md
MUST  include Trade-offs Considered section in stack.md
NEVER  skip user review — tech stack is a high-impact decision
NEVER  use stack-specific types or output in stack.md (no SQL, no Prisma schema)

EMIT  [techstack] started run_id=<uuid>

STEP 1: Read context
  - Read brief.md for the app description, audience, and domain
  - Check _grounding/overview/user_input.json for complexity and any pre-answered tech preferences
  IF _grounding/general/onboarding.md exists
    - Read onboarding notes — skip questions already answered there
  IF experience/features/ exists
    - Read feature files to gauge complexity signals

STEP 2: Discover available stacks
  - Scan skaileup-standards/profiles/*/SKILL.md
  - For each found profile, read:
    - The Identity table (name, framework, UI library, backend, database, etc.)
    - The "When to Use" section
  - Build a comparison table of all discovered profiles

EMIT  [techstack] checkpoint phase=stacks_discovered count=<N>

STEP 2b: Determine involvement level
  - Read complexity from _grounding/overview/user_input.json (default: standard)
  IF complexity is "small"
    - > "I'll pick a stack based on your features. I'll show you what I chose. Want to review the options instead?"
    - Proceed in automatic mode: use complexity signals + brief to recommend, skip to STEP 4
  IF complexity is "complex"
    - > "The tech stack is a key decision for your app. I recommend we go through it together — or I can propose something and you review?"
    - Default to involved mode → continue to STEP 3
  ELSE (standard)
    - > "Would you like to pick a tech stack together, or should I recommend one based on your features?"

STEP 3: Ask plain-language questions (involved mode)
  - If user_inputs were pre-collected, use them and skip those questions
  - Otherwise ask one at a time:
    1. Do you or your team have experience with a framework? (Vue, React, Python, or starting fresh?)
    2. Is this a web app, mobile app, desktop tool, or API only?
    3. How data-heavy is your app? (lots of tables/lists vs. mostly content/forms)
    4. Do you need a content management system or admin panel?
    5. Do you want to manage your own server, or prefer hosted/cloud?
    6. Any budget constraints? (self-hosted = no monthly cost, cloud = pay-as-you-go)
  - Adjust next question based on each answer

STEP 4: Recommend
  - Based on answers + discovered profiles, select the best matching profile
  - Present the recommendation:
    > "Based on your answers, I recommend:
    >
    > **[profile-name]** — [frontend] + [UI library] + [backend] on [database].
    >
    > Why:
    > - [reason 1 tied to user's answer]
    > - [reason 2 tied to feature signals]
    > - [reason 3 tied to preference]
    >
    > Want me to go with this, or would you like to change anything?"
  - If user wants a stack not in the discovered list, note it as custom and set tech_stack_skill: custom

STEP 5: Ask about additional integrations
  - Reference features/ to identify likely integrations (if present)
  - Ask one round covering integration categories from references/integration_categories.md:
    - Payment gateways (Stripe, Mollie, etc.)
    - Email / SMS services (SendGrid, Resend, Twilio, etc.)
    - File storage (S3-compatible, cloud, local)
    - Analytics / error tracking (PostHog, Sentry)
    - Domain-specific (AI/ML APIs, search, maps, PDF)
  - Frame questions around existing features where possible:
    > "I see a billing feature — do you need a payment gateway like Stripe?"
  - If no integrations needed, document "None identified"

EMIT  [techstack] checkpoint phase=preset_recommended tech_stack_skill=<profile-id>

STEP 6: Write stack file
  - $ mkdir -p _concept/blueprint

  OUTPUT _concept/blueprint/techstack.md
    ---
    platform: <web|mobile|api|desktop>
    frontend: <framework + version>
    ui_library: <component library>
    backend: <backend framework or service>
    orm: <ORM or query layer>
    database: <database engine>
    auth: <auth solution>
    hosting: <self-hosted|managed>
    package_manager: <bun|pnpm|npm|yarn>
    css: <CSS framework>
    tech_stack_skill: <profile-id>
    last_updated: <YYYY-MM-DD>
    ---
    # Tech Stack

    ## Frontend: <value>
    <Why this framework fits the project>

    ## UI Library: <value>
    <Why this component library fits the project>

    ## Backend: <value>
    <Why this backend was chosen>

    ## Database: <value>
    <Production readiness notes>

    ## Auth: <value>
    <Auth method and key features>

    ## Hosting: <value>
    <Hosting setup summary>

    ## Additional Integrations
    <List of identified integrations, or "None identified.">

    ## Trade-offs Considered
    <Always include — explain what was weighed against what>

STEP 7: Human approval
  CHECKPOINT stack_approved
    > "Your app will be built on [stack name].
    > [If integrations found]: It will connect to [services] for [purposes].
    > [If no integrations]: No external services needed — everything is self-contained.
    >
    > Technical details (if interested):
    >   [Frontend / UI / Backend / Database / Auth]
    >   Additional integrations: [list or none]
    >
    > Approve to continue, or tell me what to change."

  UNTIL user explicitly approves

STEP 8: Hand off
  > "Tech stack approved. Next steps:
  > - Run `architecture` to document the system design
  > - Run `datamodel` to design the data schema
  > - Run `concept-orchestrator` to continue the full pipeline"

EMIT  [techstack] completed run_id=<uuid> tech_stack_skill=<profile-id> additional_integrations=<N>

CHECKLIST
  - [ ] skaileup-standards/profiles/ was scanned for available stacks
  - [ ] stack.md exists with all required frontmatter fields
  - [ ] tech_stack_skill field is set (matches a skaileup-standards/profiles/ directory or "custom")
  - [ ] Additional Integrations section present
  - [ ] Trade-offs Considered section present
  - [ ] User has explicitly approved the stack

---

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Key decisions only — technology names, brief rationale |
| `medium` | Standard documentation — decisions with reasoning, diagrams, trade-offs (default) |
| `max` | Comprehensive documentation — full ADRs, alternative analysis, migration paths |

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Hardcoding stack details without reading profiles | Always scan skaileup-standards/profiles/*/SKILL.md — new profiles may have been added. |
| Omitting `tech_stack_skill` | This field is mandatory — all downstream skills depend on it. |
| Recommending without asking | Always ask the user, even if the answer seems obvious. Framework preference matters. |
| Skipping Trade-offs Considered | Always explain what was weighed. Tech stack is a high-impact long-term choice. |
| Writing Prisma schema or SQL in stack.md | stack.md is documentation only. Stack translation happens in the datamodel skill. |

## Integration

- **Called by:** `concept-orchestrator` or standalone (parallel track after overview)
- **Requires:** `_concept/discovery/brief.md`
- **Feeds into:** `architecture`, `datamodel`, `scaffold`, `mock` — all read `tech_stack_skill` from stack.md
