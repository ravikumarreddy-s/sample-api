#!/usr/bin/env bash
# PreToolUse (Bash): PR merges are owner-only. Block any `gh pr merge`.
set -uo pipefail

cmd=$(jq -r '.tool_input.command // ""')

if printf '%s' "$cmd" | grep -Eq '(^|[^[:alnum:]_-])gh[[:space:]]+pr[[:space:]]+merge([[:space:]]|$)'; then
  echo "Blocked: PR merges are owner-only. Open the PR and leave merging to the owner." >&2
  exit 2
fi
exit 0
