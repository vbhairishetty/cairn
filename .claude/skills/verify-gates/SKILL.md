---
name: verify-gates
description: Runs every Cairn quality gate (format check, compile with Error Prone, unit, property, golden, integration, API, architecture and security tests, coverage) and returns a PASS or FAIL table with evidence. Use before reporting a task as done, when reviewing a branch, or when someone asks whether the build is green.
allowed-tools: Bash(./mvnw *) Bash(git status *) Bash(git branch *) Bash(git diff *) Bash(git log *) Bash(docker compose ps *) Bash(docker info *) Read Grep Glob
---

# Verify gates

Run the gates in order and report the real results. Do not skip a gate because an earlier
one failed; run them all so the report is complete, unless the build cannot compile.

## Preconditions

1. `git branch --show-current` and `git status --porcelain`: record the branch and whether
   the tree is clean. Uncommitted changes are reported, not hidden.
2. `docker info` must succeed for integration tests (Testcontainers needs Docker; on the
   director's Mac this is Colima). If Docker is not running, report the integration gate as
   `NOT RUN: Docker unavailable` instead of passing it.

## Gates

| # | Gate | Command | Pass condition |
| --- | --- | --- | --- |
| 1 | Format | `./mvnw -q spotless:check` | exit code 0 |
| 2 | Compile and static checks | `./mvnw -q test-compile` | exit code 0, no Error Prone errors |
| 3 | Unit, property, golden, architecture and security tests | `./mvnw -q test` | exit code 0, 0 failures, 0 errors |
| 4 | Integration and API tests, coverage | `./mvnw -q verify` | exit code 0, Failsafe 0 failures, JaCoCo check passed |
| 5 | Commit hygiene | `git log main..HEAD --format=%B` | no line matching `Co-Authored-By`, `Generated with`, or naming an AI tool; no emoji |

Gate 4 re-runs earlier phases; that is expected. If the project does not have a gate yet
(for example JaCoCo is configured in a later task), report it as `NOT CONFIGURED` with the
reason. Never report it as passing.

## Reading results

- Surefire and Failsafe summaries look like `Tests run: N, Failures: F, Errors: E, Skipped: S`.
  Quote the final summary line for each.
- On failure, open the report in `target/surefire-reports/` or `target/failsafe-reports/` and
  quote the first failing test name and the assertion message (no personal data).
- Skipped tests are reported with their count. A newly skipped test is a finding.

## Rules

- Never change thresholds, exclusions, plugin configuration or tests to make a gate pass.
- Never run gates with flags that skip checks (`-DskipTests`, `-Dspotless.check.skip`,
  `-Djacoco.skip`, `-DskipITs`). A project hook blocks them.
- If a gate is slow, let it finish.

## Output

```
## Gate results
branch: <name>   clean tree: <yes/no>   docker: <available/unavailable>
| Gate | Result | Evidence |
| --- | --- | --- |
| 1 Format | PASS/FAIL | <line> |
| 2 Compile and static checks | PASS/FAIL | <line> |
| 3 Tests | PASS/FAIL | Tests run: ..., Failures: ..., Errors: ..., Skipped: ... |
| 4 Integration and coverage | PASS/FAIL/NOT RUN/NOT CONFIGURED | <line> |
| 5 Commit hygiene | PASS/FAIL | <offending line or "clean"> |
overall: PASS only if every gate is PASS
first failure: <test or check, and message>
```
