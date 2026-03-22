# Agent Skills Specification Reference

Summary of the [Agent Skills Specification](https://agentskills.io/specification) for quick reference during skill building.

## Required Fields

| Field | Constraints |
|---|---|
| `name` | 1–64 chars, lowercase a-z + digits + hyphens, no leading/trailing/consecutive hyphens, must match parent directory |
| `description` | 1–1024 chars, describes what + when to use, include routing keywords |

## Optional Root Fields

| Field | Constraints |
|---|---|
| `license` | License name or reference to bundled file |
| `compatibility` | 1–500 chars, environment requirements |
| `metadata` | Arbitrary key-value mapping for extensions |
| `allowed-tools` | Space-delimited pre-approved tool list (experimental) |

## Directory Structure

```
skill-name/
├── SKILL.md          # Required: metadata + instructions
├── scripts/          # Optional: executable code
├── references/       # Optional: detailed docs loaded on demand
├── assets/           # Optional: templates, schemas, resources
└── examples/         # Optional: calibration interactions
```

## Progressive Disclosure

1. **Metadata** (~100 tokens): name + description — always in context
2. **Instructions** (<5000 tokens): SKILL.md body — loaded on activation
3. **Resources** (as needed): scripts/, references/, assets/ — loaded on demand

## Validation

```bash
npx skills-ref validate ./my-skill
```
