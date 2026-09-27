#!/usr/bin/env bash
# on-project-open.sh — triggered when a project session is opened
# Reads the GitAgent definition and emits an imprint context payload
# that agent-manager.ts picks up to build the system prompt.
#
# Usage: on-project-open.sh <slug>
# Stdout: JSON imprint payload
# Exit 0: success; Exit 1: fatal error (blocks session start)

set -euo pipefail

SLUG="${1:-}"
AGENT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"  # agent/ root
PROJECT_DIR="data/projects/${SLUG}"

if [[ -z "$SLUG" ]]; then
  echo '{"error":"on-project-open: missing slug argument"}' >&2
  exit 1
fi

# Load SOUL.md
SOUL=""
if [[ -f "${AGENT_DIR}/SOUL.md" ]]; then
  SOUL=$(cat "${AGENT_DIR}/SOUL.md")
fi

# Load knowledge files listed in index.yaml (simple: load all *.md in knowledge/)
KNOWLEDGE=""
for f in "${AGENT_DIR}/knowledge/"*.md; do
  [[ -f "$f" ]] || continue
  KNOWLEDGE="${KNOWLEDGE}\n\n---\n\n$(cat "$f")"
done

# Load project-specific CLAUDE.md if present
PROJECT_CLAUDE=""
if [[ -f "${PROJECT_DIR}/CLAUDE.md" ]]; then
  PROJECT_CLAUDE=$(cat "${PROJECT_DIR}/CLAUDE.md")
fi

# Emit JSON payload consumed by server/utils/agent-definition.ts
python3 - <<PYEOF
import json, sys

soul = """${SOUL}"""
knowledge = """${KNOWLEDGE}"""
project_claude = """${PROJECT_CLAUDE}"""

parts = [soul.strip()]
if knowledge.strip():
    parts.append(knowledge.strip())
if project_claude.strip():
    parts.append("## Project-Specific Instructions\n\n" + project_claude.strip())

payload = {
    "slug": "${SLUG}",
    "system_prompt": "\n\n---\n\n".join(parts),
    "source": "gitagent-imprint"
}
print(json.dumps(payload))
PYEOF
