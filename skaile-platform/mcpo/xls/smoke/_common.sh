# Shared bash setup for smoke scripts. Source from each scenario script:
#
#   set -euo pipefail
#   source "$(dirname "$0")/_common.sh"
#
# Exports SMOKE_DIR (so embedded Python can locate _smoke_common.py), ROOT, and JAR;
# sets EXCEL_MCP_ALLOW_UNSANDBOXED=true so the fail-closed default doesn't abort startup
# (the smoke suite tests behaviour unrelated to the sandbox); and verifies the jar exists.

export EXCEL_MCP_ALLOW_UNSANDBOXED=true

SMOKE_DIR="$(cd "$(dirname "${BASH_SOURCE[1]}")" && pwd)"
ROOT="$(cd "$SMOKE_DIR/.." && pwd)"
JAR="$ROOT/target/excel-mcp-0.2.1.jar"
[[ -f "$JAR" ]] || { echo "jar not found: $JAR" >&2; exit 2; }
export SMOKE_DIR ROOT JAR
