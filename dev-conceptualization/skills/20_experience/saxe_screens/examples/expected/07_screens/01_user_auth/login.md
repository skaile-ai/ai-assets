---
status: draft
implements:
  - 2_experience/2_features/01_user_auth/login.md
data_entities: [User]
layout: 2_experience/3_screens/00_layout/shell.md
last_updated: 2026-03-11
---

# Screen: Login

## Purpose (3-second test)
User sees a login form and can sign in immediately.

## Route
/login

## Component Inventory (top to bottom)
1. Logo — brand mark, centered
2. Heading — "Sign in to TaskFlow"
3. Email input — type="email", required
4. Password input — type="password", required
5. "Remember me" checkbox
6. Submit button — "Sign in", primary color (#6366f1)
7. "Forgot password?" link — text_muted (#64748b)
8. "Create account" link — secondary action

## Data Requirements
- User entity: email, password (for validation)

## User Actions
- Fill email + password → submit → redirect to /dashboard
- Click "Forgot password?" → navigate to /password-reset
- Click "Create account" → navigate to /register

## States
- **Default:** empty form, submit button enabled
- **Loading:** submit button shows spinner, inputs disabled
- **Error:** inline validation under fields, toast for auth failure
- **Success:** redirect to /dashboard

## Template Data
```json
{
  "user": {
    "email": "maria.schmidt@example.com",
    "password": "••••••••"
  }
}
```
