# Iron Laws

These constraints are non-negotiable. No rationalization overrides them.
Skills enforce these via their `requires` field (flow node or SKILL.md frontmatter).
This document explains the WHY behind each gate.

---

## The Laws

### 1. NO CONCEPT WORK WITHOUT A BRIEF

Every conceptualization skill requires `1_discovery/1_overview/brief.md` to exist.

**Why:** Without a brief, all downstream work is speculative and will be discarded.

---

### 2. NO DATA MODEL WITHOUT FEATURES

`3_blueprint/3_datamodel/` requires `2_experience/2_features/` with at least one feature file.

**Why:** Entities derive from features. A model without features is an architecture astronaut exercise.

---

### 3. NO SCREENS WITHOUT BRAND TOKENS

`2_experience/3_screens/` requires `1_discovery/2_brand/tokens.json` to exist,
unless the brand step was explicitly skipped by the user.

**Why:** Screens without brand tokens produce generic specs that need complete rewrites later.

---

### 4. NO SCREENS WITHOUT DATA MODEL

`2_experience/3_screens/` requires `3_blueprint/3_datamodel/model.json`.

**Why:** Screens must reference real entities and seed data for template data sections.

---

### 5. NO MOCKUPS WITHOUT SCREEN SPECS

The `mock` skill requires `2_experience/3_screens/` with at least one screen file.

**Why:** Mockups that don't trace back to screen specs create drift between concept and visual output.

---

### 6. NO IMPLEMENTATION WITHOUT READINESS CHECK

Implementation skills should verify that features, screens, data model, and tech stack all exist —
either via the `ready` skill gate or by checking these paths directly.

**Why:** Partial implementation creates more debt than waiting for a complete concept.

---

### 7. NO ARTIFACT WITHOUT PREREQUISITES

A skill must verify its `requires` paths (file/folder existence) before producing any output.

**Why:** Skipping prerequisites produces artifacts built on missing foundations.

---

### 8. NO OVERWRITING WITHOUT APPROVAL

Never overwrite user-modified files without showing the diff and getting explicit approval.

**Why:** Lost work destroys trust. Show the diff, ask first.

---

### 9. QUESTIONS ARE STANDALONE MESSAGES

When you need to ask the user a question, send it as its own dedicated message — never at the
end of a longer status update or explanation. See `agent_patterns.md` Communication Style for examples.

**Why:** Questions buried in long messages get missed. A standalone question makes it obvious
that user input is needed.

---

## Rationalization Defense

| What agents say | What to do instead |
|---|---|
| "The brief is obvious from context" | Write it anyway. The brief is the contract. |
| "I can infer the data model from the description" | Read the features first. Every entity must trace to a feature. |
| "The user described the screens already" | Structure them with component inventory, states, and seed data references. |
| "This is a simple app, we can skip steps" | Use a lighter flow (e.g. `prototype`). Don't skip gates ad-hoc. |
| "I'll fix the cross-references later" | Fix them now. Broken links compound exponentially. |
| "Testing can wait" | Write the test plan alongside features. Testing is not an afterthought. |
| "I'll just ask at the end of this update" | Send the question as a separate message. Users miss questions buried in long outputs. |
