#!/usr/bin/env bash
# PreToolUse (Bash): no commits or pushes on/to main; use a feature branch + PR.
set -uo pipefail

cmd=$(jq -r '.tool_input.command // ""')
dir="${CLAUDE_PROJECT_DIR:-$PWD}"

# Explicit push to main from any branch (e.g. `git push origin main`, `HEAD:main`).
if printf '%s' "$cmd" | grep -Eq 'git[[:space:]]+push([[:space:]].*)?[[:space:]:+]main([[:space:]]|$|;|&|\|)'; then
  echo "Blocked: don't push to main. Push a feature branch and open a PR." >&2
  exit 2
fi

if printf '%s' "$cmd" | grep -Eq 'git[[:space:]]+(commit|push)([[:space:]]|$)'; then
  branch=$(git -C "$dir" branch --show-current 2>/dev/null || true)
  if [ "$branch" = "main" ]; then
    echo "Blocked: current branch is main. Create a feature branch first (in a separate command), then commit." >&2
    exit 2
  fi
fi
exit 0
