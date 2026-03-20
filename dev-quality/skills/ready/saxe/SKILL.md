---
name: app-ready
description: "Pre-flight readiness check before E2E testing. Verifies that each feature has a concept doc, screen spec, data model entry, brand tokens, and mockup. Blocks testing of incomplete features."
keywords: readiness, preflight, checklist, testing, validation
---

ROLE  Readiness Gate — verifies features are ready for E2E testing, surfaces gaps.

READS
  _concept/2_experience/2_features/**/*.md             — feature list and status frontmatter
  _concept/2_experience/3_screens/**/*.md              — screen specs (implements: field)
  _concept/3_blueprint/3_datamodel/postxl-schema.json — data model
  _concept/3_blueprint/3_datamodel/feature_map.json   — model-to-feature mapping
  _concept/1_discovery/3_brand/tokens.json            — brand tokens existence
  _concept/3_blueprint/1_techstack/stack.md           — tech stack existence
  ? _concept/05_mockups/**/*.html          — mockup files

WRITES
  (none — read-only audit skill, output is the report shown to user)

REFERENCES
  shared/contracts/concept_structure.md          — expected _concept/ paths
  references/report_templates.md        — readiness table, fix templates, check details

STEP 1: Discover features
  - Read all files in _concept/2_experience/2_features/**/*.md
  - Build feature list from discovered files (feature name + group)
  IF no feature files found
    - Stop with: "No features found in `_concept/2_experience/2_features/`. Run `concept-2-experience-2-features` first."

STEP 2: Check global prerequisites
  - Verify _concept/1_discovery/3_brand/tokens.json exists
  - Verify _concept/3_blueprint/1_techstack/stack.md exists
  - Record global status (brand tokens ✓/✗, tech stack ✓/✗)

STEP 3: Check each feature
  For each feature, verify all of:
  - Concept doc exists: _concept/2_experience/2_features/<group>/<feature>.md
  - Screen spec: at least one .md in _concept/2_experience/3_screens/ has this feature in `implements:`
  - Data model: feature listed in feature_map.json for at least one model
  - Mockup: at least one .html in _concept/05_mockups/ linked from feature or screen
  - Status: frontmatter has `status: implemented` or `status: mockup_ready`
  A feature is "ready" only when ALL checks pass.

STEP 4: Print readiness report
  - Print readiness table (see references/report_templates.md)
  - Print global prerequisite status line
  - For each NOT-ready feature, list missing items with remediation command
  - Print verdict: "X of Y features are ready for E2E testing"

STEP 5: Verdict message
  IF all features ready
    > "All features ready. Run `app-e2e` with confidence."
  ELSE IF some features ready
    > "Partial readiness. Run `app-e2e` only for ready features, or fix gaps first."
  ELSE
    > "No features ready for E2E testing. Fix gaps above first."

MUST  check every feature — never skip or sample
MUST  show remediation command for each missing item
NEVER  modify any _concept/ files — this is a read-only audit
NEVER  report a feature as ready when any check fails

EMIT  [app-ready] started run_id=<uuid>
EMIT  [app-ready] completed ready=<N> total=<M>
