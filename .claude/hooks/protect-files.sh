#!/bin/bash
# PreToolUse hook for Edit and Write.
# Denies edits to secret files and asks before edits to CI and agent configuration.
# Written for macOS bash 3.2.

input=$(cat)
path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')

if [ -z "$path" ]; then
  exit 0
fi

name=$(basename "$path")

decide() {
  jq -n --arg decision "$1" --arg reason "$2" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: $decision,
      permissionDecisionReason: $reason
    }
  }'
  exit 0
}

# Secrets and keys are never edited by agents.
case "$name" in
  .env | .env.local | .env.production | .env.development | *.pem | *.key | *.p12 | id_rsa* | id_ed25519*)
    decide "deny" "Editing secret or key files is not allowed by project guardrails."
    ;;
esac

# CI, agent configuration and project instructions change only with the director's approval.
case "$path" in
  */.github/workflows/* | .github/workflows/*)
    decide "ask" "This changes a CI workflow. The director must approve workflow changes."
    ;;
  */.claude/settings.json | .claude/settings.json | */.claude/hooks/* | .claude/hooks/*)
    decide "ask" "This changes agent permissions or hooks. The director must approve."
    ;;
  */.claude/agents/* | .claude/agents/* | */.claude/rules/* | .claude/rules/* | */CLAUDE.md | CLAUDE.md)
    decide "ask" "This changes agent instructions or project rules. The director must approve."
    ;;
  */src/main/resources/db/migration/*)
    # Merged migrations are append-only. Editing an existing file needs confirmation.
    if [ -f "$path" ]; then
      decide "ask" "This edits an existing migration. Migrations are append-only once merged."
    fi
    ;;
esac

exit 0
