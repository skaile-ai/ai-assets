---
implements:
  - 03_features/01_user_auth/login.md
data_entities: [user]
layout: 07_screens/00_layout/shell.md
last_updated: 2026-03-11
---

# Screen: Login

## Purpose
Let users sign in to access their personal and team workspace.

## What the User Sees
- The app logo and name
- A sign-in form asking for email and password
- A "remember me" option
- Links to reset a forgotten password or create a new account

## Information Displayed
- **User entity:** email (entered by user)

## Actions
- **Sign in:** enter email and password, then submit to access the dashboard
- **Forgot password:** navigate to password recovery
- **Create account:** navigate to registration

## Situations
- **First visit:** empty form, ready to fill in
- **Signing in:** form is temporarily disabled while checking credentials
- **Wrong credentials:** a message explains what went wrong, form is ready to retry
- **Success:** user is taken to the dashboard

## Entities Involved
- **User** — the person signing in (email, password for authentication)
