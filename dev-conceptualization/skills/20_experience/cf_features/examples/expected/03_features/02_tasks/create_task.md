---
priority: must-have
roles: [all_users]
agent_notes: |
  This is the hero flow — must be fast and frictionless.
  Inline creation (no modal) preferred.
screens: []
data_entities: []
last_updated: 2026-03-11
---

# Feature: Create Task

## Description
Users can create a new task with a title, optional description,
assignee, and due date.

## User Benefit
Capture work items in seconds without leaving the current view.

## Requirements
- [ ] Title field (required)
- [ ] Description field (optional, supports basic formatting)
- [ ] Assignee picker (team members)
- [ ] Due date picker (optional)
- [ ] Status defaults to "todo"
- [ ] Task appears immediately in the list after creation

## Success Criteria
User can create a task in under 10 seconds with just a title.

## Error States
- Empty title → inline validation, prevent submit
- Network error → retry with draft preserved
