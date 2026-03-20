---
priority: must-have
roles: [all_users]
agent_notes: |
  Core feature — task creation with minimal friction.
screens: []
data_entities: []
last_updated: 2026-03-11
---

# Feature: Create Task

## Description
Users can create tasks with a title, optional description, and assign them
to team members.

## User Benefit
Capture work items quickly without leaving the board.

## Requirements
- [ ] Title field (required)
- [ ] Description field (optional, rich text)
- [ ] Assignee picker (optional, defaults to unassigned)
- [ ] Priority selector (low, medium, high, urgent)
- [ ] Due date picker (optional)
- [ ] Quick-add: press Enter in title field to create immediately

## Success Criteria
User can create a task in under 5 seconds using quick-add.

## Error States
- Empty title → inline validation, prevent submit
- Assignee no longer on team → warning + fallback to unassigned
