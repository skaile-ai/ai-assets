# concept CLI

## Trigger

Invoke with: "Conceptualize an app" or "Start a new concept" or `/concept [description]`

## Modes

- **Default:** pauses for human approval at each checkpoint
- **Auto-review:** (`--auto-review`) runs lint + gardening between phases, auto-approves when score >= 70

## Workflow

```
Phase 0: Initialize PLANS.md (or resume from existing)
Phase 1: Project brief         → _concept/1_discovery/1_overview/
Phase 2: Research (optional)   → _concept/1_discovery/2_research/
Phase 3: Features              → _concept/2_experience/2_features/
Phase 4: Parallel tracks:
  4a: Brand                    → _concept/1_discovery/3_brand/     (concept-1-discovery-3-brand)
  4b: Tech stack               → _concept/3_blueprint/1_techstack/  (concept-3-blueprint-1-techstack)
  4c: Data model               → _concept/3_blueprint/3_datamodel/  (concept-3-blueprint-3-datamodel)
Phase 5: Screens               → _concept/2_experience/3_screens/    (concept-2-experience-3-screens)
Phase 6: Generate implementation plan in PLANS.md
```

## Output

- `PLANS.md` — concept progress + implementation plan
- `_concept/.snapshots/` — approval snapshots at each checkpoint
- All `_concept/1_discovery/1_overview/` through `_concept/2_experience/3_screens/` artifacts

## Next Steps

After concept complete → `app-design`, `implement-1-setup-1-scaffold`, `app-audit`, `app-e2e`
