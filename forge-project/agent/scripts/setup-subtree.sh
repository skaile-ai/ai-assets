#!/usr/bin/env bash
# setup-subtree.sh — set up agent/ as a git subtree linked to a separate remote repo
#
# Run this ONCE after creating the remote agent repository.
#
# Usage:
#   ./agent/scripts/setup-subtree.sh <remote-url>
#   Example:
#     ./agent/scripts/setup-subtree.sh git@bitbucket.org:ecsplico/pichi-agent.git
#
# After setup, to push agent/ changes to the separate repo:
#   git subtree push --prefix=agent pichi-agent main
#
# To pull in upstream changes from the agent repo:
#   git subtree pull --prefix=agent pichi-agent main --squash

set -euo pipefail

REMOTE_URL="${1:-}"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

if [[ -z "$REMOTE_URL" ]]; then
  echo "Usage: $0 <remote-url>" >&2
  echo "" >&2
  echo "Create the remote repo first, then run:" >&2
  echo "  $0 git@bitbucket.org:ecsplico/pichi-agent.git" >&2
  exit 1
fi

cd "$REPO_ROOT"

echo "Adding remote 'pichi-agent' → $REMOTE_URL"
git remote add pichi-agent "$REMOTE_URL" 2>/dev/null || \
  git remote set-url pichi-agent "$REMOTE_URL"

echo ""
echo "Pushing agent/ as subtree to pichi-agent/main..."
echo "(This may take a moment — it splits the subtree history)"
git subtree push --prefix=agent pichi-agent main

echo ""
echo "Done. The agent/ directory is now tracked as a subtree."
echo ""
echo "Future workflow:"
echo "  # Push agent changes to separate repo:"
echo "  git subtree push --prefix=agent pichi-agent main"
echo ""
echo "  # Pull changes from agent repo into Pichi:"
echo "  git subtree pull --prefix=agent pichi-agent main --squash"
echo ""
echo "  # Create a PR for a persona/skill change:"
echo "  ./agent/scripts/propose-change.sh persona 'description of change'"
