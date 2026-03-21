# Reference Implementation: Pipeline Status Computation

Server-side computation of pipeline step status from artifact folders. From concept-forge's `server/utils/pipeline.ts`.

## Core Algorithm

```typescript
type StepStatus = 'not_started' | 'in_progress' | 'complete' | 'approved' | 'skipped' | 'blocked'

interface ImplStatusSummary {
  pending: number
  implemented: number
  tested: number
}

function getPipelineStatus(): PipelineStatus {
  const definition = loadPipelineDefinition()  // Read pipeline.json
  const conceptDir = getConceptDir()           // _concept/ path
  const approvedSteps = loadConfig()?.approved_steps ?? []

  // First pass: compute raw status from file system
  const stepStatuses = new Map<string, StepStatus>()
  const rawStatuses = definition.steps.map(step => {
    let status: StepStatus
    let fileCount: number

    if (!step.folder) {
      status = 'not_started'
      fileCount = 0
    } else {
      const dir = join(conceptDir, step.folder)
      fileCount = countFilesRecursive(dir)

      if (fileCount === 0) {
        status = 'not_started'
      } else {
        // Check expected outputs
        const outputs = (step.outputs || []).filter(o => !o.includes('<'))
        if (outputs.length > 0) {
          const existing = outputs.filter(o => existsSync(join(dir, o)))
          status = existing.length < outputs.length ? 'in_progress' : 'complete'
        } else {
          status = 'complete'
        }
      }
    }

    // Override: approved in config.json
    if (approvedSteps.includes(step.id) && (status === 'complete' || status === 'in_progress')) {
      status = 'approved'
    }

    stepStatuses.set(step.id, status)
    return { step, status, fileCount }
  })

  // Second pass: compute canRun and blocked
  return rawStatuses.map(({ step, status, fileCount }) => {
    const depsComplete = step.depends_on.every(dep => {
      const depStatus = stepStatuses.get(dep)
      return depStatus === 'complete' || depStatus === 'approved'
    })

    if (!depsComplete && step.hard_gates.length > 0 && status === 'not_started') {
      status = 'blocked'
    }

    const canRun = depsComplete && status !== 'complete' && status !== 'approved' && status !== 'skipped'

    // Aggregate impl_status from frontmatter (features/screens only)
    const implStatus = scanImplStatus(join(conceptDir, step.folder))

    return { ...step, status, canRun, fileCount, implStatus }
  })
}
```

## impl_status Aggregation

```typescript
const IMPL_STATUS_FOLDERS = new Set(['03_features', '07_screens'])

function scanImplStatus(dir: string, folderName: string): ImplStatusSummary | null {
  if (!IMPL_STATUS_FOLDERS.has(folderName)) return null
  if (!existsSync(dir)) return null

  const summary = { pending: 0, implemented: 0, tested: 0 }
  scanRecursive(dir, summary)
  const total = summary.pending + summary.implemented + summary.tested
  return total > 0 ? summary : null
}

function scanRecursive(dir: string, summary: ImplStatusSummary): void {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      scanRecursive(join(dir, entry.name), summary)
    } else if (entry.name.endsWith('.md')) {
      const content = readFileSync(join(dir, entry.name), 'utf-8')
      const fm = parseFrontmatter(content)
      if (fm?.impl_status === 'implemented') summary.implemented++
      else if (fm?.impl_status === 'tested') summary.tested++
      else summary.pending++
    }
  }
}
```

## Key Design Decisions

1. **Status is always recomputed** — never stored in a status field
2. **Two-pass algorithm** — first compute raw status, then resolve dependencies
3. **config.json stores approvals** — separate from artifact state
4. **impl_status is aggregated** — individual file frontmatter → step-level summary
5. **blocked requires hard_gates** — steps without gates are never blocked, just not_started
