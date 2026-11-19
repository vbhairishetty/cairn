---
name: design-review
description: Runs the Elephant-Goldfish style review of a Cairn design document or high-risk task card with fresh-context reviewers - comprehension check, adversarial review and implementation-readiness check - and consolidates the findings for the director. Use before implementation starts on a design or a risky card.
argument-hint: "[document path]"
disable-model-invocation: true
---

# Design review of $ARGUMENTS

The session that wrote a design (the Elephant) cannot judge it objectively. Fresh reviewers
with no shared context (Goldfish) can. This skill runs that review.

## 1. Prefer the official plugin

If the Elephant-Goldfish Model plugin is installed (its commands appear under `/egm:`), tell
the director to run `/egm:egm-review` on the document, because it implements the full review
protocol. Then continue at step 3 with its findings.

## 2. Otherwise, run fresh reviewers

Launch three separate `design-reviewer` subagents on the same document, one per mode, using
the design reviewer prompt in `.claude/skills/run-task/delegation-prompts.md`:

1. `MODE: comprehension`
2. `MODE: adversarial`
3. `MODE: readiness`

Run them independently. Do not pass one reviewer's output to another.

## 3. Consolidate

1. Compare the comprehension summary with the document's intent. Every misunderstanding is a
   place the document must be clearer.
2. Merge duplicate findings. Keep the highest severity.
3. For each finding, propose a specific edit to the document.
4. Present to the director:

```
## Design review: <document>
Readiness: GO | NO-GO
Misunderstood by a fresh reader: <list, or "none">

| Id | Severity | Section | Issue | Proposed edit |
| --- | --- | --- | --- | --- |

Decisions needed: <questions, or "none">
```

5. After the director agrees, apply the edits, and re-run the `readiness` reviewer until it
   returns `GO`. Record the review in `docs/ai-workflow.md` under "Design reviews".
