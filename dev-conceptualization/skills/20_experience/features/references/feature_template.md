# Feature File Template

## Output path

`_concept/2_experience/2_features/<NN_group>/<feature>.md`

## Frontmatter

```yaml
---
priority: must-have
story_refs: [story-001, story-002]
roles: [all_users]
permissions:
  all_users: [view, create, edit, delete]
agent_notes: |
  Context from user conversation.
screens: []
data_entities: []
last_updated: YYYY-MM-DD
---
```

**Notes:**
- `story_refs` — IDs from stories.json that this feature derives from. Required.
- `screens[]` — populated by the **screens** skill via the feedback loop. Leave empty.
- `data_entities[]` — populated by the **datamodel** skill via the feedback loop. Leave empty.

## Body sections

```markdown
# Feature: <Name>

## Description
What does this feature do?

## User Benefit
Why is this valuable to the user?

## Requirements
- [ ] Requirement 1
- [ ] Requirement 2

## Success Criteria
What proves this feature works?

## Error States
What happens when things go wrong?

## Permissions

| Action | admin | member | viewer |
|--------|-------|--------|--------|
| View   | yes   | yes    | yes    |
| Create | yes   | yes    | no     |
| Edit   | yes   | own    | no     |
| Delete | yes   | no     | no     |
```

## Feature identification questions

For each feature, answer:

| # | Question |
|---|----------|
| 1 | What should the user be able to do? |
| 2 | What happens when it works? When it fails? |
| 3 | Who uses this — everyone, or a specific role? |
| 4 | Must-have for launch, or nice-to-have? |
| 5 | Which stories in stories.json does this trace to? |

## Scope guidance

Focus feature specs on **custom business logic and non-standard flows**. Most
frameworks and platforms provide standard CRUD operations, list/detail views,
and admin UIs automatically — avoid speccing what your stack handles for free.

Ask: "Does this feature require custom logic, special UI treatment, or domain-specific
rules?" If yes, it belongs here. If it is a standard list or form, note it briefly
and move on.

## Summary table format

Present features in a summary table for user review:

```
| # | Feature | Group | Priority | Roles |
|---|---------|-------|----------|-------|
| 1 | Login | 01_user_auth | must-have | all_users |
| 2 | Registration | 01_user_auth | must-have | all_users |
| 3 | Dashboard | 02_dashboard | must-have | all_users |
```
