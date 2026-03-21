---
name: concept-3-blueprint-1-techstack
description: "Step 7: Tech stack documentation for PostXL. Documents the fixed PostXL production stack (Vite + React 19, NestJS, tRPC, Prisma, PostgreSQL, Keycloak) and identifies any additional integrations needed."
keywords: techstack, postxl, react, nestjs, trpc, prisma, postgresql, keycloak, tailwind, radix
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

ROLE  Tech Stack Advisor — documents the fixed PostXL stack and identifies additional integrations.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, description, audience
  ? _concept/2_experience/2_features/**/*.md            — feature requirements (complexity signals)

WRITES
  _concept/3_blueprint/1_techstack/stack.md            — full stack documentation + integrations

REFERENCES
  shared/contracts/concept_structure.md              — valid _concept/ paths and naming
  shared/contracts/frontmatter.md                    — stack.md frontmatter fields
  references/postxl_stack.md                — fixed stack definition + stack.md template
  references/integration_categories.md      — additional integration checklist + examples
  concept/concept/references/complexity_tiers.md      — tier definitions and phase behavior
  concept/concept/references/technical_involvement.md — involvement choice protocol

REQUIRES
  state: _concept/1_discovery/1_overview/brief.md exists

# ─── Workflow ───────────────────────────────────────────────────────────

EMIT  [concept-3-blueprint-1-techstack] started run_id=<uuid> reads=1_discovery/1_overview/brief.md,2_experience/2_features/

STEP 1: Read context
  - Read brief.md for app name, description, and audience
  - Read 2_experience/2_features/**/*.md if present to gauge feature complexity
  - Note any feature-level hints about external services (payments, email, maps, etc.)

STEP 2: Document the fixed stack
  - $ mkdir -p _concept/3_blueprint/1_techstack
  - Write _concept/3_blueprint/1_techstack/stack.md using the template in references/postxl_stack.md
  - The core stack is NOT configurable — copy it verbatim
  - Set frontmatter status: draft, last_updated: <today>

EMIT  [concept-3-blueprint-1-techstack] checkpoint phase=stack_documented framework=PostXL

STEP 2b: Determine involvement level for integrations
  - Read complexity_tier from _concept/1_discovery/1_overview/brief.md frontmatter (default: standard)
  IF complexity_tier is small
    - > "I'll check if your features need any external services (like payments or email) and add them automatically. Want to review the list instead?"
    - Default to automatic mode
  IF complexity_tier is complex
    - > "Let's go through potential integrations together — your features may need external services like payment processing, email, or third-party APIs."
    - Default to involved mode
  ELSE (standard)
    - > "Would you like to review potential integrations, or should I identify them from your features?"

  IF automatic mode
    - Scan features for integration keywords (payment, email, SMS, maps, AI, file upload, etc.)
    - Auto-populate integrations section based on findings
    - Skip to STEP 4 with identified integrations
  ELSE (involved mode)
    - Continue to STEP 3

STEP 3: Ask about additional integrations (involved mode)
  - Present the integration categories from references/integration_categories.md
  - Ask ONE round of questions covering:
    - External APIs (payment, email, SMS, maps)
    - File storage (S3-compatible, local, cloud)
    - Analytics / monitoring (PostHog, Sentry, Grafana)
    - Third-party auth providers (social login, enterprise SSO via Keycloak)
    - Domain-specific services (AI/ML, geolocation, search)
  - If user has no additional integrations, move on

STEP 4: Write final stack file
  - Update the "Additional Integrations" section of stack.md with any identified integrations
  - If none identified, leave as "None identified."

OUTPUT _concept/3_blueprint/1_techstack/stack.md
  ---
  status: draft
  platform: web
  framework: PostXL
  frontend: "Vite + React 19"
  ui_library: "@postxl/ui-components (Radix + Tailwind v4)"
  backend: "NestJS + Fastify + tRPC"
  orm: Prisma
  database: PostgreSQL
  auth: Keycloak
  package_manager: pnpm
  last_updated: <today>
  ---
  # Tech Stack
  ## Platform: PostXL
  ...
  ## Additional Integrations
  <identified integrations or "None identified.">

STEP 5: Human approval
  CHECKPOINT stack_review
    > "Your app is built on a proven production stack — fast, secure, and ready to scale.
    > [If integrations found]: It will connect to [services] for [purposes].
    > [If no integrations]: No external services needed — everything is self-contained.
    >
    > Technical details (if interested):
    >   Full stack: PostXL (React 19, NestJS, PostgreSQL, Keycloak)
    >   Additional integrations: [list or none]
    >
    > Approve to continue."

EMIT  [concept-3-blueprint-1-techstack] blocked waiting=human_approval prompt="Review 3_blueprint/1_techstack/stack.md"

STEP 6: Hand off
  - Confirm approval and summarize downstream consumers:
    - `concept-3-blueprint-3-datamodel` — produces postxl-schema.json (PostXL native format)
    - `app-design` — uses @postxl/ui-components
    - `implement-1-setup-1-scaffold` — scaffolds a PostXL project
  - Suggest next: run `concept-3-blueprint-3-datamodel` or continue with concept pipeline

EMIT  [concept-3-blueprint-1-techstack] completed run_id=<uuid> framework=PostXL additional_integrations=<count>

# ─── Constraints ────────────────────────────────────────────────────────

MUST  use the fixed PostXL stack — no framework choices
MUST  document all additional integrations identified by the user
MUST  set frontmatter status to draft until approved
NEVER  suggest alternative frameworks to PostXL core components
NEVER  skip the integration consultation step

CHECKLIST
  - [ ] stack.md contains complete PostXL stack definition
  - [ ] Frontmatter has all required fields (status, platform, framework, etc.)
  - [ ] Additional integrations section reflects user consultation
  - [ ] status is draft (set to approved only after checkpoint)
