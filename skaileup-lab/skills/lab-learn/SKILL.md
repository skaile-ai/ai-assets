---
name: lab-learn
description: Analyze skill usage observations and extract patterns, corrections, and test cases
source: MERGED
version: 1.0.0
keywords: [lab, learn, observations, patterns]
user_inputs: []
reads_from: [data/observations/]
writes_to: [learnings/, tests/cases/]
---

# Lab Learn

Analyze observations from real skill usage and extract actionable learnings.

Process:
1. **Load observations** for the target skill
2. **Identify patterns**:
   - Recurring corrections (old → new patterns, e.g., deprecated API usage)
   - Common errors (same error message appearing across sessions)
   - Frequent tool sequences (patterns that could become recipes)
3. **Apply thresholds** — only promote patterns above configured thresholds
4. **Classify by confidence**: low (2 occurrences), medium (3-4), high (5+)
5. **Generate outputs**:
   - `learnings/corrections.md` — patterns to fix in the skill
   - `learnings/candidates.md` — recipe candidates for human review
   - `tests/cases/learned-*.test.yaml` — new test cases from error patterns

Never auto-promote recipe candidates. Always require human review.
