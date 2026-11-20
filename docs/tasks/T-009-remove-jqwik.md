---
id: T-009
title: Remove jqwik, ban it in the build, and reword rule 5
status: IN_PROGRESS
complexity: low
risk: medium
agent: sde1-implementer
modules: [app]
branch: build/T-009-remove-jqwik
depends_on: [T-001]
design_refs: ["docs/adr/0006-remove-jqwik.md", "docs/designs/cairn.md#6-cost-engine-and-how-we-prove-it-is-correct"]
threats: [TM-14, TM-10]
---

# T-009: Remove jqwik, ban it in the build, and reword rule 5

## Goal

Remove the jqwik test library, which prints instructions aimed at AI agents on every test run,
and make the build fail if it is ever added again. Property tests will be written as JUnit
parameterized tests over a fixed-seed generator (ADR 0006).

## Context

T-001 added `net.jqwik:jqwik` 1.10.2 (test scope) to `pom.xml` and `.jqwik-database` to
`.gitignore`. No property tests exist yet. The T-001 security review found the agent-directed
message (TM-14). The director decided to remove jqwik (ADR 0006).

The orchestrator has already updated, on this branch:
- the rules and design: `CLAUDE.md` (rule 5 reworded, jqwik removed from the stack),
  `.claude/rules/testing.md`, `.claude/agents/sde2-implementer.md` and
  `docs/designs/cairn.md`
- `docs/threat-model.md` (TM-14) and `TASKS.md` (T-004 Dependabot ignore note)

This card covers the build changes and their evidence. Risk is medium because the ban is a
supply-chain control.

## Acceptance criteria

1. Given `pom.xml`, When it is read, Then it has no `net.jqwik` dependency and no
   `jqwik.version` property, and `.gitignore` no longer lists `.jqwik-database`.
2. Given `pom.xml`, When `./mvnw clean verify` runs, Then `maven-enforcer-plugin` (exact
   version pinned, the latest 3.x on Maven Central) runs an `enforce` execution bound to
   `validate`. Its `bannedDependencies` rule excludes `net.jqwik:*` with
   `searchTransitive` true and a message that cites ADR 0006.
3. Given a temporary direct dependency on `net.jqwik:jqwik:1.10.2` (test scope) added to the
   pom, When `./mvnw clean verify` runs, Then the build fails in the Enforcer step and names
   the banned artifact. *(Evidence in the report; the change is reverted, not committed.)*
4. Given `BuildConfigurationTest`, When `./mvnw test` runs, Then a new test
   `buildBansJqwik` passes. It parses `pom.xml` with the JDK's XML parser (no new
   dependency) and asserts that an Enforcer `bannedDependencies` rule lists `net.jqwik:*` in
   its `excludes`. Test first: it fails before the Enforcer configuration exists.
5. Given the full test run, When `./mvnw clean verify` runs, Then the output contains no line
   from jqwik, and all gates pass.

## Out of scope

- The Dependabot ignore rule (T-004 creates `.github/dependabot.yml`; recorded in `TASKS.md`).
- Writing any property test or input generator (engine cards, Day 4).
- Other Enforcer rules (Java or Maven version, dependency convergence). These are follow-ups if
  wanted.
- The rule, design and threat-model edits already made by the orchestrator.

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| modify | `pom.xml` | remove jqwik; add `maven-enforcer-plugin` with the ban |
| modify | `.gitignore` | remove `.jqwik-database` |
| modify | `src/test/java/app/cairn/BuildConfigurationTest.java` | add `buildBansJqwik` |

## Public API

none.

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| `BuildConfigurationTest#buildBansJqwik` | unit (reads `pom.xml`) | 2, 4 |
| Temporary jqwik dependency makes `clean verify` fail in Enforcer (not committed) | build | 3 |
| `./mvnw clean verify` output has no jqwik line | build | 1, 5 |

## Security and privacy

- Trust boundaries touched: dependencies and build output read by agents.
- Threats: TM-14 (the agent-directed text goes away, and the ban prevents its return). TM-10
  (the Enforcer version is pinned exactly).
- Personal data: none.

## Gates

All gates in `/verify-gates` must pass (gate 4 is `./mvnw clean verify`).

## Definition of done

- [ ] Every acceptance criterion has a passing test or recorded evidence
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] Director merged the pull request

## Open questions

- none

## Notes

- The pull request also carries the T-008 close-out commit and the orchestrator's doc edits.
