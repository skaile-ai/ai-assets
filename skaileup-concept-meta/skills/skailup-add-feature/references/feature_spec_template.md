# Feature Spec Template

## New Feature Frontmatter

Matches the frontmatter written by the `features` skill.
No `status` field — removed globally.

```yaml
---
priority: must-have
story_refs: [<story-id-1>, <story-id-2>]
roles: [all_users]
permissions:
  all_users: [read, create, update]
screens: []
data_entities: []
agent_notes: |
  Added via add-feature. <brief context about why this was added>
last_updated: YYYY-MM-DD
---

# Feature: <Name>

## Description
<What this feature does>

## User Benefit
<Why a user needs this feature, linked to a story outcome if applicable>

## Requirements
- <specific requirement>
- <specific requirement>

## Success Criteria
- <measurable success criterion>
- <measurable success criterion>

## Error States
- <error condition>: <expected system behavior>
```

Notes:
- `story_refs` links to story IDs in stories.json (use IDs, not titles)
- `permissions` is a role-action matrix (if no permissions needed, omit the section)
- `agent_notes` is optional but recommended for cascade context
- `screens: []` starts empty — populated during cascade Step 5
- `data_entities: []` starts empty — populated during cascade Step 4

---

## Discovery Questions

### For a new feature:

| # | Question |
|---|----------|
| 1 | What should the user be able to do? (plain language outcome) |
| 2 | What happens when it works? When it fails? |
| 3 | Who uses this — all users, or a specific role? |
| 4 | Must-have or nice-to-have? |
| 5 | Does it belong in an existing feature group, or is it a new group? |
| 6 | Are there any existing stories in stories.json that relate to this? |

### For modifying an existing feature:

| # | Question |
|---|----------|
| 1 | Which feature are you changing? (confirm by showing the current spec) |
| 2 | What's changing — requirements, roles, priority, or scope? |
| 3 | Are there new data entities or screens needed? |
| 4 | Does this change affect other features that depend on this one? |

---

## Impact Assessment Template

Present this before writing the feature spec:

```
Impact Assessment: <Feature Name>

Type: New feature | Modification
Group: <NN_group_name> (existing | new)
Priority: must-have | nice-to-have

Downstream impact:
- Journeys: [new story in <stage> | update downstream links on stories: X, Y | no change]
- Tech stack: [new integration: X | no change]
- Architecture: [new module: X | new external integration: Y | no change]
- Data model: [new entities: X | new fields on: Y | new relation: X → Y | no change]
- Screens: [new screen: X | update existing: Y | no change]
- Seed data: [update populated scenario | no change]

Files to create: <list>
Files to modify: <list>
```

---

## Cascade Summary Template

Present at the end of Step 5:

```
Cascade Summary: <Feature Name>

Feature: <name> (new | modified)
Group: <NN_group>/<feature>.md

Journeys: <updated stories.json — N stories added> | no changes | not present
Tech stack: <added integration: X> | no changes | not present
Architecture: <updated sections: X, Y> | no changes | not present
Data model: <N new entities, M fields added> | no changes | not present
Seed data: <updated populated scenario + edge_cases> | no changes | not present
Feature map: <N entities mapped> | no changes | not present
Screens: <N new screens, M modified screens> | no changes | not present

Feedback loop:
- Feature data_entities: [<list>]
- Feature screens: [<list>]
- Screen implements: updated on [<list of screens>]
```
