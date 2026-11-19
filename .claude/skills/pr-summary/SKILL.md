---
name: pr-summary
description: Writes the pull request description for a Cairn task as a senior engineer would for a teammate - What, Key changes, Testing (Review for docs-only changes), Risks, and How to try it for user-facing changes - in 80 to 200 words, including only what helps decide whether to merge and linking to the repo for detail. Use when opening or updating a pull request.
argument-hint: "[task-id]"
---

# Pull request summary for $ARGUMENTS

Write like a senior engineer writing for a teammate: short, plain, clean. Include only what
helps someone decide whether to merge this change. If a line would not affect that decision,
leave it out. Every claim must come from a command you ran or a report you verified.

## Gather

1. The task card in `docs/tasks/`.
2. `git log main..HEAD --oneline` and `git diff main...HEAD --stat`.
3. Your latest `verify-gates` results.
4. The final implementer, review and security reports.

## Write

Save to `target/pr-summary-<task-id>.md` (git-ignored) and use it as the body. Use these
sections in this order, and include a section only when it has content:

```
## What
<One or two sentences: what the change does and why.>

## Key changes
- <3 to 6 short bullets, no sub-bullets>

## Testing
<What was run and the result, in one or two lines.>

## Risks
- <Only risks this change introduces, one line each. Omit the section if there are none.>

## How to try it
<Only for user-facing changes: the commands or steps.>
```

For docs-only changes, use `## Review` instead of `## Testing`, with the review result.

## Rules

- Target 80 to 200 words.
- Never address the reader as "you". Write as the author.
- Never repeat detail that lives in the repo. Link to the design doc, an ADR or
  `docs/ai-workflow.md` instead, with links that survive the merge (a commit SHA, or `main`
  once merged).
- Keep honest caveats and accepted risks, one line each. Brevity never removes a caveat that
  affects the merge decision.
- Do not overstate: if coverage is 78%, write 78%. If anything is pending or failing, say so
  in What.
- No AI credit lines, no emoji, no headings beyond the sections above.
