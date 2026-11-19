# 0004. Let engine read reference data from rules

Status: Accepted
Date: 2026-11-19
Deciders: Director

## Context

Coverage-example reconciliation runs the engine on the standard claims that CMS publishes
for each SBC coverage example. The design review (Nov 19) placed reconciliation in `engine`,
because it needs the engine and `validate` cannot depend on `engine` without a cycle. Those
claims are public reference data with a source URL and an effective date, just like the
legal rules, so they belong in the `rules` data pack. The seed values for typical prices
(`price_assumption`) are the same kind of data. The dependency table lets `engine` use only
`common` and `validate`.

## Options considered

### Option A: `engine` may depend on `rules`
- Pros: All versioned, sourced reference data stays in one place with one loader, one
  startup validation and one `verified_on` review flag.
- Cons: Adds one dependency edge.

### Option B: Keep a separate copy of the claims and prices inside `engine`
- Pros: No new edge.
- Cons: A second YAML loader and a second validation path. Reference data would then be split
  across modules, so the 90-day review flag would have to be built twice.

### Option C: The caller (`web`) passes the claims in
- Pros: No new edge.
- Cons: Reconciliation runs in response to `PlanValidated`, which has no web caller. It would
  also push domain logic into the web layer.

## Decision

Option A. `engine` may depend on `rules`. The pure calculation core still takes everything as
arguments; only the engine's edge service (the event listener and repository) calls the
`rules` API. No cycle is created, because `rules` depends only on `common`.

## Consequences

- Positive: One home for reference data, and the pure engine stays pure.
- Negative or accepted risks: `engine` now depends on two modules.
- Follow-ups: Update the dependency table in `.claude/rules/architecture.md` (done with the
  design review edits, pending this ADR's approval).
