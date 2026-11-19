---
name: walkthrough
description: Evening teaching session for the Cairn director - explains in plain language what was built today and why, how the code works, what reviewers caught and what was rejected, then checks understanding with teach-back questions and an optional find-the-bug drill. Use at the end of a work day or when the director asks to understand a merged change.
argument-hint: "[task-id or 'today']"
disable-model-invocation: true
---

# Walkthrough

The director does not write code but must be able to explain every part of Cairn in an
interview: what it does, why it was built that way, what alternatives were rejected, and how
it was verified. Teach, do not lecture. Keep each explanation short and concrete.

## Scope

- `today`: every task merged today (`gh pr list --state merged --search "merged:>=<today>"`).
- A task id: that task only.

## For each task

1. **The problem in one sentence**, from the user's point of view.
2. **The design in plain words:** which module, what goes in, what comes out, and where it sits
   in the architecture diagram.
3. **The code tour:** 3 to 5 key places, each as `file:line` with a one-sentence explanation.
   Show at most 15 lines of code at a time, and explain every line in that excerpt in plain
   words, including any command used to build or test it.
4. **Why this way:** the main decision, the alternative rejected, and the reason (link the ADR
   if there is one).
5. **How we know it works:** the tests that prove it, in plain words ("this test checks that a
   $30 copay is charged even before the deductible is met").
6. **What the reviewers caught:** the most interesting finding and how it was fixed. This is
   interview material.

## Teach-back

Ask the director three questions, one at a time, and wait for each answer:

1. A "what" question: "What does the normalize module produce?"
2. A "why" question: "Why do we store money as BigDecimal?"
3. A "what if" question: "What happens if an SBC says 'Not covered' for specialists?"

Correct gently and precisely. Note any topic the director found hard and revisit it tomorrow.

## Find-the-bug drill (two or three times a week)

1. Take a real excerpt (10 to 20 lines) from today's merged code.
2. Introduce one realistic bug **in the chat only** (for example `>` instead of `>=` on a
   deductible boundary, or `equals` on `BigDecimal`). Never change files, never commit, never
   create a branch for the drill.
3. Ask the director to find it and explain the user impact, with up to two hints.
4. Show the correct version and the test that would catch the bug.

## Close

End with three lines for the director's dev log: one decision and why, one bug caught and
how, and one thing rejected from an agent and why.
