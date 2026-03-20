# Git Workflow for Implementation

Branch naming, commit conventions, and merge protocol for the implementation pipeline.

## Branch Strategy

```
main (or default branch)
  └─ implement/<app-slug>          Long-lived implementation branch
       ├─ feat/<group>/<feature>   Short-lived feature branches
       ├─ feat/<group>/<feature>
       └─ ...
```

### Branch Types

| Branch                             | Lifetime              | Created by     | Merged by                        |
| ---------------------------------- | --------------------- | -------------- | -------------------------------- |
| `implement/<app-slug>`             | Entire implementation | `implement-1-setup-1-scaffold` | Manual (after full verification) |
| `feat/<group-slug>/<feature-slug>` | Single feature        | `implement-2-features`  | `implement-2-features` (after approval)   |

### Naming Convention

- `<app-slug>`: lowercase, hyphens, from project brief (e.g., `paxl`, `client-portal`)
- `<group-slug>`: feature group with number prefix, hyphens (e.g., `01-auth-workspace`)
- `<feature-slug>`: feature name, hyphens (e.g., `sso-login`)

## Commit Convention

Format: `<type>: <description>`

| Type         | When                                                   |
| ------------ | ------------------------------------------------------ |
| `scaffold`   | Project scaffolding (implement-1-setup-1-scaffold)                     |
| `generate`   | PostXL code generation output (implement-generate)           |
| `foundation` | Theming, auth, shell, shared services (implement-1-setup-2-foundation) |
| `test`       | Adding or updating tests (E2E, Storybook)              |
| `feat`       | Feature implementation code                            |
| `fix`        | Bug fix during implementation                          |
| `refactor`   | Code restructuring without behavior change             |
| `chore`      | Config, dependency updates, CI                         |

Examples:

```
scaffold: initialize PostXL project from concept schema
generate: run PostXL generators (20 models, 7 standard)
foundation: apply brand tokens as CSS custom properties
foundation: implement app shell layout with sidebar navigation
test: write ACs and E2E tests for SSO login
feat: implement SSO login with Keycloak realm routing
fix: correct redirect URL after successful login
```

## Merge Protocol

### Feature Branch → Implementation Branch

After user approves a feature:

1. Ensure all tests pass on feature branch
2. Squash merge to `implement/<app-slug>`
3. Merge commit message: `feat(<group>): <feature-name> — <one-line summary>`
4. Delete feature branch after merge
5. Update `_implementation/progress.json`

```bash
git checkout implement/<app-slug>
git merge --squash feat/<group>/<feature>
git commit -m "feat(<group>): <feature> — <summary>"
git branch -d feat/<group>/<feature>
```

### Implementation Branch → Main

After full verification (implement-3-verify) and user approval:

1. Create PR from `implement/<app-slug>` to `main`
2. PR description references `_implementation/PLANS.md` and verification report
3. Merge strategy: merge commit (preserve implementation history)

## When to Commit

| Event                     | Commit?                 | Branch                   |
| ------------------------- | ----------------------- | ------------------------ |
| Project scaffold complete | Yes                     | `implement/<app>`        |
| Generator output          | Yes                     | `implement/<app>`        |
| Foundation phase complete | Yes (one per sub-phase) | `implement/<app>`        |
| AC + E2E tests written    | Yes                     | `feat/<group>/<feature>` |
| Storybook stories written | Yes                     | `feat/<group>/<feature>` |
| Feature implementation    | Yes (can be multiple)   | `feat/<group>/<feature>` |
| Feature approved          | Squash merge            | `implement/<app>`        |
| Verification passed       | Yes (report)            | `implement/<app>`        |

## Conflict Handling

When merging feature branches back to the implementation branch:

1. **Auto-resolve:** If conflicts are in generated files (identifiable via `postxl-lock.json`),
   re-run `implement-generate` to regenerate and resolve.
2. **Self-resolve:** If conflicts are in custom code, analyze both sides and merge
   intelligently. Prefer the feature branch's changes for feature-specific code.
3. **Escalate:** Only escalate to user if the conflict represents a genuine feature-level
   design decision (e.g., two features modifying the same component differently).
   In this case, suggest refining the concept rather than asking the user to resolve code.

## Initial Repository Setup

`implement-1-setup-1-scaffold` initializes git with:

```bash
git init
git checkout -b implement/<app-slug>
# ... scaffold project ...
git add -A
git commit -m "scaffold: initialize PostXL project from concept schema"
```
