---
status: draft
priority: must-have
roles: [all_users]
agent_notes: |
  Simple login for small teams.
  Social login or magic link could reduce friction.
screens:
  - path: 2_experience/3_screens/01_user_auth/login.md
    status: draft
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
