# Procedures — implement skill

## PROCEDURE log_learnings
  - After each checkpoint, reflect on what happened
  - Append to LEARNINGS.md under the most relevant category
  - Only log genuine observations, not status updates

## PROCEDURE update_progress
  - Check off completed phase/feature in PLANS.md
  - Update progress.json with new status and timestamp
  - Commit: "chore: update implementation progress"

## PROCEDURE eval_feature_gate(feature_group, app_url)
  DISPATCH eval-feature as a FRESH sub-agent (new context — not this agent)
    Inputs: feature_group=<group-name>, app_url=<app_url>
  WAIT for eval-feature to write _implementation/eval-feature/{group}.json

  READ _implementation/eval-feature/{group}.json

  IF verdict = "approved"
    - Update progress.json: group approved
    - RETURN approved

  IF verdict = "needs_revision"
    - Pass revision_instructions to implement-feature sub-agent
    - Re-run implement-feature for this group with the instructions
    - revision_cycle += 1
    - IF revision_cycle >= 3: ESCALATE to user
    - ELSE: CALL eval_feature_gate again (loop)
    - RETURN needs_revision

  IF verdict = "escalate"
    - Show eval-feature findings to user
    - PAUSE for user guidance
    - Resume based on user instruction
    - RETURN escalate
