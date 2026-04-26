# Report Templates

Output templates for audit and gardening modes.

## Audit Mode — Health Report

```
## Structure Audit Report

### Quality Score: <score>/100

| Category | Score | Details |
|----------|-------|---------|
| Structure | <N> | <M> of <T> pipeline steps present |
| Frontmatter | <N> | <M> of <T> files compliant |
| Golden Principles | <N> | <M> of <T> rules passing |
| Cross-references | <N> | <M> of <T> links valid |
| Coverage | <N> | <M> of <T> features have screens + data |
| Entropy | <N> | <detail> |

**Overall: <score>/100** — <pass/warn/block>

> Score < 70 blocks proceeding to new pipeline steps.

---

### Pipeline Completeness

| Phase | Folder | Status | Files |
|-------|--------|--------|-------|
| Overview | `discovery/` | <status> | <count> |
| Brand | `discovery/brand/` | <status> | <count> |
| Journeys | `experience/journeys/` | <status> | <count> |
| Features | `experience/features/` | <status> | <count> |
| Screens | `experience/screens/` | <status> | <count> |
| Tech Stack | `blueprint/` | <status> | <count> |
| Architecture | `blueprint/` | <status> | <count> |
| Data Model | `blueprint/datamodel/` | <status> | <count> |

---

### Issues

| # | Severity | Category | File | Details |
|---|----------|----------|------|---------|
| 1 | CRITICAL | <category> | <path> | <description> |
| 2 | HIGH | <category> | <path> | <description> |
| 3 | MEDIUM | <category> | <path> | <description> |

---

### Recommended Actions

1. <highest-priority action>
2. <second action>
3. <third action>

> Run `garden` mode to auto-fix safe issues.
> Run `sync` to repair cross-references.
```

### Pipeline Completeness Status Values

| Symbol | Meaning |
|--------|---------|
| `✓ present` | All required files present |
| `⚠ partial` | Some required files present, some missing |
| `— missing` | Folder exists but no required output files |
| `— not started` | Folder does not exist |

---

## Gardening Mode — Report

```
## Doc Gardening Report

Score before: <before>/100

### Auto-fixed (<N> changes)

- ✓ `<path>` — <description of fix applied>
- ✓ `<path>` — <description of fix applied>

### Needs human attention (<N> issues)

- ⚠ `<path>` — <description of issue requiring human judgment>
- ⚠ `<path>` — <description of issue requiring human judgment>

### Score: <before> → <after>/100
```

---

## Observability Events

### Audit mode

```
[review] started mode=audit run_id=<uuid>
[review] audit_pass check=<check_name> files=<N>
[review] audit_warn check=<check_name> file=<path> detail=<msg>
[review] audit_fail check=<check_name> file=<path> detail=<msg>
[review] completed mode=audit run_id=<uuid> score=<N> issues=<N>
```

### Gardening mode

```
[review] started mode=gardening run_id=<uuid>
[review] auto_fix file=<path> action=<description> value=<new_value>
[review] audit_warn check=<check_name> file=<path> detail=<msg>
[review] completed mode=gardening run_id=<uuid> auto_fixes=<N> remaining=<N> score_before=<N> score_after=<N>
```
