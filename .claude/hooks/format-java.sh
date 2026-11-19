#!/bin/bash
# PostToolUse hook for Edit and Write.
# Formats an edited Java file with Spotless so style never drifts.
# It never blocks: formatting problems are caught again by the spotless:check gate.
# Written for macOS bash 3.2.

input=$(cat)
path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')

case "$path" in
  *.java) ;;
  *) exit 0 ;;
esac

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
if [ ! -x "$project_dir/mvnw" ] || [ ! -f "$path" ]; then
  exit 0
fi

# Spotless expects a regular expression of absolute paths.
escaped=$(printf '%s' "$path" | sed 's/[][\.^$*+?(){}|/]/\\&/g')
(cd "$project_dir" && ./mvnw -q spotless:apply -DspotlessFiles="$escaped" >/dev/null 2>&1) || true

exit 0
