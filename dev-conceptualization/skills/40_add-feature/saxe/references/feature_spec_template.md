# Feature Spec Template

## New Feature Frontmatter

```yaml
---
status: draft
priority: must-have
roles: [all_users]
agent_notes: |
  Added via concept-add-feature. Context from conversation.
screens: []
data_entities: []
last_updated: YYYY-MM-DD
---

# Feature: <Name>

## Description
## User Benefit
## Requirements
- [ ] Requirement 1
## Success Criteria
## Error States
```

## Discovery Questions

### For a new feature:

| # | Question |
|---|----------|
| 1 | What should the user be able to do? |
| 2 | What happens when it works? When it fails? |
| 3 | Who uses this — everyone, or a specific role? |
| 4 | Must-have or nice-to-have? |
| 5 | Does it belong in an existing feature group, or is it a new group? |

### For modifying an existing feature:

| # | Question |
|---|----------|
| 1 | Which feature are you changing? (confirm by showing current spec) |
| 2 | What's changing — requirements, roles, priority, or scope? |
| 3 | Are there new data entities or screens needed? |

## Impact Assessment Template

```
Impact Assessment:
- Feature: <new or modified feature name>
- Group: <NN_group> (existing or new)
- Data model changes: <new models, new fields, modified enums>
- Architecture changes: <new modules, integrations, protocols>
- Screen changes: <new screens, modified screens>
- Tech stack changes: <new integrations>
- Behavior changes: <new/modified .allium specs>
```

## Cascade Summary Template

```
Cascade Summary:
- Feature: <name> (new | modified)
- Behavior: <updated file> | no changes | not present
- Tech stack: <added integration> | no changes | not present
- Architecture: <updated sections> | no changes | not present
- Data model: <N new models, M modified models> | no changes | not present
- Seed data: <updated scenarios> | no changes | not present
- Screens: <N new screens, M modified screens> | no changes | not present
- Feature cross-refs updated: data_entities, screens
```
