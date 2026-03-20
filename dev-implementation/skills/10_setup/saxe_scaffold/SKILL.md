---
name: implement-1-setup-1-scaffold
description: "PostXL project scaffolding from a completed concept. This skill should be used when the user asks to 'scaffold the app', 'create the project', 'bootstrap the app', 'set up the project', or 'initialize the project'. Creates a blank PostXL project, copies the data schema, runs initial code generation, sets up the database, and initializes git."
---

ROLE  Scaffold agent — creates a buildable PostXL project from a completed _concept/ pipeline.

READS
  _concept/1_discovery/1_overview/brief.md                — app name, slug, elevator pitch
  _concept/3_blueprint/3_datamodel/postxl-schema.json     — data model (custom + standard models, auth config)
  ? _concept/3_blueprint/1_techstack/stack.md             — PostXL stack confirmation, additional integrations
  ? _concept/3_blueprint/2_architecture/architecture.md  — custom modules, processes, external integrations
  ? _concept/3_blueprint/3_datamodel/seed.json            — scenario-based seed data (populated scenario)
  ? _concept/1_discovery/3_brand/tokens.json              — brand tokens (applied later by implement-1-setup-2-foundation)

WRITES
  <app-slug>/                                  — generated PostXL project directory
  _implementation/PLANS.md                     — implementation plan initialized from concept
  _implementation/progress.json                — all features in pending status
  _implementation/decisions.md                 — empty decision log with header

REFERENCES
  shared/contracts/prerequisites.md                     — tool prerequisite checks
  shared/contracts/implementation_structure.md          — _implementation/ folder layout
  shared/contracts/git_workflow.md                      — branch naming and commit conventions
  shared/contracts/verification.md                      — build verification checks (Level 1)
  shared/contracts/concept_structure.md                 — where to find concept artifacts
  references/project_structure.md              — generated PostXL directory layout
  references/seed_data_transform.md            — seed.json transformation rules
  references/auth_policy_template.md           — default authorization policy

REQUIRES
  hard: pnpm, git, @postxl/cli
  soft: docker (database migration deferred without it)
  state: _concept/3_blueprint/3_datamodel/postxl-schema.json exists
  state: _concept/1_discovery/1_overview/brief.md exists

STEP 1: Read concept context
  - Read brief.md for app name, slug, and elevator pitch
  - Read postxl-schema.json for model count, schema names, auth config
  - Read stack.md if present to confirm PostXL stack
  IF _concept/3_blueprint/2_architecture/architecture.md exists
    - Extract custom_modules[], apps[], external_integrations[], protocols[]
    - Identify npm packages needed (see implement-1-setup-3-infrastructure references/dependency_mapping.md)

STEP 2: Confirm with user
  - Present summary: app name, slug, model count (N custom + M standard), branch name
  IF architecture exists with custom modules
    - Include: Custom modules: <list>, Additional processes: <list>
  CHECKPOINT scaffold_confirm
    > "Ready to set up your project '<app-name>'. This creates the foundation that everything else builds on.
    >
    > Technical details (if interested):
    >   Models: N custom + M standard, Branch: implement/<app-slug>
    >
    > Proceed with setup?"

STEP 3: Create PostXL project
  IF inside an existing git repository
    $ pxl create-project --name <app-slug> --schema _concept/3_blueprint/3_datamodel/postxl-schema.json --skip-git
  ELSE
    $ pxl create-project --name <app-slug> --schema _concept/3_blueprint/3_datamodel/postxl-schema.json
  - If pxl not globally available, use pnpx @postxl/cli
  NEVER use --skip-generate

STEP 3b: Install architecture dependencies
  IF _concept/3_blueprint/2_architecture/architecture.md exists
    - Map external_integrations to npm packages (see implement-1-setup-3-infrastructure references/dependency_mapping.md)
    - Map protocols to npm packages (websocket → @fastify/websocket, ws, @types/ws)
    - $ cd backend && pnpm add <mapped packages>
    - $ cd backend && pnpm add -D <mapped dev packages>
    - Add TypeScript path aliases to backend/tsconfig.json for each custom_module
    - This step is additive only — actual module code is created by implement-1-setup-3-infrastructure

STEP 4: Run environment setup
  $ cd <app-slug> && pnpm run setup
  - Copies all .env.example files to .env (root, backend/apps/api, frontend, e2e)

STEP 5: Run database migration
  IF docker is available
    $ docker-compose up -d postgres
    $ pnpm prisma migrate dev --name init
    - Fix schema validation errors or migration conflicts if they occur
  ELSE
    - Warn: database migration deferred, app runs in stateless/in-memory mode

STEP 6: Configure seed data
  IF _concept/3_blueprint/3_datamodel/seed.json exists
    - Extract populated scenario from seed.json
    - Transform per references/seed_data_transform.md
    - Write to backend/test-data.json
    - Update backend/libs/seedData/src/seed-migrations.ts with jsonSeed entry

STEP 7: Configure authorization policy
  - Populate authorization-policy.service.ts schemas per references/auth_policy_template.md
  - Adjust roles based on schema auth config and concept feature requirements

STEP 8: Verify build
  $ cd backend && pnpm run build
  $ cd frontend && pnpm run test:types
  $ pnpm run lint
  UNTIL all checks pass

STEP 9: Initialize git and implementation tracking
  $ git init
  $ git checkout -b implement/<app-slug>
  - Create _implementation/PLANS.md from concept's implementation plan
  - Create _implementation/progress.json with all must-have features in pending status
  - Create _implementation/decisions.md with empty header

STEP 10: Initial commit
  $ git add -A
  $ git commit -m "scaffold: initialize PostXL project from concept schema"

STEP 11: Emit completion event
  EMIT [implement-1-setup-1-scaffold] started run_id=<uuid>
  EMIT [implement-1-setup-1-scaffold] completed project=<app-slug> models=N build=passed branch=implement/<app-slug>
  - Present summary: models generated, build status, branch name
  - Suggest next: implement-1-setup-2-foundation (branding + shared services) or implement (full pipeline)

MUST  verify postxl-schema.json exists before starting
MUST  run Level 1 verification before committing
MUST  initialize _implementation/ tracking structure
MUST  create git branch per shared/contracts/git_workflow.md convention
MUST  run pnpm run setup before any build or migration step
NEVER proceed without user confirmation at Step 2
NEVER commit code that does not build
NEVER modify _concept/ files (read-only)
NEVER skip database migration when docker is available

CHECKLIST
  - [ ] postxl-schema.json validated and copied
  - [ ] Environment setup complete (.env files exist)
  - [ ] Database migrated (or deferred with docker warning)
  - [ ] Seed data configured from concept scenarios
  - [ ] Authorization policy populated with default rules
  - [ ] Backend builds successfully
  - [ ] Frontend type-checks successfully
  - [ ] Lint passes
  - [ ] Git branch created: implement/<app-slug>
  - [ ] _implementation/ tracking initialized
  - [ ] Initial commit created
