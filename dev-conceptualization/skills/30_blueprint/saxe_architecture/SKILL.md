---
name: concept-3-blueprint-2-architecture
description: "Step 8: System architecture. Documents app structure, NestJS module layout, data flow, and communication protocols. Starts from PostXL defaults and extends for project-specific needs (custom apps, agents, real-time protocols, external integrations)."
keywords: architecture, modules, nestjs, dataflow, protocols, backend, api, services, websocket, agents
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

ROLE  Architecture agent — produces _concept/3_blueprint/2_architecture/ from features and tech stack.

READS
  _concept/1_discovery/1_overview/brief.md             — app name, audience, problem domain, complexity_tier
  _concept/2_experience/2_features/**/*.md             — feature requirements (drives module needs)
  _concept/3_blueprint/1_techstack/stack.md           — confirmed tech stack and constraints

WRITES
  _concept/3_blueprint/2_architecture/architecture.md — complete system architecture specification

REFERENCES
  shared/contracts/concept_structure.md        — valid _concept/ paths and naming rules
  shared/contracts/frontmatter.md              — required YAML fields and status lifecycle
  shared/contracts/semantic_types.md           — PostXL field types (for data flow context)
  references/postxl_defaults.md       — PostXL default architecture (app structure, data flow, auth, modules, docker)
  references/output_template.md       — architecture.md section templates and field documentation
  concept/concept/references/complexity_tiers.md      — tier definitions and phase behavior
  concept/concept/references/technical_involvement.md — involvement choice protocol

REQUIRES
  state: _concept/1_discovery/1_overview/brief.md exists
  state: _concept/2_experience/2_features/ contains at least one .md file
  state: _concept/3_blueprint/1_techstack/stack.md exists

MUST  start from PostXL defaults and extend only where features demand it
MUST  include all six sections in architecture.md (overview, modules, data flow, protocols, integrations, infrastructure)
MUST  document PostXL defaults as baseline in every section before adding extensions
MUST  include all required frontmatter fields
NEVER  reinvent standard PostXL modules — document them, don't replace them
NEVER  skip external integration error handling or credential management docs

EMIT  [concept-3-blueprint-2-architecture] started run_id=<uuid> reads=1_discovery/1_overview/brief.md,2_experience/2_features/,3_blueprint/1_techstack/stack.md

STEP 1: Read context
  - Read brief.md for app name, audience, and problem domain
  - Read all feature .md files for functional requirements
  - Read stack.md for tech stack decisions and constraints
  - Analyze feature requirements for:
      - Entity state machines → may require event-driven patterns
      - Complex workflows → may need background processing
      - Multi-actor interactions → may need real-time protocols
      - External system interactions → may need adapter modules

STEP 1b: Determine involvement level
  - Read complexity_tier from _concept/1_discovery/1_overview/brief.md frontmatter (default: standard)
  IF complexity_tier is small
    - > "I'll design the architecture automatically based on your features. I'll show you a summary when done. Want to review the details instead?"
    - Default to automatic mode
  IF complexity_tier is complex
    - > "The architecture is a key decision point for your app — it determines what your app can do behind the scenes. I recommend we go through the design together. Or I can propose something and you review?"
    - Default to involved mode
  ELSE (standard)
    - > "Would you like to be involved in the architecture design, or should I handle it based on your features?"

  IF automatic mode
    - Analyze all features for architecture needs (same analysis, without asking the user)
    - Make best-judgment decisions on apps, protocols, integrations
    - Skip to STEP 3 (write) with determined requirements
  ELSE (involved mode)
    - Continue to STEP 2

STEP 2: Analyze architecture needs (involved mode)
  - For each feature, assess whether PostXL defaults are sufficient or extensions are needed
  - Ask the user:
    1. Does this app need to do anything in the background — like processing, scheduling, or long-running tasks?
    2. Do users need to see updates in real-time — like live chat, notifications, or collaborative editing?
    3. Does the app connect to any external services — like payment systems, email providers, or third-party APIs?
    4. Are there any special communication needs — like streaming data or instant updates?
    5. Does the app handle data in any non-standard way — like event logging, multi-step workflows, or file processing?
  - If user is uncertain, analyze features and suggest what's likely needed

STEP 3: Write architecture document
  - $ mkdir -p _concept/3_blueprint/2_architecture
  - Write architecture.md following references/output_template.md
  - Include PostXL defaults as baseline in every section (see references/postxl_defaults.md)
  - Add project-specific extensions identified in Step 2

  OUTPUT _concept/3_blueprint/2_architecture/architecture.md
    ---
    status: draft
    apps: [api, web, ...]
    custom_modules: [...]
    protocols: [http, trpc, ...]
    external_integrations: [...]
    last_updated: <YYYY-MM-DD>
    ---
    Six sections: System Overview, Backend Module Structure, Data Flow,
    Communication Protocols, External Integrations, Infrastructure.
    (See references/output_template.md for full section structure.)

EMIT  [concept-3-blueprint-2-architecture] checkpoint phase=architecture_documented apps=N custom_modules=N protocols=N

STEP 4: Human approval
  CHECKPOINT architecture_review
    Show business summary first:
      > "Your app's structure is designed to support [primary business capability].
      > [If real-time features]: Features like [feature] will show updates instantly.
      > [If custom modules]: Custom logic handles [business process description].
      > [If external integrations]: Your app connects to [service] for [purpose].
      > [If no extensions]: The standard setup covers everything your app needs.
      >
      > Technical details (if interested):
      >   Apps: N, Custom modules: N, Protocols: N, Integrations: N, Docker services: N
      >
      > Approve, or tell me what to change."

EMIT  [concept-3-blueprint-2-architecture] blocked waiting=human_approval

  UNTIL user explicitly approves
    - Apply requested changes to architecture.md
    - Show updated summary and ask for approval again

STEP 5: Hand off
  - Tell the user:
    > "Architecture documented. This informs:
    > - `concept-3-blueprint-3-datamodel` — knows which entities need custom modules vs standard CRUD
    > - `concept-2-experience-3-screens` — knows which protocols screens use for real-time features
    > - `implement` — knows which custom modules and apps to build
    >
    > Next: run `concept-3-blueprint-3-datamodel` or continue with the concept pipeline."

EMIT  [concept-3-blueprint-2-architecture] completed run_id=<uuid> apps=N custom_modules=N external_integrations=N

CHECKLIST
  - [ ] _concept/3_blueprint/2_architecture/architecture.md exists with all frontmatter fields
  - [ ] All six sections present (overview, modules, data flow, protocols, integrations, infrastructure)
  - [ ] PostXL defaults documented as baseline in every section
  - [ ] Custom modules have purpose and dependency listed
  - [ ] Non-standard protocols have endpoints, message types, lifecycle, and error handling
  - [ ] External integrations have API/SDK, data exchanged, error handling, and credentials
  - [ ] User has explicitly approved the architecture
