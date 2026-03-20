# Conflict Resolution Strategies

Detailed guide for resolving conflicts between PostXL-generated code and
custom implementations.

## Understanding PostXL's Ejection System

PostXL tracks every generated file in `postxl-lock.json`. When a file is
modified by a developer (custom code added), it becomes "ejected" — the
generator will skip it on subsequent runs unless explicitly told to overwrite.

### File States

| State | In lock file | Modified by user | Generator behavior |
|-------|-------------|-----------------|-------------------|
| Generated | Yes (hash matches) | No | Overwrite freely |
| Ejected | Yes (hash differs) | Yes | Skip (preserve user changes) |
| New | No | N/A | Create |
| Custom-only | No | Yes | Ignore |

### Custom Block Markers

Within generated files, PostXL uses markers to delineate user-customizable sections:

```typescript
// <<<<<<< Custom: imports
import { CustomService } from './custom.service'
// >>>>>>> Custom: imports

// Generated code here...

// <<<<<<< Custom: methods
async customMethod() {
  // User's custom business logic
}
// >>>>>>> Custom: methods
```

The generator preserves everything between custom markers and regenerates
everything else.

## Conflict Resolution Cascade

### Level 1: No Conflict (Most Common)

**Scenario:** File is generated-only, no custom modifications.
**Action:** Generator overwrites automatically.
**Frequency:** ~80% of files.

### Level 2: Custom Blocks Preserved

**Scenario:** File has custom blocks, generator wants to update surrounding code.
**Action:** Generator regenerates non-custom sections, preserves custom blocks.
**Frequency:** ~15% of files.
**Verification:** After regeneration, verify custom blocks survived:

```bash
# Check custom blocks are intact
grep -c "<<<<<<< Custom" <file>
# Should match the expected number of custom blocks
```

### Level 3: Ejected File Merge

**Scenario:** User ejected the file (modified outside custom blocks) AND
generator has updates.
**Action:** Intelligent merge.

**Strategy:**

1. Get the generator's version: `pnpm run generate --diff`
2. Compare with current file using a three-way diff:
   - **Base:** Previous generated version (from git history)
   - **Ours:** Current file (with user modifications)
   - **Theirs:** New generated version

3. Apply merge rules:

| Section | Who changed it | Resolution |
|---------|---------------|-----------|
| Imports | Generator added new import | Accept generator |
| Imports | User added custom import | Preserve user |
| Imports | Both | Merge both (no conflict) |
| Type definitions | Generator updated types | Accept generator (types must match schema) |
| Generated methods | Generator updated | Accept generator |
| Custom methods | User wrote | Preserve user |
| Configuration | Generator updated | Accept generator, check for user overrides |
| Both modified same function | — | **Requires analysis** |

4. For functions both modified:
   - If changes are additive (both added different things): merge
   - If changes are contradictory: prefer the version that matches the current schema
   - If unclear: this is a Level 4 escalation

### Level 4: Feature-Level Conflict

**Scenario:** Two features or a feature and the generator want fundamentally
different things in the same code.

**Examples:**
- Generator creates a standard CRUD page, but the feature spec requires a
  completely custom UI
- Two features both modify the same shared component differently
- Schema change invalidates a feature's custom implementation

**Action:** Do NOT ask the user to resolve code. Instead:

1. Identify which concept artifact drives the conflict
2. Suggest refining the concept:
   ```
   Conflict detected in AppList component.

   The generator creates a standard DataGrid, but the concept's screen spec
   for app_list.md specifies a custom card-based layout.

   Recommendation: Eject this file (accept custom version) and implement
   the screen spec's design. The generator's DataGrid is not needed here.
   ```
3. If the conflict is between two features: suggest the feature that was
   implemented second should adapt to the first

## Common Conflict Patterns

### Pattern 1: Generated Route vs. Custom Route

Generated routes follow a standard pattern. Custom routes diverge for UX reasons.

**Resolution:** Eject the route file. Use the custom version. Add route to
`.postxl-ignore` if repeated regeneration causes issues.

### Pattern 2: Generated tRPC Router vs. Custom Endpoints

The generator creates standard CRUD endpoints. Features add custom endpoints.

**Resolution:** Custom endpoints should be in separate files (not modifying
generated router files). Use custom blocks if the generated file structure
requires it.

### Pattern 3: Prisma Schema Changes

Schema changes can invalidate existing migrations.

**Resolution:**
- Development: `prisma migrate reset` + re-seed
- If custom migrations exist: create a new migration that bridges the gap

### Pattern 4: Component Style Conflicts

Generated components use default styling. Brand tokens require different styling.

**Resolution:** Brand tokens should override via CSS custom properties (set up
by `implement-1-setup-2-foundation`), not by modifying component source. If a generated component
doesn't support theming, eject it and apply brand tokens directly.

## Prevention Strategies

1. **Use custom blocks** for all custom code within generated files
2. **Eject early** if a file will be heavily customized
3. **Separate custom code** into distinct files when possible
4. **Run generation after each feature group** to catch conflicts early
5. **Never modify generated type files** — they must match the schema

## Tools

```bash
# Show which files are ejected
pnpm run generate --ejected

# Show diffs between current and would-be-generated
pnpm run generate --diff

# Force regenerate specific files (overwrites ejections)
pnpm run generate --force --pattern "backend/libs/router/**"

# Skip specific files during generation
# Add to .postxl-ignore (if supported) or use --pattern to exclude
```
