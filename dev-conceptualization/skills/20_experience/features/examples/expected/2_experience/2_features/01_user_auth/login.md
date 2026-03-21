---
priority: must-have
story_refs: [story-001, story-002]
roles: [all_users]
permissions:
  all_users: [view, create]
agent_notes: |
  Simple login for small teams.
  Social login or magic link could reduce friction.
screens: []
data_entities: []
last_updated: 2026-03-11
---

# Feature: Login

## Description
Users can sign in to access their team's task board.

## User Benefit
Secure access to personal and team tasks.

## Requirements
- [ ] Email + password login form
- [ ] Error message on invalid credentials
- [ ] Redirect to dashboard on success
- [ ] "Remember me" option

## Success Criteria
User can sign in and see their dashboard within 3 seconds.

## Error States
- Invalid credentials → inline error, no page reload
- Account locked → message with support contact
- Server error → generic retry message

## Permissions

| Action | all_users |
|--------|-----------|
| View   | yes       |
| Create | yes       |
