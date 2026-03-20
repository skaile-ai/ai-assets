---
name: concept-behavior
description: "DEPRECATED — Behavioral specifications are now captured as EARS acceptance criteria in stories.json (produced by concept-2-experience-1-journeys, step 4). This skill is no longer part of the active pipeline."
keywords: deprecated, behavior, allium
deprecated: true
replaced_by: concept-2-experience-1-journeys
---

DEPRECATED

This skill has been replaced by `concept-2-experience-1-journeys` (step 4).

Behavioral specifications (state machines, transition rules, guard conditions)
are now captured as EARS acceptance criteria within `_concept/2_experience/1_journeys/stories.json`.

- Event-driven EARS criteria replace Allium rules
- State-driven EARS criteria replace Allium state machines
- Story personas replace Allium surface `facing` clauses
- Story downstream links replace Allium entity definitions

If you need behavioral formalization, run `concept-2-experience-1-journeys` instead.
