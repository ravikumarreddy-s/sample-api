#!/usr/bin/env bash
# PreToolUse (Bash): no commits or pushes on/to main; use a feature branch + PR.
set -uo pipefail

cmd=$(jq -r '.tool_input.command // ""')
dir="${CLAUDE_PROJECT_DIR:-$PWD}"

# Check each part of a chained command separately, so text after a `git push`
# (e.g. `&& gh pr create --base main`) isn't mistaken for a push target.
segments=$(printf '%s\n' "$cmd" | awk '{ gsub(/&&|\|\||;|\|/, "\n"); print }')

# Explicit push to main from any branch (e.g. `git push origin main`, `HEAD:main`).
while IFS= read -r seg; do
  if printf '%s' "$seg" | grep -Eq 'git[[:space:]]+push([[:space:]].*)?[[:space:]:+]main([[:space:]]|$)'; then
    echo "Blocked: don't push to main. Push a feature branch and open a PR." >&2
    exit 2
  fi
done <<< "$segments"

if printf '%s' "$cmd" | grep -Eq 'git[[:space:]]+(commit|push)([[:space:]]|$)'; then
  branch=$(git -C "$dir" branch --show-current 2>/dev/null || true)
  if [ "$branch" = "main" ]; then
    # On main: allow only pushes that delete a remote branch (deleting main is
    # already blocked above). Per-segment so `git push --delete x && git commit ...`
    # is still blocked.
    while IFS= read -r seg; do
      if printf '%s' "$seg" | grep -Eq 'git[[:space:]]+commit([[:space:]]|$)'; then
        blocked=1
      elif printf '%s' "$seg" | grep -Eq 'git[[:space:]]+push([[:space:]]|$)' &&
           ! printf '%s' "$seg" | grep -Eq '[[:space:]](--delete|-d)([[:space:]]|$)'; then
        blocked=1
      fi
    done <<< "$segments"

    if [ "${blocked:-0}" = 1 ]; then
      echo "Blocked: current branch is main. Create a feature branch first (in a separate command), then commit." >&2
      exit 2
    fi
  fi
fi
exit 0
