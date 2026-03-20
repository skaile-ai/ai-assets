---
priority: must-have
roles: [all_users]
agent_notes: |
  Keep registration minimal — name, email, password.
  Team invite flow comes later.
screens: []
data_entities: []
last_updated: 2026-03-11
---

# Feature: Registration

## Description
New users can create an account to join or start a team.

## User Benefit
Quick onboarding — under 60 seconds from landing to first task.

## Requirements
- [ ] Name, email, password fields
- [ ] Password strength indicator
- [ ] Email verification
- [ ] Redirect to empty dashboard after signup

## Success Criteria
User can register and see an empty dashboard in under 60 seconds.

## Error States
- Email already taken → suggest login instead
- Weak password → inline strength feedback
- Verification email not received → resend option
