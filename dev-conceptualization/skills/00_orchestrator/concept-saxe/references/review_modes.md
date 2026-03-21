# Review Modes

The orchestrator supports two approval modes. The user can switch at any time.

## Default Mode (human review)

After each phase, the orchestrator pauses and waits for explicit human approval
before continuing. This is the standard flow.

## Auto-review Mode

Activated when the user says "auto-review", "autonomous", or "run without stopping".

Flow after each phase:

1. Execute phase
2. Run lint: `python3 scripts/lint_concept.py _concept`
3. Run `concept-review --garden` (auto-fix safe issues)
4. Read the quality score from `_concept/quality.json`
5. If score >= 70 AND 0 critical/high issues:
   - Auto-approve
   - Log in PLANS.md: `- [x] <step> — auto-approved YYYY-MM-DD (score: NN, 0 blocking issues)`
   - Continue to next phase
6. If score < 70 OR any critical/high issues:
   - Show the health report to the user
   - Log in PLANS.md: `- [ ] <step> — auto-review escalated YYYY-MM-DD (score: NN, N high issues)`
   - Ask: "Auto-review found issues. Please review and approve or tell me what to fix."
   - Wait for human response

## Switching Modes

- "stop auto-reviewing" → revert to default mode
- "auto-review from here" → switch to auto-review mode

The human can switch at any checkpoint.
