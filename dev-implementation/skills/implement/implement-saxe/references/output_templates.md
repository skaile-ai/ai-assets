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
1. Scaffold — create PostXL project
2. Startup — verify app runs
3. Foundation — brand, auth, shell
3.5. Infrastructure — <N> custom modules, <M> providers, <K> processes (if architecture specifies)
4. Features — <N> features in <M> groups
5. Re-generate — final generator pass
6. Verify — full-stack verification

Infrastructure (if applicable):
  Custom modules: <list from architecture>
  Additional processes: <list from architecture>
  External integrations: <list from architecture>

Feature groups (priority order):
  01_<group>: <feature1>, <feature2>, ...
  02_<group>: <feature1>, <feature2>, ...
  ...

Proceed? (approve / modify scope)
```

## Feature Group Completion (Phase 4)

```
Feature group <NN_group_name> complete.
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
[implement] feature_complete feature=<group>/<feature> tests=<count> branch=feat/<group>/<feature>
[implement] feature_auto_approved feature=<group>/<feature>
[implement] completed run_id=<uuid> features=<count> e2e_tests=<count> duration=<time>
```
