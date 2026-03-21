# Multi-Agent — Parent + Sub-Agent Hierarchy

## Root agent.yaml

```yaml
spec_version: "0.1.0"
name: engineering-workspace
version: 1.0.0
description: Multi-agent workspace for software engineering

model:
  preferred: claude-opus-4-6

skills:
  - overview
  - features
  - implement

agents:
  frontend:
    description: Frontend development agent
    delegation:
      mode: auto
      triggers:
        - frontend_task_detected
  backend:
    description: Backend development agent
    delegation:
      mode: auto
      triggers:
        - backend_task_detected

dependencies:
  - name: frontend
    source: ./frontend
    version: 1.0.0
    mount: frontend
  - name: backend
    source: ./backend
    version: 1.0.0
    mount: backend

delegation:
  mode: router
  router: overview
```

## frontend/agent.yaml

```yaml
spec_version: "0.1.0"
name: frontend-agent
version: 1.0.0
description: React/Vue frontend development specialist
extends: ../agent.yaml

model:
  preferred: claude-sonnet-4-6

skills:
  - component-builder
  - style-review
  - a11y-check

runtime:
  max_turns: 50
```

## frontend/SOUL.md

```markdown
# Frontend Agent

## Core Identity
You specialize in building accessible, performant frontend interfaces.

## Values
- Accessibility first (WCAG 2.1 AA minimum)
- Component composition over inheritance
- Progressive enhancement
```

## backend/agent.yaml

```yaml
spec_version: "0.1.0"
name: backend-agent
version: 1.0.0
description: API and database development specialist
extends: ../agent.yaml

model:
  preferred: claude-sonnet-4-6

skills:
  - api-design
  - migration
  - security-review
```
