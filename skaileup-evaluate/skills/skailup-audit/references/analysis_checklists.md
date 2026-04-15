# Audit Analysis Checklists

## Sub-agent 1: Logic & Runtime Errors

- Incorrect conditionals, off-by-one errors
- Missing null/undefined checks
- Race conditions in async code
- Unhandled promise rejections
- Missing error boundaries
- Incorrect type assumptions

## Sub-agent 2: UI/UX & Accessibility

- Forms missing error/loading states
- Broken responsive layouts
- Missing ARIA attributes
- Poor contrast ratios (WCAG AA)
- Missing focus states
- Missing empty states

## Sub-agent 3: Security & Data Integrity

- SQL/NoSQL injection risks
- XSS vulnerabilities
- Missing auth checks on protected routes
- Exposed secrets (hardcoded keys)
- Missing input validation at API boundaries
- CSRF vulnerabilities

## Structure Integrity Checks

Only when `_concept/` exists. Subset of `review` (mechanical checks only).

- Cross-reference integrity (features <-> screens)
- Orphaned files
- Frontmatter compliance
- Stale files (last_updated > 30 days)

## Report Template

```markdown
## Audit Report

### Critical (fix before shipping)
- [Description] — [file:line] — [category]

### High (fix before E2E testing)
- [Description] — [file:line] — [category]

### Medium (fix before launch)
- [Description] — [file:line] — [category]

### Low (nice to fix)
- [Description] — [file:line] — [category]

### Structure Integrity
- Cross-references: N valid, N broken
- Stale files: N
- Frontmatter compliance: N%

### Summary
Code issues: N (C critical, H high, M medium, L low)
Structure issues: N
```
