# Feature File Template

## Output path

`_concept/2_experience/2_features/<NN_group>/<feature>.md`

## Frontmatter

```yaml
---
status: draft
priority: must-have
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

## PostXL context

PostXL provides standard models out of the box: `User`, `Action`,
`ActionOperation`, `Comment`, `File`, `TableView`, `Config`. It also generates
standard CRUD views, list/detail pages, and admin UI automatically.

Focus feature specs on **custom business logic and non-standard flows** — don't
specify basic CRUD that PostXL handles for free.

## Summary table format

Present features in a summary table for user review:

```
| # | Feature | Group | Priority | Roles |
|---|---------|-------|----------|-------|
| 1 | Login | 01_user_auth | must-have | all_users |
| 2 | Registration | 01_user_auth | must-have | all_users |
| 3 | Dashboard | 02_dashboard | must-have | all_users |
```
