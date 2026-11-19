---
name: ai-log
description: Appends an entry to docs/ai-workflow.md recording how a Cairn task was built - which agents and models did what, how many rounds it took, what reviewers and security caught, what was rejected, and what CI found. Use after a task's pull request is merged, or when the director asks how a task was built.
argument-hint: "[task-id]"
---

# Log task $ARGUMENTS in the AI workflow record

`docs/ai-workflow.md` is the honest record of how Cairn was built with AI agents. It is
interview evidence, so it must be accurate and specific.

## Gather

- The task card, the pull request (`gh pr view <number>`), and every implementer, review and
  security report from every round.
- CI runs for the pull request (`gh pr checks <number>`).

## Append this entry

```
### <task id>: <title> (<YYYY-MM-DD>)
- Pull request: <link>
- Implementer: <agent> (<model>); rounds: <N>
- Reviewer findings: <count by severity>; most important: <one line and how it was fixed>
- Security findings: <count by severity>; most important: <one line or "none">
- Rejected: <what the orchestrator or a reviewer rejected from an agent and why, or "nothing">
- CI: <passed first time, or what failed in CI and why>
- Director decisions: <questions escalated and the answers, or "none">
- Interview note: <one sentence the director could say about this task>
```

## Rules

- Record failures and rejections as carefully as successes; they are the most valuable part.
- No personal data and no secrets.
- Never edit earlier entries except to fix a factual error, and say so when you do.
