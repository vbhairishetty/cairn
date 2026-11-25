---
id: T-010
title: Retry the Maven wrapper download in CI
status: DONE
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
does **not** re-check a distribution that is already under `~/.m2/wrapper/dists`.

**Correction (2026-11-24, review of this card):** the first version of this card said that
folder was not cached across runs. That was wrong. The pinned `actions/setup-java` v6.3.0 with
`cache: maven` also caches `~/.m2/wrapper/dists` as a separate `maven-wrapper` cache. The
`main` runs log "maven-wrapper cache restored", and the wrapper runs that copy without checking
it again. The director decided (2026-11-24) that the retry script deletes `~/.m2/wrapper/dists`
before its first attempt, so every job downloads Maven and verifies its checksum. The retry
covers the extra downloads. The `~/.m2/repository` cache stays and is recorded as an accepted
risk (TM-15). The director approved fixing the flaky download (2026-11-24). `.claude/rules/testing.md` treats an intermittent failure as
a bug to fix, not to re-run.

Workflow files change only through a card that says so. This card does, and the orchestrator
asks the director before pushing.

**Round 3 change (director, 2026-11-25):** after merge-ready CI, one `jqwik-ban` run
(45323210784) failed all 3 attempts within 30 seconds, while the other jobs in the same run
downloaded Maven. A re-run passed. Deleting the cache means every job now downloads Maven, so
the retry window was widened to 5 attempts over about 4 minutes, with the wrapper's real
download error shown in the log.

## Acceptance criteria

1. Given `ci.yml` (`build`, `jqwik-ban`) and `codeql.yml` (`codeql`), When a job runs, Then
   before its first Maven command a step named `Download Maven (with retry)` first deletes
   `${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists`, then runs `./mvnw -B -v` up to 5 times, waiting
   15, 30, 60 and 120 seconds between attempts (about 4 minutes of waiting; each attempt is
   bounded only by the job timeout). It stops at the first
   success and fails the job if all 5 attempts fail. The wrapper runs in verbose mode inside
   the script only, so a failed download shows its real error in the log. If an
   attempt reports a checksum mismatch ("Failed to validate Maven distribution SHA-256"), the
   script fails at once with no retry, so a tampering signal is never turned into a green run.
   In `codeql` the step runs before `Initialize CodeQL`.
2. Given the retry step, When it is read, Then it uses no `${{ }}` expressions inside `run:`, no
   new action, no secrets and no change to permissions. It lives in one script,
   `scripts/ci/maven-download-with-retry.sh`, called from each job, so the logic is not copied
   three times.
3. Given the script, When the download succeeds on attempt N, Then it prints
   `Maven download ok on attempt N`. When all attempts fail, it prints
   `FAIL: Maven download failed 5 times` and exits non-zero.
4. Given a local test with a fake failing `mvnw` (a stub in a temporary directory that fails
   four times and then succeeds), When the script runs against it, Then it reports success on
   attempt 5. With a stub that always fails, it exits non-zero after 5 attempts. *(Evidence in
   the report; the stubs are not committed. The script takes the wrapper path as an optional
   argument so it can be tested; it defaults to `./mvnw`.)*
5. Given the pull request, When CI runs, Then `build`, `jqwik-ban`, `codeql` and
   `dependency-review` are green, and the log of each Maven job (`build`, `jqwik-ban`,
   `codeql`) shows
   `Deleted cached Maven distributions under ...`, then the wrapper's verbose line
   `Couldn't find MAVEN_HOME, downloading and installing it`, then `Maven download ok on
   attempt 1`. The verbose line appears only when no cached copy was used, so it is the
   evidence.

## Out of scope

- Changing `setup-java` caching (the director kept `cache: maven`; see TM-15).
- Upgrading Maven (Dependabot PR #8 was closed; a deliberate upgrade card later).
- Retrying Maven dependency downloads during the build (Maven has its own resolver retries).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `scripts/ci/maven-download-with-retry.sh` | download Maven through the wrapper with up to 5 attempts |
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
| Stub `mvnw` that fails four times then succeeds; stub that always fails (not committed) | local script test | 3, 4 |
| A pre-populated `wrapper/dists/.../bin/mvn` that prints a marker is deleted and never run (local runs set `MAVEN_USER_HOME` to a temporary directory) | local script test | 1 |
| A stub that prints the checksum-mismatch line exits non-zero after exactly one attempt | local script test | 1 |
| CI run on the PR, each job showing attempt 1 | CI | 1, 5 |
| actionlint (checksum-verified temporary binary) and a read-through of permissions | review | 2 |

## Security and privacy

- Trust boundaries touched: CI runners, the Maven Central download.
- Threats: TM-10 and TM-15. The wrapper verifies the pinned SHA-256 on every job's download,
  because the cached distribution is deleted first. The cached `~/.m2/repository` is an
  accepted risk recorded in TM-15.
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
