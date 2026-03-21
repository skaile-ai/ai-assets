---
name: implement-1-setup-2-foundation
description: "App foundation and shared services setup. This skill should be used when the user asks to 'set up theming', 'apply brand', 'configure auth', 'implement the app shell', 'set up foundation', or 'configure shared services'. Applies brand tokens as CSS custom properties, configures authentication, implements the app shell layout, and sets up shared services like error handling and navigation."
metadata:
  stage: alpha
  requires:
  - implementation-contract
---

ROLE Foundation agent — applies brand tokens, customizes the PostXL-generated app shell, and verifies shared services.

READS
\_concept/1_discovery/3_brand/tokens.json — color scheme, typography, spacing, shadows
\_concept/1_discovery/3_brand/identity.md — design philosophy, atmosphere
\_concept/2_experience/3_screens/00_layout/shell.md — app shell layout, navigation, header, footer
\_concept/3_blueprint/1_techstack/stack.md — auth provider, additional integrations
? \_concept/3_blueprint/3_datamodel/seed.json — seed data for development

WRITES
frontend/src/styles/theme-default.css — brand tokens as CSS custom properties
frontend/src/components/AppSidebarNavigation.tsx — shared sidebar navigation
e2e/specs/example.spec.ts — updated example E2E tests
\_implementation/progress.json — foundation phase status
\_implementation/verification/screenshots/foundation/ — visual verification screenshots

REFERENCES
shared/contracts/prerequisites.md — tool prerequisite checks
shared/contracts/concept_structure.md — where to find concept artifacts
shared/contracts/implementation_structure.md — tracking updates
shared/contracts/verification.md — Level 1 build verification
shared/contracts/git_workflow.md — commit conventions
references/shell_customization.md — HSL conversion, DynamicTabbedSidebar pattern, PostXL defaults

REQUIRES
hard: pnpm, git
soft: agent-browser (visual verification skipped without it)
state: implement-1-setup-1-scaffold + implement-generate complete (scaffolded project with passing build)

STEP 1: Read concept inputs

- Read tokens.json: all color tokens (light + dark), font families, spacing scale, border radii, shadows
- Read identity.md: design philosophy, atmosphere description
- Read shell.md: navigation structure, sidebar items, header components, footer content
- Read stack.md: auth provider (typically Keycloak), any additional services
  EMIT [implement-1-setup-2-foundation] started run_id=<uuid> reads=1_discovery/3_brand,2_experience/3_screens/00_layout,3_blueprint/1_techstack

STEP 2: Apply brand tokens

- Convert hex values from tokens.json to HSL format (see references/shell_customization.md)
- Update frontend/src/styles/theme-default.css — replace default colors with HSL-converted brand tokens
- Set up Google Fonts (or other provider) for specified font families
- Update font family CSS custom properties to reference imported fonts
- Apply atmosphere (background gradients, glow effects) to root layout
  IF tokens.json specifies both light and dark modes
  - Write :root block for light mode and .dark block for dark mode
    $ pnpm run build
    EMIT [implement-1-setup-2-foundation] checkpoint phase=brand_tokens
    Commit: `foundation: apply brand tokens as CSS custom properties`

STEP 3: Configure authentication (verify and customize)

- Verify auth config matches concept's role requirements
- Adjust role mappings if concept defines custom roles beyond defaults
- Verify mock auth mode works (AUTH_MOCK=true)
  IF concept specifies SSO or social login
  - Configure Keycloak federation
    IF generated defaults match concept
  - Skip — no changes needed
    Commit: `foundation: configure authentication (Keycloak OIDC)` (if changes needed)

STEP 4: Customize app shell

- Read shell.md for navigation structure
- Create AppSidebarNavigation component with all nav items from shell spec
- Register AppSidebarNavigation in authorized-page-layout.tsx using @custom-start/@custom-end blocks
- Update header with project branding (logo, app name), verify user menu and theme toggle
  IF shell spec defines footer
  - Implement footer component
- Verify responsive behavior: mobile sidebar (collapsible/drawer) works per shell spec
  MUST use @postxl/ui-components for all UI primitives (Sidebar, Avatar, DropdownMenu, Button)
  NEVER let individual pages register navigation tabs — navigation is shared at layout level
  Commit: `foundation: customize app shell layout with shared sidebar navigation`

STEP 5: Verify shared services

- Verify error boundary renders with brand styling
- Verify Sonner toasts appear with correct theme
- Verify unauthenticated redirect to login works
  IF concept requires services beyond PostXL defaults (e.g., real-time presence)
  - Implement custom shared services
    Commit: `foundation: verify and customize shared services` (if changes needed)

STEP 6: Replace faker seed data with concept seed data

- Read \_concept/3_blueprint/3_datamodel/seed.json — use the `populated` scenario
- Create backend/test-data.json in PostXL scenario format:
  ```json
  { "scenarios": { "populated": { "description": "...", "data": { ... } }, "empty": { ... } } }
  ```

  - Map concept field names to Prisma camelCase (e.g., `user_sub` → `sub`, `provider_type` → `providerType`)
  - Assign explicit IDs to each entity (e.g., `usr-admin`, `proj-1`, `tpl-default`)
  - Resolve cross-references by slug/sub to use the assigned IDs as foreign keys
  - Include a Config entry in each scenario: `{ "id": "Config", "version": "0.0.1", "slug": "<project-slug>" }`
  - Create at least: `populated` (full demo data), `single_user` (minimal), `empty` (just config)
  - Keep only models that exist in the Prisma schema — skip entities not yet migrated
- Update backend/libs/seedData/src/seed-migrations.ts:
  - Replace the `excel` migration with a `jsonSeed` migration pointing to backend/test-data.json
  - Set `scenario: 'populated'` to use the realistic demo data
  - Remove the `customAction` example migration
- Also update backend/seed-data.json with the flat (non-scenario) populated data for direct use
  $ pnpm prisma migrate reset --force (or equivalent seed command)
- Verify seed data loaded correctly by querying a few records
- Verify dev user (mock auth sub: "test") has appropriate org/workspace memberships
  Commit: `foundation: replace faker seed data with concept populated scenario`

STEP 7: Create Storybook foundation

- Configure Storybook theme with brand tokens (background, fonts, colors)
- Create theme decorator wrapping all stories with brand CSS variables
- Set up viewport presets matching shell spec's responsive breakpoints
  $ pnpm run storybook:start
  Commit: `foundation: configure Storybook with brand theme`

STEP 8: Update generated example tests

- Update e2e/specs/example.spec.ts to match new foundation content
- Replace any snapshot tests with assertions that verify the presence of key elements
- Replace default greeting assertions with actual dashboard content
- Verify theme toggle works with custom brand tokens
- Verify navigation links from custom shell
  Commit: `foundation: update example E2E tests to match custom shell`

STEP 9: Verify and checkpoint
$ pnpm run build
$ pnpm run dev
IF agent-browser is available - Navigate to app, verify: login page, app shell with sidebar/header, theme toggle, nav links - Take screenshots → \_implementation/verification/screenshots/foundation/

- Update \_implementation/progress.json: set foundation phase to approved
- Update \_implementation/PLANS.md: check off foundation tasks
  EMIT [implement-1-setup-2-foundation] completed run_id=<uuid> phase=foundation

CHECKPOINT foundation_approval

> "Your app now has your brand's look and feel — colors, fonts, and layout are applied.
> [Show screenshots if available from _implementation/verification/screenshots/foundation/]
>
> Approve, or tell me what to change."

MUST consume brand tokens from \_concept/1_discovery/3_brand/tokens.json — never invent colors
MUST use @postxl/ui-components for all UI primitives
MUST implement both light and dark modes if tokens specify both
MUST verify with agent-browser before requesting approval (when available)
MUST one commit per sub-phase (brand, auth, shell, services, seed, storybook, tests)
NEVER invent colors, fonts, or spacing not in tokens.json
NEVER use raw HTML elements when a library component exists
NEVER skip auth configuration (even if using mock mode for dev)
NEVER hardcode navigation items — derive from shell spec
NEVER modify \_concept/ files

CHECKLIST

- [ ] Brand tokens applied to theme-default.css (HSL format, light + dark)
- [ ] Custom fonts imported and referenced in CSS custom properties
- [ ] Auth configuration verified (roles, mock mode)
- [ ] AppSidebarNavigation shared at layout level (not per-page)
- [ ] Header updated with project branding
- [ ] Error boundary, toasts, and nav guards verified
- [ ] Seed data loads correctly
- [ ] Storybook configured with brand theme decorator
- [ ] Example E2E tests updated for custom shell
- [ ] Build passes (Level 1 verification)
- [ ] Visual verification screenshots saved (if agent-browser available)
