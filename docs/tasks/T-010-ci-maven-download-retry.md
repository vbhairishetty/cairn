---
id: T-010
title: Retry the Maven wrapper download in CI
status: IN_PROGRESS
complexity: low
risk: medium
agent: sde1-implementer
modules: [ci]
branch: ci/T-010-maven-download-retry
depends_on: [T-004]
design_refs: [".claude/rules/git-and-delivery.md", ".claude/rules/testing.md#flaky-tests"]
threats: [TM-10]
---

# T-010: Retry the Maven wrapper download in CI

## Goal

Stop CI failing when Maven Central briefly refuses the Maven download, without trusting an
unverified cached copy and without adding a new action.

## Context

On 2026-11-24 the `build` job on `main` (run 45296340292) failed before Maven started. The
log showed `wget: Failed to fetch .../apache-maven-3.9.16-bin.zip`, and a re-run passed. Every
job downloads Maven through `./mvnw` (wrapper 3.3.4, `only-script`), which verifies
`distributionSha256Sum` when it downloads. The T-001 security review noted that the wrapper
does **not** re-check a distribution that is already under `~/.m2/wrapper/dists`. Caching that
folder across runs would therefore reuse a copy that is never verified again, and would add a
third-party action. The director approved fixing the flaky download (2026-11-24). This card
uses a retry, which needs neither. `.claude/rules/testing.md` treats an intermittent failure as
a bug to fix, not to re-run.

Workflow files change only through a card that says so. This card does, and the orchestrator
asks the director before pushing.

## Acceptance criteria

1. Given `ci.yml` (`build`, `jqwik-ban`) and `codeql.yml` (`codeql`), When a job runs, Then
   before its first Maven command a step named `Download Maven (with retry)` runs `./mvnw -B -v`
   up to 3 times, 15 seconds apart. It stops at the first success and fails the job if all 3
   attempts fail.
2. Given the retry step, When it is read, Then it uses no `${{ }}` expressions inside `run:`, no
   new action, no secrets and no change to permissions. It lives in one script,
   `scripts/ci/maven-download-with-retry.sh`, called from each job, so the logic is not copied
   three times.
3. Given the script, When the download succeeds on attempt N, Then it prints
   `Maven download ok on attempt N`. When all attempts fail, it prints
   `FAIL: Maven download failed 3 times` and exits non-zero.
4. Given a local test with a fake failing `mvnw` (a stub in a temporary directory that fails
   twice and then succeeds), When the script runs against it, Then it reports success on
   attempt 3. With a stub that always fails, it exits non-zero after 3 attempts. *(Evidence in
   the report; the stubs are not committed. The script takes the wrapper path as an optional
   argument so it can be tested; it defaults to `./mvnw`.)*
5. Given the pull request, When CI runs, Then `build`, `jqwik-ban`, `codeql` and
   `dependency-review` are green, and each job's log shows
   `Maven download ok on attempt 1`.

## Out of scope

- Caching `~/.m2/wrapper` or `~/.m2/repository` beyond what `setup-java` already does
  (rejected above).
- Upgrading Maven (Dependabot PR #8 was closed; a deliberate upgrade card later).
- Retrying Maven dependency downloads during the build (Maven has its own resolver retries).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `scripts/ci/maven-download-with-retry.sh` | download Maven through the wrapper with up to 3 attempts |
| modify | `.github/workflows/ci.yml` | call the script before the first Maven command in both jobs |
| modify | `.github/workflows/codeql.yml` | call the script before the build step |

## Public API

none.

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| Stub `mvnw` that fails twice then succeeds; stub that always fails (not committed) | local script test | 3, 4 |
| CI run on the PR, each job showing attempt 1 | CI | 1, 5 |
| actionlint (checksum-verified temporary binary) and a read-through of permissions | review | 2 |

## Security and privacy

- Trust boundaries touched: CI runners, the Maven Central download.
- Threats: TM-10. The wrapper still verifies the pinned SHA-256 on every download, and nothing
  unverified is cached.
- Secrets: none. Personal data: none.

## Gates

All gates in `/verify-gates` pass locally, and all CI checks pass on the pull request.

## Definition of done

- [ ] Every acceptance criterion has evidence
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] Director approved pushing the workflow change
- [ ] CI green on the pull request
- [ ] Director merged the pull request

## Open questions

- none
