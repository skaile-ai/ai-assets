# Report Templates

Output templates for audit and gardening modes.

## Audit Mode — Health Report

```
## Structure Audit Report

### Quality Score: <score>/100

| Category | Score | Details |
|----------|-------|---------|
| Structure | <N> | <M> of <T> steps present |
| Frontmatter | <N> | <detail> |
| Golden Principles | <N> | <detail> |
| Cross-references | <N> | <detail> |
| Coverage | <N> | <M> of <T> features have screens |
| Entropy | <N> | <detail> |

### Pipeline Completeness
| Step | Status | Files |
|------|--------|-------|
| 1_discovery/1_overview | <status> | <count> |
| 1_discovery/2_research | <status> | <count> |
| 1_discovery/3_brand | <status> | <count> |
| 2_experience/1_journeys | <status> | <count> |
| 2_experience/2_features | <status> | <count> |
| 2_experience/3_screens | <status> | <count> |
| 3_blueprint/1_techstack | <status> | <count> |
| 3_blueprint/2_architecture | <status> | <count> |
| 3_blueprint/3_datamodel | <status> | <count> |

### Cascade Warnings

| Changed file | Snapshot | Downstream at risk |
|--------------|----------|--------------------|
| <path> | <snapshot_name> (<date>) | <downstream steps> |

Recommended: re-run downstream skills to pick up changes.

### Issues
| # | Severity | Category | Details |
|---|----------|----------|---------|
| 1 | <severity> | <category> | <description> |

### Recommended Actions
1. <action>
2. <action>
```

Status values for pipeline completeness:
- `✓ approved` — all files approved
- `✓ N approved, M draft` — partial progress
- `⚠ partial` — some files present but incomplete
- `— skipped` — folder empty or missing
- `— not started` — folder does not exist

## Gardening Mode — Report

```
## Doc Gardening Report

### Auto-fixed (N changes)
- ✓ <description of fix applied to file>
- ✓ <description of fix applied to file>

### Needs human attention (N issues)
- ⚠ <description of issue requiring human judgment>
- ⚠ <description of issue requiring human judgment>

### Quality Score: <before> → <after> (after fixes)
```

## Observability Events

### Audit mode
```
[concept-review] started mode=audit run_id=<uuid>
[concept-review] audit_pass check=<check_name> files=<N>
[concept-review] audit_warn check=<check_name> file=<path> detail=<msg>
[concept-review] audit_fail check=<check_name> file=<path> detail=<msg>
[concept-review] completed mode=audit run_id=<uuid> score=<N> issues=<N>
```

### Gardening mode
```
[concept-review] started mode=gardening run_id=<uuid>
[concept-review] auto_fix file=<path> action=<description> value=<new_value>
[concept-review] audit_warn check=<check_name> file=<path> detail=<msg>
[concept-review] completed mode=gardening run_id=<uuid> auto_fixes=<N> remaining=<N> score_before=<N> score_after=<N>
```
