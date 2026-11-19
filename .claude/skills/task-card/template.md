---
id: T-NNN
title: <imperative summary, for example "Parse cost-sharing phrases into typed rules">
status: DRAFT            # DRAFT, READY, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED
complexity: medium       # low, medium, high
risk: medium             # low, medium, high
agent: sde1-implementer  # sde2-implementer, sde1-implementer, junior-implementer
modules: [normalize]
branch: feat/T-NNN-<slug>
depends_on: []           # task ids that must be DONE first
design_refs: ["docs/designs/cairn.md#<section>"]
threats: []              # threat ids from docs/threat-model.md, or empty
---

# T-NNN: <title>

## Goal

<One or two sentences: what this delivers and why it matters to a user.>

## Context

<What already exists that this builds on, with file paths. What the implementer needs to know
that is not obvious from the code.>

## Acceptance criteria

1. Given <state>, When <action>, Then <observable result with concrete values>.
2. ...

## Out of scope

- <tempting extra that belongs to another card, with that card's id if known>

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `src/main/java/app/cairn/<module>/<Type>.java` | <public API type> |
| create | `src/main/java/app/cairn/<module>/internal/<Impl>.java` | <implementation> |
| create | `src/test/java/app/cairn/<module>/<Type>Test.java` | <tests> |
| modify | `<path>` | <change> |

## Public API

```java
// Signatures only. Javadoc in the code, not here.
public interface <Type> {
  <ReturnType> <method>(<ParamType> param);
}
public record <Value>(<fields>) {}
```

## Data

- Tables and migrations: <none, or schema.table with columns and the migration file name in
  db/migration/<module>/>
- Events published or consumed: <none, or names>

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| `<Type>Test#<behavior>` | unit | 1, 2 |
| `<Type>Properties#<invariant>` | property | invariant: <statement> |
| `<Type>GoldenTest` | golden | fixture: <file> |

## Security and privacy

- Trust boundaries touched: <none, or list>
- Threats: <ids and how this task addresses them>
- Personal data: <none, or what is stored and where>

## Gates

All gates in `/verify-gates` must pass. Coverage for `<module>` must meet the threshold in
`.claude/rules/testing.md`.

## Definition of done

- [ ] Every acceptance criterion has a passing test
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] CI green on the pull request
- [ ] Director merged the pull request

## Open questions

- <none, or each question for the director>

## Notes

<Anything else: references, fixture sources, decisions taken while writing the card.>
