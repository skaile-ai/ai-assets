# Concept Snapshots

Snapshots capture the state of `_concept/` at approval checkpoints.
They enable diffing, rollback reasoning, and cascade detection when
upstream files change after downstream steps have been built on them.

## Location

```
_concept/.snapshots/
├── manifest.json                   ← index of all snapshots
├── 1_discovery_1_overview_approved/            ← snapshot after step 1 approval
│   └── 1_discovery/1_overview/
│       ├── brief.md
│       ├── goals.md
│       └── comparable.md
├── 2_experience_2_features_approved/           ← snapshot after step 3 approval
│   └── 2_experience/2_features/
│       └── ...
├── 3_blueprint_3_datamodel_approved/
│   └── 3_blueprint/3_datamodel/
│       └── ...
└── full_concept_approved/          ← snapshot after all steps done
    ├── 1_discovery/1_overview/
    ├── 2_experience/2_features/
    ├── ...
    └── 2_experience/3_screens/
```

## manifest.json

```json
{
  "snapshots": [
    {
      "id": "1_discovery_1_overview_approved",
      "step": "1_discovery/1_overview",
      "timestamp": "2026-03-11T14:30:00Z",
      "trigger": "user_approval",
      "files": [
        "1_discovery/1_overview/brief.md",
        "1_discovery/1_overview/goals.md",
        "1_discovery/1_overview/comparable.md"
      ],
      "validation": {
        "skill": "concept-1-discovery-1-overview",
        "verdict": "pass",
        "rules_checked": 6,
        "violations": 0
      }
    },
    {
      "id": "2_experience_2_features_approved",
      "step": "2_experience/2_features",
      "timestamp": "2026-03-11T15:10:00Z",
      "trigger": "user_approval",
      "files": [
        "2_experience/2_features/01_user_auth/login.md",
        "2_experience/2_features/01_user_auth/registration.md",
        "2_experience/2_features/02_dashboard/overview.md"
      ],
      "validation": {
        "skill": "concept-2-experience-2-features",
        "verdict": "pass",
        "rules_checked": 8,
        "violations": 0
      }
    }
  ]
}
```

### Validation field

Every snapshot entry MUST include a `validation` object recording the result of
running `validate_skill_rules.py` against the sub-skill that produced the artifacts:

| Field | Type | Description |
|-------|------|-------------|
| `skill` | string | The sub-skill name that was validated (e.g. `concept-2-experience-1-journeys`) |
| `verdict` | `"pass"` \| `"fail"` \| `"error"` \| `"skipped"` | Validation outcome |
| `rules_checked` | number | Number of MUST/NEVER rules checked (omit if skipped) |
| `violations` | number \| array | 0 for pass, or array of violation strings for fail |
| `reason` | string | Only present for `error` or `skipped` verdicts |

The orchestrator runs validation **before** asking the user for approval. If the
verdict is `fail`, violations must be fixed and validation re-run before proceeding.

## When to Snapshot

Skills create snapshots at **approval checkpoints** — after the user
explicitly approves a step's output.

| Event                  | Snapshot ID             | What's captured |
| ---------------------- | ----------------------- | --------------- |
| Project brief approved | `1_discovery_1_overview_approved`   | `1_discovery/1_overview/`   |
| Features approved      | `2_experience_2_features_approved`  | `2_experience/2_features/`  |
| Brand approved         | `1_discovery_3_brand_approved`     | `1_discovery/3_brand/`     |
| Tech stack approved    | `3_blueprint_1_techstack_approved` | `3_blueprint/1_techstack/` |
| Data model approved    | `3_blueprint_3_datamodel_approved` | `3_blueprint/3_datamodel/` |
| Screens approved       | `2_experience_3_screens_approved`   | `2_experience/3_screens/`   |
| Full concept complete  | `full_concept_approved` | everything      |

## How to Create a Snapshot

1. Create the snapshot folder: `_concept/.snapshots/<snapshot_id>/`
2. Copy the relevant files (preserve folder structure)
3. Add entry to `manifest.json`

```bash
mkdir -p _concept/.snapshots/2_experience_2_features_approved/2_experience/2_features
cp -r _concept/2_experience/2_features/* _concept/.snapshots/2_experience_2_features_approved/2_experience/2_features/
```

## How to Diff

When a file changes after its snapshot was taken, compare:

```bash
diff _concept/.snapshots/2_experience_2_features_approved/2_experience/2_features/01_user_auth/login.md \
     _concept/2_experience/2_features/01_user_auth/login.md
```

Or for an agent: read both versions and compare frontmatter + body.

## Cascade Detection

When an upstream file changes after downstream steps depend on it,
the system should detect and flag the impact.

### Change → Impact Map

| Changed file                      | Downstream impacts                                            |
| --------------------------------- | ------------------------------------------------------------- |
| `1_discovery/1_overview/brief.md`             | Features may need re-scoping, brand mood may shift            |
| `2_experience/2_features/*.md`                | Data model entities may need updating, screens may be invalid |
| `1_discovery/3_brand/tokens.json`            | All mockups need regenerating, screen color refs outdated     |
| `3_blueprint/1_techstack/stack.md`           | Data model translation changes, bootstrap config outdated     |
| `3_blueprint/3_datamodel/postxl-schema.json` | Screen data requirements may be wrong, seed data outdated     |

### Detection Protocol

When `concept-review` runs (audit or gardening mode):

1. For each snapshot in manifest.json, compare current file to snapshot
2. If a file has changed since its snapshot:
   - Read `shared/contracts/pipeline.json` to find all steps that depend on this step
   - Check if those downstream steps have already been completed
   - If yes → flag as **CASCADE WARNING**

```
## Cascade Warnings

| Changed file | Since | Downstream at risk |
|--------------|-------|--------------------|
| 2_experience/2_features/01_user_auth/login.md | 2_experience_2_features_approved (Mar 11) | 3_blueprint/3_datamodel, 2_experience/3_screens |
| 1_discovery/3_brand/tokens.json | 1_discovery_3_brand_approved (Mar 11) | 2_experience/3_screens, mockups |

Recommended: re-run `concept-3-blueprint-3-datamodel` and `concept-2-experience-3-screens` to pick up changes.
Or run `concept-review --garden` to check if changes are cosmetic.
```

### Severity Levels

| Change type                                  | Severity | Action                                 |
| -------------------------------------------- | -------- | -------------------------------------- |
| Frontmatter field added/removed              | HIGH     | Downstream skills must re-run          |
| Body text edited (description, requirements) | MEDIUM   | Review downstream, may not need re-run |
| Only `last_updated` or `status` changed      | LOW      | Informational, no cascade              |
| New file added to a step folder              | MEDIUM   | Downstream may need to incorporate it  |
| File deleted from a step folder              | HIGH     | Cross-references will break            |

## Rules

- Snapshots are read-only — never modify files inside `.snapshots/`
- Creating a new snapshot for a step overwrites the previous one for that step
- `full_concept_approved` captures everything — used as the baseline for implementation
- `.snapshots/` can be gitignored if the team prefers (snapshots are reconstructable from git history)
- Snapshot diffing is optional — the system works without it, it just loses cascade detection
