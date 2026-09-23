#!/usr/bin/env bash
# TEMPORARY — negative control for #81. Removed before this PR is ready.
set -uo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/assert.sh"
assert_eq "green" "red" "negative control: the lane must report this as a failure"
assert_done
