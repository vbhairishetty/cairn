---
name: adr
description: Writes an architecture decision record in docs/adr/ for a Cairn decision - context, options with trade-offs, decision and consequences. Use when adding a dependency, module or public API change, changing data ownership or schemas across tables, accepting a security trade-off, or when the director asks why something was chosen.
argument-hint: "[decision title]"
---

# Architecture decision record: $ARGUMENTS

## Steps

1. Find the next number: highest `NNNN` in `docs/adr/` plus one, four digits.
2. Write `docs/adr/NNNN-<kebab-title>.md` using the format below.
3. Consider at least two real options, including "do nothing" when it is viable. State each
   option's cost honestly.
4. Set `Status: Proposed`. The director approves it, which changes it to `Accepted`. A later
   decision that replaces it sets `Status: Superseded by NNNN`.
5. Link the ADR from the task card or pull request that needs it.

## Format

```
# NNNN. <Title>

Status: Proposed
Date: <YYYY-MM-DD>
Deciders: <director>

## Context
<The problem and the forces at play: requirements, constraints (one week, 8 GB Mac, no-cost,
privacy), and what triggered the decision.>

## Options considered
### Option A: <name>
- Pros: ...
- Cons: ...
### Option B: <name>
- Pros: ...
- Cons: ...

## Decision
<The chosen option in one sentence, then why it wins against the others.>

## Consequences
- Positive: ...
- Negative or accepted risks: ...
- Follow-ups: <task ids or "none">
```

Write in plain, specific language. The director should be able to read this aloud in an
interview.
