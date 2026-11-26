#!/usr/bin/env bash
# PostToolUse (Edit|Write): run the test suite after a .java file changes.
set -uo pipefail

file=$(jq -r '.tool_input.file_path // ""')
case "$file" in
  *.java) ;;
  *) exit 0 ;;
esac

cd "${CLAUDE_PROJECT_DIR:-$PWD}" || exit 0
if ! out=$(./gradlew test -q 2>&1); then
  echo "Tests failed after editing $file:" >&2
  printf '%s\n' "$out" | tail -n 40 >&2
  exit 2
fi
exit 0
