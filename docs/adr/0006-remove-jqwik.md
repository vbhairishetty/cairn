# 0006. Remove jqwik and write property tests as seeded parameterized tests

Status: Accepted
Date: 2026-11-20
Deciders: Director

## Context

The design relies on property-based tests for the cost engine: cost never exceeds premiums
plus the out-of-pocket maximum, more usage never costs less, in-network preventive care is
free, and results are deterministic. T-001 added jqwik 1.10.2 for this.

The T-001 security review found that jqwik 1.10.x prints, on every test run, a message
addressed to AI coding agents. It tells them to stop using the library and to ignore jqwik
test results. The message is a deliberate feature of the official release, switched by a
jqwik setting, not a tampered artifact. Cairn is built by AI agents that read build output. A
dependency that tries to steer agents away from test results is a supply-chain risk to the
gates (TM-14), even though no agent acted on it, and even though gate verdicts come from exit
codes and report counts. A future release could carry more targeted text.

No property tests exist yet, so removing jqwik now costs only a pom change and new guidance.

## Options considered

### Option A: Keep jqwik and treat the message as data
- Pros: Real shrinking and generators. No new test pattern.
- Cons: Every agent-run build keeps printing agent-directed instructions. We would depend on
  every agent ignoring them, and on the maintainer never changing the text.

### Option B: Keep jqwik and hide the message with its setting
- Pros: Clean output, real property testing.
- Cons: It silences the maintainer's own notice and keeps a dependency whose maintainer asks
  agents not to use it. The risk stays for the next release.

### Option C: Remove jqwik; use JUnit `@ParameterizedTest` fed by a fixed-seed random generator
- Pros: No extra dependency, since JUnit is already in the stack. Inputs are reproducible from a
  seed constant, and failures name the seed and case index.
- Cons: No automatic shrinking, so a failing case is reported as generated, not minimized. The
  generators for plans and usage are ours to write and test.

## Decision

Option C. Property tests are JUnit `@ParameterizedTest` methods whose `@MethodSource` draws a
fixed number of inputs (default 1,000) from `new SplittableRandom(SEED)`, with `SEED` a
constant in the test class. The seed and case index appear in each test's display name so a
failure can be reproduced exactly. The `*Properties.java` naming stays. jqwik is banned from
the build: the Maven Enforcer plugin's `bannedDependencies` rule fails `./mvnw clean verify` if
`net.jqwik` is added directly or transitively. The Dependabot configuration (T-004) ignores
`net.jqwik:*`.

## Consequences

- Positive: No dependency prints instructions to the agents running the build. Property tests
  are plain JUnit, deterministic and reproducible.
- Negative or accepted risks: No shrinking. Failing inputs can be larger than a minimal
  counterexample, so the engine card should keep generators small and readable. The input
  generators need their own unit tests.
- Follow-ups: T-009 removes jqwik, adds the Enforcer ban and updates the rules and design.
  T-004 adds the Dependabot ignore rule.
