# Mutation Strategies

Ranked by priority. The improver picks the highest-priority applicable strategy.

| Priority | Strategy | Trigger | Action |
|---|---|---|---|
| 1 | fix_failing_test | Test case with gate=fail | Fix recipe/example to make test pass |
| 2 | incorporate_correction | Unaddressed correction in learnings | Update skill to use corrected pattern |
| 3 | update_pattern | Version drift in references/versions.json | Update outdated API patterns |
| 4 | strengthen_weak_test | Test passes but quality < 70 | Improve recipe clarity or patterns |
| 5 | add_learned_recipe | Approved candidate in learnings/candidates.md | Promote to recipes/ directory |
| 6 | improve_prompt | Low scores across test cases | Clarify SKILL.md instructions |
| 7 | add_missing_example | Recipe without matching atomic test | Add atomic example and test |

## Key Principles

- One change per iteration — never combine strategies
- Always explain the rationale in commit messages
- Prefer targeted fixes over broad rewrites
- Respect guard constraints absolutely
