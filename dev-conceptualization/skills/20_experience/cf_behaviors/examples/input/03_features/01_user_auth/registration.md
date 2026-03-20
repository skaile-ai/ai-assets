---
priority: must-have
roles: [all_users]
agent_notes: |
  Simple registration for team members.
screens: []
data_entities: []
last_updated: 2026-03-11
---

# Feature: Registration

## Description
New users can create an account to join their team.

## User Benefit
Quick onboarding — set up in under a minute.

## Requirements
- [ ] Email + password registration form
- [ ] Password strength indicator
- [ ] Email verification
- [ ] Auto-redirect to dashboard after verification

## Success Criteria
User can register and reach their dashboard within 2 minutes.

## Error States
- Email already taken → inline error with "sign in" link
- Weak password → strength indicator + requirements tooltip
- Verification link expired → resend option
