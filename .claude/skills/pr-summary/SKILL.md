---
name: pr-summary
description: Writes the pull request description for a Cairn task so the director can decide to merge in a few minutes - plain-language summary, what changed, verification evidence, review and security findings with resolutions, risks, and how to try it. Use when opening or updating a pull request.
argument-hint: "[task-id]"
---

# Pull request summary for $ARGUMENTS

The director reads this to decide whether to merge. They do not write code, so lead with
plain language, then give the evidence an engineer would want. Every claim must come from a
command you ran or a report you verified.

## Gather

1. The task card in `docs/tasks/`.
2. `git log main..HEAD --oneline` and `git diff main...HEAD --stat`.
3. Your latest `verify-gates` results.
4. The final implementer, review and security reports, including every finding from every
   round and how it was resolved.

## Write

Save to `target/pr-summary-<task-id>.md` (git-ignored) and use it as the body. Use exactly
these sections:

```
## In plain words
<2 to 4 sentences a non-programmer understands: what this adds and why a Cairn user cares.>

## What changed
- <module>: <what and why, one line each>

## How it was verified
| Check | Result |
| --- | --- |
| Format, Error Prone, tests, Modulith, coverage | <PASS with key numbers: tests run, coverage %> |
| Independent review | <APPROVE after N rounds> |
| Security review | <PASS> |
| CI | <link or "pending"> |

## What the reviews caught
- <finding id, severity, one line, and the fix with its test>
- <"none" only if true>

## Accepted risks and limitations
- <each accepted Medium finding with its reason, known simplifications, or "none">

## How to try it
<commands or steps, for example "docker compose up, then open http://localhost:8080/compare">

## Task
<task id and card path>. Design: <design refs>.
```

## Rules

- No AI credit lines, no "Generated with", no emoji.
- Do not overstate: if coverage is 78%, write 78%, not "about 80%".
- If anything is pending or failing, say so at the top in one sentence.
