---
name: task-card
description: Writes a complete, testable Cairn task card in docs/tasks/ from a feature, a roadmap item or a design section, with acceptance criteria, files, public API signatures, tests, security notes and the right implementer. Use when planning work, when the director asks for a task, or before running /run-task on new work.
argument-hint: "[short task description or design section]"
---

# Write a task card

Create one task card for: $ARGUMENTS

A task card is the contract between the director, the orchestrator and the implementer. An
engineer who has never seen this conversation must be able to implement it, and a reviewer
must be able to judge it, from the card alone.

## Steps

1. Read the relevant sections of `docs/designs/cairn.md`, `TASKS.md`, existing cards in
   `docs/tasks/`, and the code in the modules involved.
2. Pick the next id: the highest `T-NNN` in `docs/tasks/` plus one, zero-padded to three
   digits.
3. Size it. A card is one pull request of roughly 100 to 400 changed lines that one agent can
   finish in one session. If it is bigger, split it into several cards with `depends_on`
   links, and say so.
4. Fill in [template.md](template.md) completely. No empty sections: write "none" where
   something truly does not apply.
5. Set `complexity` and `risk` honestly, then `agent` using the routing table in
   `/run-task`. Business logic, money, dates, parsing, validation and anything
   security-sensitive is at least `medium` risk.
6. Write acceptance criteria in Given / When / Then form, each one testable, with concrete
   values where the behavior is numeric (for example: Given a $1,500 deductible and a $1,000
   allowed amount, When the first claim is processed, Then member cost is $1,000.00).
7. List the threat ids from `docs/threat-model.md` the task touches, or "none".
8. Save as `docs/tasks/T-NNN-<slug>.md` with `status: DRAFT`, and add a line to `TASKS.md`.
9. For `risk: high` cards, run the `design-reviewer` subagent in `readiness` mode on the card
   and fix every High finding before presenting it.
10. Present the card to the director in five lines: goal, agent, size, risks, dependencies.
    The director's approval changes `status` to `READY`.

## Quality bar

- Specific: names files, classes, method signatures, tables and fixtures.
- Testable: every criterion maps to at least one test the card names.
- Bounded: "Out of scope" lists the tempting extras that belong to other cards.
- Honest: assumptions and open questions are written down, not hidden.
