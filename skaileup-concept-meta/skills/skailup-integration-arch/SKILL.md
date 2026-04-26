---
name: project-concept-integration-architecture
description: "Generate the 3_integration/ section of a meta-concept: inter-repo architecture, deployment topology, and shared contracts for a multi-product ecosystem."
source: MERGED
version: 1.0.0
keywords: [meta-concept, integration, architecture, deployment, shared-contracts, ecosystem]
user_inputs:
  - key: PROJECT_ROOT
    prompt: "Where is the shell repo root?"
    required: true
reads_from:
  - contracts/meta-concept-contract/CONTRACT.md
  - "{PROJECT_ROOT}/_concept/discovery/"
  - "{PROJECT_ROOT}/_concept/2_subsystems/"
  - "{PROJECT_ROOT}/CLAUDE.md"
  - "{PROJECT_ROOT}/package.json"
writes_to:
  - "{PROJECT_ROOT}/_concept/3_integration/architecture.md"
  - "{PROJECT_ROOT}/_concept/3_integration/deployment.md"
  - "{PROJECT_ROOT}/_concept/3_integration/shared_contracts.md"
---

# Project Concept: Integration Architecture

Generate the integration layer of a multi-product umbrella concept.

## Prerequisites

- `discovery/brief.md` must exist
- `2_subsystems/index.md` must exist with at least two subsystems
- Read the meta-concept contract: `contracts/meta-concept-contract/CONTRACT.md`

## Process

### Step 1: Analyze Connections

Examine how subsystems connect:
- `package.json` workspaces and dependency resolution
- Git submodule references
- Shared `@scope/*` packages
- Runtime protocols (HTTP, WebSocket, IPC, Docker API)
- Shared file conventions (`CLAUDE.md`, `_concept/`, `skaile.yaml`)

### Step 2: Write architecture.md

Produce `_concept/3_integration/architecture.md` with:

1. **Frontmatter** per the contract schema
2. **Repo Topology** — how the repos are organized (submodules, workspaces, standalone)
3. **Dependency Resolution** — how cross-repo dependencies are resolved at dev time and at deploy time
4. **Package Graph** — which shared packages exist and who consumes them (table format)
5. **Runtime Protocols** — how subsystems communicate at runtime (if they do)
6. **Convention Alignment** — shared conventions across subsystems (naming, config, testing)
7. **Boundaries** — what is intentionally NOT shared and why

### Step 3: Write deployment.md

Produce `_concept/3_integration/deployment.md` with:

1. **Frontmatter** per the contract schema
2. **Deployment Models** — one section per distinct deployment pattern
   - For each: what subsystems it covers, infrastructure requirements, how it's triggered
3. **Environment Matrix** — dev, staging, production per subsystem
4. **Shared Infrastructure** — databases, auth providers, CDNs, registries used by multiple subsystems
5. **Independence Guarantees** — which subsystems can deploy independently vs. which require coordination

### Step 4: Write shared_contracts.md

Produce `_concept/3_integration/shared_contracts.md` with:

1. **Frontmatter** per the contract schema
2. **Shared Packages** — table of packages consumed by 2+ subsystems (name, purpose, consumers)
3. **Shared Types** — type definitions or interfaces that cross subsystem boundaries
4. **Shared Conventions** — file formats, directory structures, config patterns
5. **Contract Ownership** — who owns each shared contract and how changes propagate

## Output Quality

- Architecture must describe the ACTUAL connections, not aspirational ones
- Deployment models must be concrete enough for someone to set up the environment
- Shared contracts must list real packages with real consumers
- Do not describe intra-subsystem architecture — that belongs in the subsystem's own `_concept/`

## Iron Laws

- Read the `package.json` workspaces and submodule config to verify connections — do not guess
- Do not describe deployment infrastructure that doesn't exist yet without marking it as planned
- Ask the user to confirm the architecture summary before writing deployment and shared contracts
