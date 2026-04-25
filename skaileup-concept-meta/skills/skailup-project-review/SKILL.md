---
name: project-concept-review
description: "Audit a meta-concept for completeness, consistency, and accuracy. Checks that all subsystems are documented, references are valid, maturity levels are honest, and no detail is duplicated from subsystem concepts."
source: MERGED
version: 1.0.0
keywords: [meta-concept, review, audit, consistency, completeness]
user_inputs:
  - key: PROJECT_ROOT
    prompt: "Where is the shell repo root?"
    required: true
reads_from:
  - contracts/meta-concept-contract/CONTRACT.md
  - "{PROJECT_ROOT}/_concept/"
  - "{PROJECT_ROOT}/**/CLAUDE.md"
  - "{PROJECT_ROOT}/**/_concept/"
writes_to: []
---

# Project Concept: Review

Audit an existing meta-concept for quality, completeness, and consistency.

## Prerequisites

- At minimum `_concept/1_discovery/1_overview/brief.md` must exist
- Read the meta-concept contract: `contracts/meta-concept-contract/CONTRACT.md`

## Checklist

### 1. Structure Compliance

- [ ] Directory structure follows the contract (`1_discovery/`, `2_subsystems/`, `3_integration/`, `PLANS.md`)
- [ ] All markdown files have valid YAML frontmatter
- [ ] All frontmatter includes `last_updated` with a valid ISO date
- [ ] No files exist outside the defined structure

### 2. Discovery Completeness

- [ ] `brief.md` has all required frontmatter fields
- [ ] `goals.md` has success criteria, scope, and constraints
- [ ] `comparable.md` has at least 3 comparable products/ecosystems
- [ ] `identity.md` exists (as content or as a valid reference pointer)

### 3. Subsystem Coverage

- [ ] Every directory/submodule in the shell repo is accounted for in `2_subsystems/index.md`
- [ ] Every subsystem in the index has a corresponding `<subsystem>.md` file
- [ ] No subsystem is described that doesn't actually exist in the repo
- [ ] `concept_ref` paths resolve to actual directories
- [ ] Maturity levels are defensible (cross-check with actual code state)

### 4. Integration Accuracy

- [ ] `architecture.md` describes connections that actually exist in `package.json` / submodule config
- [ ] `deployment.md` deployment models match actual infrastructure
- [ ] `shared_contracts.md` lists real packages with real consumers
- [ ] No aspirational connections are presented as existing without being marked as planned

### 5. No Duplication

- [ ] No feature specs in the meta-concept (those belong in subsystem `_concept/`)
- [ ] No screen specs in the meta-concept
- [ ] No data model in the meta-concept
- [ ] No per-subsystem tech stack details beyond the summary in subsystem files
- [ ] Subsystem descriptions are summaries, not copies of `CLAUDE.md` content

### 6. Cross-Reference Integrity

- [ ] All subsystem files are linked from `index.md`
- [ ] All `concept_ref` paths are valid
- [ ] Brand `identity.md` reference (if a pointer) resolves to an existing file
- [ ] PLANS.md subsystem status matches `2_subsystems/*.md` maturity fields

### 7. PLANS.md Quality

- [ ] Subsystem status table is current
- [ ] Cross-product journeys are described (if any exist)
- [ ] Roadmap has at least one future milestone
- [ ] Decisions section captures major architectural choices

## Output

Produce a review report with:

1. **Summary** — pass/fail per checklist section
2. **Findings** — each violation with severity (critical / warning / info)
3. **Recommendations** — concrete actions to fix each finding

Do not auto-fix. Present findings and let the user decide what to address.

## Severity Guide

| Severity | Meaning |
|---|---|
| Critical | Missing required file, broken reference, incorrect subsystem inventory |
| Warning | Stale `last_updated`, missing optional section, inflated maturity |
| Info | Style suggestion, minor improvement opportunity |
