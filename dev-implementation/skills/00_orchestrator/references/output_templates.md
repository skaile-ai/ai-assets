# Output Templates

## Resume Status (Phase 0, existing plan)

```
Resuming implementation of <app-name>.
Current phase: <phase>
Features completed: X/Y
Last activity: <date>
```

## Implementation Plan (Phase 0, new plan)

```
Implementation Plan: <App Name>

Phases:
1. Scaffold — create project structure
2. Startup — verify app runs
3. Foundation — brand tokens, auth, app shell
3.5. Infrastructure — <N> custom modules, <M> processes (if architecture defines them)
4. Features — <N> must-have features across <M> journeys/groups
5. UAT — user acceptance testing
6. Verify — full-stack verification

Infrastructure (if applicable):
  Custom modules: <list>
  Additional processes: <list>
  External integrations: <list>

Feature journeys (hero → vital → hygiene order):
  [Hero]   <journey label>: <feature1>, <feature2>, ...
  [Vital]  <journey label>: <feature1>, <feature2>, ...
  [Vital]  <journey label>: <feature1>, ...
  [Hygiene] <journey label>: <feature1>, ...

  OR (if no stories.json):
  01_<group>: <feature1>, <feature2>, ...
  02_<group>: <feature1>, <feature2>, ...

Expert skills available: <list>

Complexity tier: <small|standard|complex>
Auto-approve features: true

Proceed? (approve / modify scope)
```

## Feature Group / Journey Completion (Phase 4)

```
Journey/group <name> complete.
Features implemented: <list>
All E2E tests: passing

Approve group? (approve / request changes)
```

## Implementation Complete (Phase 6)

```
Implementation complete!

<App Name> has been fully implemented and verified.
Branch: implement/<app-slug>
Features: N/N implemented and approved
E2E tests: N passing
Verification: PASS

Next steps:
- Merge implement/<app-slug> to main
- Deploy to staging environment
- Run app-audit for security review
```

## Observability Events

```
[implement] started run_id=<uuid> app=<app-name> features=<count>
[implement] checkpoint phase=<name> status=approved|pending details=<summary>
[implement] feature_complete feature=<journey>/<feature> tests=<count>
[implement] feature_auto_approved feature=<journey>/<feature>
[implement] uat_complete journeys=N passed=P failed=F
[implement] completed run_id=<uuid> features=<count> e2e_tests=<count>
```
