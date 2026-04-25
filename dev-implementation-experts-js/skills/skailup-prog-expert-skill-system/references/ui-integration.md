# UI Integration: Two-Way Skill-to-UI Communication

## Architecture

A skill system can have a companion browser UI that provides:
- Visual pipeline progress tracking
- Artifact viewing and editing
- Onboarding wizard for project setup
- Step-level actions (run, approve, revise)

The UI and skills communicate through **artifacts on disk** and **API endpoints**.

## Communication Patterns

### Pattern 1: Pipeline Status (UI reads artifacts)

The UI computes step status by scanning artifact folders — no status field stored:

```typescript
// Server-side: scan _concept/ folders to determine step status
function getStepStatus(step, conceptDir) {
  const dir = join(conceptDir, step.folder)
  const fileCount = countFiles(dir)

  if (fileCount === 0) return 'not_started'

  // Check if key output files exist
  const outputs = step.outputs.filter(o => !o.includes('<'))
  const existing = outputs.filter(o => existsSync(join(dir, o)))
  if (existing.length < outputs.length) return 'in_progress'

  return 'complete'
}
```

Step status types: `not_started | in_progress | complete | approved | skipped | blocked`

### Pattern 2: Config State (Machine state separate from agent state)

Two separate state files:
- **config.json** — machine state (route, complexity, approved_steps). Read by the UI.
- **PLANS.md** — agent-readable plan with raw description. Read by skills.

```typescript
// config.json (UI reads/writes)
interface ConceptConfig {
  route: string
  complexity: string
  research_depth: string
  profile: string
  created: string
  approved_steps: string[]  // pipeline step approval (UI workflow)
}
```

### Pattern 3: Onboarding (UI drives initial setup)

The onboarding wizard collects:
1. **Route** — which pipeline route to use (e.g., mvp, prototype, product)
2. **Profile** — configuration preset (syncs complexity + research_depth)
3. **Description** — raw project description for the first skill
4. **Complexity** — overrideable, seeded from profile
5. **Research depth** — overrideable, seeded from profile

On submit:
- Creates config.json with machine state
- Creates PLANS.md with raw description (agent-readable)
- Does NOT create artifact folders (skills create their own output)

### Pattern 4: Step Approval (Pipeline-level, not file-level)

Step approval is a UI workflow concept — "I've reviewed this step's output and want to proceed."

- Stored in `config.json.approved_steps[]`
- Separate from file-level `impl_status` (pending/implemented/tested)
- UI shows "Approve & Continue" button when step status is `complete`
- Approval unlocks downstream steps that depend on this one

### Pattern 5: impl_status Aggregation (File frontmatter → sidebar badges)

For artifact folders that track implementation progress (features, screens):

```typescript
// Server: scan markdown files for impl_status frontmatter
function scanImplStatus(dir: string): { pending: number; implemented: number; tested: number } | null {
  // Recursively read .md files, parse frontmatter, count impl_status values
}

// Client: show color-coded badge in sidebar
// Green: all tested | Blue: all implemented | Yellow: partial
```

### Pattern 6: Sidebar File Tree (Hidden directories)

Some artifact folders should not appear in the sidebar:
- `_grounding/` — research data, too noisy for editing
- Other internal folders as needed

```typescript
const HIDDEN_DIRS = new Set(['_grounding'])
```

Folders like `_standards/` should be VISIBLE — users may want to edit discovered conventions.

### Pattern 7: AI Drawer (Contextual skill invocation)

When a user focuses a pipeline step in the sidebar:
- The main content area shows step-specific UI (blocked/ready/complete/approved)
- An AI chat drawer opens with step-specific starter questions
- "Generate with AI" triggers the skill via agent prompt API
- The drawer concept is `_pipeline` for consistent chat history

## API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/pipeline/status` | GET | Returns computed pipeline status (steps, phases, settings) |
| `/api/pipeline/start` | POST | Creates config.json + PLANS.md from onboarding data |
| `/api/pipeline/approve/:stepId` | POST | Adds step to approved_steps in config.json |
| `/api/concepts` | GET | Lists all artifact files (filtered by HIDDEN_DIRS) |
| `/api/concepts/:path` | GET/POST | Read/write individual artifact files |
| `/api/agent/prompt` | POST | Sends a prompt to the agent to trigger a skill |

## Key Insight: Separation of Concerns

```
UI config (config.json)     ←→  UI reads/writes
Agent plan (PLANS.md)       ←→  Skills read/write
Artifacts (_concept/)       ←→  Both read, skills write, UI displays
Pipeline def (pipeline.json) ←→  Both read, neither writes
```

The UI never writes artifacts directly (except through the editor). Skills never read config.json. PLANS.md is the bridge — the UI writes the initial plan, skills read and update it.
