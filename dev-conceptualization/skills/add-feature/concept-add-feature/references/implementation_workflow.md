# Implementation Workflow (Phase 4)

Only run this phase if `_implementation/progress.json` exists.

## 4a: Re-generate (if data model changed)

If `postxl-schema.json` was modified during cascade:

1. Copy updated schema to project root
2. Run `pnpm run generate`
3. Resolve conflicts following the cascade strategy from `implement-generate`:
   - Level 1: Auto-resolve generated-only files
   - Level 2: Preserve `@custom-start`/`@custom-end` blocks
   - Level 3: Intelligent merge for ejected files
   - Level 4: Escalate feature-level conflicts
4. Run Prisma migration: `pnpm prisma migrate dev --name add_<feature_slug>`
5. Level 1 verification: `pnpm run build && pnpm run lint && pnpm run test:types`
6. Commit: `generate: sync schema for <feature name>`

## 4b: Implement the feature (TDD)

Follow the `implement-2-features` workflow:

1. **Create feature branch:**
   ```bash
   git checkout implement/<app-slug>
   git checkout -b feat/<group-slug>/<feature-slug>
   ```

2. **Write acceptance criteria** to
   `_implementation/acceptance_criteria/<group>/<feature>.ac.md`
   - One AC per requirement, one per screen state, one per behavioral rule
   - Mark one AC as `snapshot` type
   - Commit: `test: write acceptance criteria for <feature>`

3. **Write E2E tests** to `e2e/specs/<group>/<feature>.spec.ts`
   - Scope locators to `main` (avoid PostXL sidebar matches)
   - All tests must FAIL (no implementation yet)
   - Commit: `test: write E2E tests for <feature>`

4. **Write Storybook stories** for new components
   - Commit: `test: add Storybook stories for <feature>`

5. **Implement frontend** until E2E tests pass
   - Use `@postxl/ui-components`, brand tokens, generated tRPC client
   - Follow screen spec component inventory
   - Commit incrementally: `feat: implement <feature> frontend`

6. **Assess and implement backend** (if needed)
   - Write backend ACs and unit tests first
   - Implement custom logic in `@custom-start` blocks
   - Commit: `feat: implement <feature> backend`

7. **Level 2 verification:**
   - All E2E tests pass for this feature
   - Storybook stories render
   - Build passes
   - agent-browser visual check (if available)
   - Save report: `_implementation/verification/reports/<feature>-report.json`

8. **Update tracking:**
   - Create `_implementation/features/<group>/<feature>.md`
   - Update `_implementation/progress.json` (add new feature entry)
   - Update `_implementation/PLANS.md`

9. **Merge after approval:**
   ```bash
   git checkout implement/<app-slug>
   git merge --squash feat/<group>/<feature>
   git commit -m "feat(<group>): <feature> — <summary>"
   git branch -d feat/<group>/<feature>
   ```

## 4c: Regression check

After merging, verify existing features still work:

1. Run the full E2E suite: `pnpm run e2e`
2. If any existing tests fail:
   - If caused by the new feature: fix the regression before completing
   - If pre-existing: note in `_implementation/decisions.md`, don't block
3. Report regression status:
   ```
   Regression Check:
   - Total E2E tests: N
   - Passing: N
   - Failing: N (0 caused by new feature)
   - New feature tests: N/N passing
   ```

## 4d: Handle modified features (already-implemented)

When modifying a feature that's already implemented (`status: approved` or
`status: tested` in `progress.json`):

1. Diff the old and new feature spec to identify what changed
2. Determine which existing tests need updating:
   - New requirements -> new ACs and E2E tests
   - Changed requirements -> update existing ACs and tests
   - Removed requirements -> remove corresponding tests
3. Create a feature branch: `feat/<group>/<feature>-update`
4. Update ACs, E2E tests, Storybook stories
5. Update implementation code
6. Run Level 2 verification
7. Run regression check (full E2E suite)
8. Merge after approval
