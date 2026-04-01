# Iron Laws

These constraints are non-negotiable. No rationalization overrides them.
Skills read these from `pipeline.json` hard_gates — this document explains the WHY.

## The Laws

### 1. NO CONCEPT WORK WITHOUT A BRIEF
Every conceptualization step requires `01_project/brief.md` to exist.
**Why:** Without a brief, all downstream work is speculative and will be discarded.

### 2. NO DATA MODEL WITHOUT FEATURES
`06_datamodel/` requires `03_features/` with at least one feature file.
**Why:** Entities derive from features. A model without features is an architecture astronaut exercise.

### 3. NO SCREENS WITHOUT BRAND TOKENS
`07_screens/` requires `04_brand/tokens.json` to exist (unless brand step was explicitly skipped by user).
**Why:** Screens without brand tokens produce generic specs that need complete rewrites later.

### 4. NO SCREENS WITHOUT DATA MODEL
`07_screens/` requires `06_datamodel/model.json`.
**Why:** Screens must reference real entities and seed data for template data sections.

### 5. NO MOCKUPS WITHOUT SCREEN SPECS
`cf_concept_mock/` requires `07_screens/` with at least one screen file.
**Why:** Mockups that don't trace back to screen specs create drift between concept and visual output.

### 6. NO IMPLEMENTATION WITHOUT READINESS CHECK
`implement/` should verify `ready/` checklist or at minimum: features, screens, datamodel, techstack all exist.
**Why:** Partial implementation creates more debt than waiting for a complete concept.

### 7. NO ARTIFACT WITHOUT PREREQUISITES
A skill must verify its hard_gates (file existence checks) before producing any output.
**Why:** Skipping prerequisites produces artifacts built on missing foundations.

### 8. NO OVERWRITING WITHOUT APPROVAL
Never overwrite user-modified files without showing the diff and getting explicit approval.
**Why:** Lost work destroys trust. Show the diff, ask first.

### 9. QUESTIONS ARE STANDALONE MESSAGES
When you need to ask the user a question, send it as its own dedicated message — never at the end of a longer status update or explanation. See `agent_patterns.md` Communication Style for examples.
**Why:** Questions buried in long messages get missed. A standalone question makes it obvious that user input is needed.

## Rationalization Defense

| What agents say | What to do instead |
|----------------|-------------------|
| "The brief is obvious from context" | Write it anyway. The brief is the contract. |
| "I can infer the data model from the description" | Read the features first. Every entity must trace to a feature. |
| "The user described the screens already" | Structure them with component inventory, states, and seed data references. |
| "This is a simple app, we can skip steps" | Use complexity presets in pipeline.json. Don't skip ad-hoc. |
| "I'll fix the cross-references later" | Fix them now. Broken links compound exponentially. |
| "Testing can wait" | Write the test plan alongside features. Testing is not an afterthought. |
| "I'll just ask at the end of this update" | Send the question as a separate message. Users miss questions buried in long outputs. |
