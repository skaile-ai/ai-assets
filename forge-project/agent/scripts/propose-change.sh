#!/usr/bin/env bash
# propose-change.sh — create a PR branch for a forge-project agent definition change
#
# Usage:
#   ./agent/scripts/propose-change.sh persona  "add warmth to tone"
#   ./agent/scripts/propose-change.sh rules    "allow reading files outside workspace with confirmation"
#   ./agent/scripts/propose-change.sh skill    "add new skill: database-management"
#
# The script:
#   1. Creates branch  agent/<type>/<slug>
#   2. Opens $EDITOR for you to make the change
#   3. Commits + pushes
#   4. Prints the Bitbucket PR URL
#
# Requires: git, bash 4+

set -euo pipefail

CHANGE_TYPE="${1:-}"
CHANGE_SUMMARY="${2:-}"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BITBUCKET_REPO="ecsplico/forge-project"

if [[ -z "$CHANGE_TYPE" || -z "$CHANGE_SUMMARY" ]]; then
  echo "Usage: $0 <persona|rules|duties|skill> <\"change summary\">" >&2
  exit 1
fi

# Slug the summary for branch name
SLUG=$(echo "$CHANGE_SUMMARY" | tr '[:upper:]' '[:lower:]' | tr ' ' '-' | tr -cd 'a-z0-9-')
BRANCH="agent/${CHANGE_TYPE}/${SLUG}"

cd "$REPO_ROOT"

# Ensure we're on main and clean
if [[ "$(git branch --show-current)" != "main" ]]; then
  echo "Error: must run from the main branch" >&2
  exit 1
fi

git fetch origin main --quiet

# Create branch
git checkout -b "$BRANCH"
echo ""
echo "Branch created: $BRANCH"
echo ""

# Determine which file to edit based on change type
case "$CHANGE_TYPE" in
  persona)  TARGET="agent/SOUL.md" ;;
  rules)    TARGET="agent/RULES.md" ;;
  duties)   TARGET="agent/DUTIES.md" ;;
  skill)
    echo "Enter the skill directory name (e.g., 'database-management'): "
    read -r SKILL_NAME
    TARGET="agent/skills/${SKILL_NAME}/SKILL.md"
    mkdir -p "$(dirname "$TARGET")"
    ;;
  *)
    echo "Unknown change type: $CHANGE_TYPE (use: persona, rules, duties, skill)" >&2
    git checkout main
    git branch -D "$BRANCH"
    exit 1
    ;;
esac

echo "Opening $TARGET for editing..."
echo "(Make your changes, save, and close the editor)"
echo ""
${EDITOR:-nano} "$TARGET"

# Stage and commit
git add "$TARGET"
git commit -m "agent(${CHANGE_TYPE}): ${CHANGE_SUMMARY}

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"

# Push
git push -u origin "$BRANCH"

echo ""
echo "Branch pushed. Open your PR:"
echo "  https://bitbucket.org/${BITBUCKET_REPO}/pull-requests/new?source=${BRANCH}&dest=main"
echo ""
echo "After the PR is merged, run:"
echo "  git checkout main && git pull origin main"
