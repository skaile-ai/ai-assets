# Pipeline Contract: pipeline.json

The pipeline definition is the machine-readable dependency graph that drives the entire skill system.

## Schema

```typescript
interface PipelineJson {
  version: string                    // "2.0"
  phases: Record<string, Phase>      // execution phases
  steps: Step[]                      // all pipeline steps
  feedback_loops: FeedbackLoop[]     // cross-reference rules
  config: Config                     // system configuration
}

interface Phase {
  name: string
  description: string
  order: number                      // execution order
  sub_phases?: Record<string, { name: string; order: number }>
}

interface Step {
  id: string                         // unique step identifier
  name: string                       // human-readable name
  skill: string                      // skill to invoke
  phase: string                      // which phase this belongs to
  sub_phase: string | null           // optional sub-phase grouping
  folder: string | null              // artifact folder this step writes to
  depends_on: string[]               // step IDs that must complete first
  optional_reads?: string[]          // folders to read if they exist
  optional: boolean                  // can be skipped based on complexity
  parallel_group: string | null      // steps with same group run concurrently
  subagent: boolean                  // run in fresh agent context
  hard_gates: HardGate[]             // file existence checks
  user_inputs: UserInputs | null     // what the skill needs from the user
  description: string                // what this step produces
  outputs?: string[]                 // expected output files
}

interface HardGate {
  type: "file_exists"
  path: string                       // relative to _concept/
}

interface UserInputs {
  dialog: DialogInput[]              // questions to ask the user
  files: string[]                    // _concept/ files needed as input
}

interface DialogInput {
  id: string
  label: string
  type: "text" | "select" | "multiselect" | "boolean" | "number" | "textarea"
  required?: boolean
  options?: string[]                 // for select/multiselect
  default?: string
  hint?: string
}

interface FeedbackLoop {
  from: string                       // skill that creates the link
  to: string                        // artifact folder being updated
  field: string                      // frontmatter field being modified
  description: string
}

interface Config {
  orchestrator_skill: string
  standalone_mode: { enabled: boolean; description: string }
  research_mode?: Record<string, unknown>
  complexity_presets: Record<string, {
    skip_steps: string[]
    research_depth: string
  }>
  routes: Record<string, {
    name: string
    description: string
    include_phases: string[]
    skip_steps: string[]
    default_complexity: string
    default_research_depth: string
  }>
  profiles?: { file: string; project_override: string }
  standards_mode?: Record<string, unknown>
  standards_injection?: Record<string, unknown>
}
```

## Design Rules

1. **Steps form a DAG** — `depends_on` must not create cycles
2. **Hard gates are file_exists only** — no status checks, no approval gates
3. **Parallel groups** — steps with same `parallel_group` run concurrently within a phase
4. **Routes vs complexity** — routes define which phases exist, complexity filters optional steps within
5. **User inputs** — define what the skill needs upfront so UIs can generate forms
6. **Feedback loops** — declare cross-reference rules so quality tools can verify integrity

## Step Status Computation (Server-Side)

Step status is computed by scanning artifact folders, NOT stored:

```
no files in folder         → not_started
some files, not all outputs → in_progress
all expected outputs exist  → complete
explicitly approved         → approved  (stored in config.json)
dependencies not met        → blocked
route/complexity skips it   → skipped
```

This means status is always fresh — no stale state to manage.
