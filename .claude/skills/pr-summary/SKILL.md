---
name: pr-summary
description: Writes the pull request description for a Cairn task in the voice of a senior engineer writing for a teammate - What, Key changes, Testing (or Review for docs), Risks, and How to try it for user-facing changes - in 100 to 200 words that link to the repo for detail. Use when opening or updating a pull request.
argument-hint: "[task-id]"
---

# Pull request summary for $ARGUMENTS

Write the description the way a senior engineer writes one for a teammate: short, specific,
and honest. The reader should be able to decide whether to merge in two minutes and know
where to look for more. Every claim must come from a command you ran or a report you
verified.

## Gather

1. The task card in `docs/tasks/`.
2. `git log main..HEAD --oneline` and `git diff main...HEAD --stat`.
3. Your latest `verify-gates` results.
4. The final implementer, review and security reports, including every finding and how it
   was resolved.

## Write

Save to `target/pr-summary-<task-id>.md` (git-ignored) and use it as the body. Use these
sections, in this order:

```
## What
<1 to 2 sentences: what this change does and why.>

## Key changes
- <the 3 to 6 changes that matter, one line each>

## Testing
- <commands run and results with real numbers: tests run, coverage %, gate status>
- <review outcomes: independent review and security review, rounds, notable catches>
- <CI link or "pending">

## Risks
- <each accepted risk, open finding or known limitation, one line each, or "None.">

## How to try it
<only for user-facing changes: the commands or steps>

Refs: <task id>, <card path>
```

- **Docs-only changes:** replace **Testing** with **Review**: who or what reviewed it, how many
  rounds, and the outcome.
- **How to try it:** include it only when the change has behavior a user can see (UI, API,
  CLI). Omit it for build, config, docs and internal refactors.

## Style rules

- 100 to 200 words in total, excluding the Refs line. If it runs longer, move the detail into
  the repo and link to it.
- Short bullets, one line each. No sub-bullets unless a list truly cannot be flattened.
- Write as the author ("Adds", "Moves", "I chose X because Y" only where a reason is needed).
  Never address the reader as "you".
- Detail lives in the repo: the design doc, ADRs, task cards and `docs/ai-workflow.md`.
  Link to them instead of repeating them. Use links that survive the merge (commit SHA or
  `main` once merged).
- Keep every honest caveat and accepted risk, each in one line. Brevity never removes a
  caveat.
- Do not overstate: if coverage is 78%, write 78%, not "about 80%". If anything is pending
  or failing, say so in the What section.
- No AI credit lines, no "Generated with", no emoji.
