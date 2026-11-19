---
name: plan-day
description: Morning standup for the Cairn director - reviews yesterday's results, open pull requests and CI, then proposes today's ordered task cards with agents, dependencies and risks for the director to approve. Use at the start of a work day or when the director asks what is next.
argument-hint: "[day number, optional]"
disable-model-invocation: true
---

# Plan the day

Prepare today's plan for the director. Keep the briefing short; the director approves, edits
or rejects it.

## 1. Gather the facts

- `TASKS.md` and the cards in `docs/tasks/` (statuses and dependencies).
- The roadmap for today in `docs/designs/cairn.md` (day $ARGUMENTS if given).
- Open pull requests: `gh pr list --state open`.
- CI on `main`: `gh run list --branch main --limit 3`.
- Yesterday's entries in `docs/ai-workflow.md`.

## 2. Decide

- Anything red on `main` or a pull request waiting on the director comes first.
- Order tasks so dependencies finish first and the riskiest task starts in the morning.
- Aim for 3 to 6 tasks a day. If the roadmap needs more, say which items move to the cut list
  in the design (first: Ask Cairn, then red-flag scanner, then dental and vision parsing).
- Create any missing cards with the `task-card` skill (status `DRAFT`).

## 3. Present

```
## Plan for <date>
Yesterday: <one line: what merged, what is waiting>
Blockers: <none, or each with what you need from the director>

| Order | Task | Agent | Size | Depends on | Main risk |
| --- | --- | --- | --- | --- | --- |
| 1 | T-NNN <title> | sde2-implementer | M | - | <risk> |

Decisions needed from you:
1. <question with options, or "none">

Cut-list status: <on track, or what you recommend cutting>
```

## 4. After approval

Set approved cards to `READY` and update `TASKS.md`. Do not start `/run-task` until the
director approves the plan.
