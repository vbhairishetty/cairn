---
id: T-011
title: Reuse a verified Maven distribution in CI instead of downloading it every job
status: IN_PROGRESS
complexity: medium
risk: medium
agent: sde1-implementer
modules: [ci]
branch: ci/T-011-cache-verified-maven
depends_on: [T-010]
design_refs: [".claude/rules/git-and-delivery.md", ".claude/rules/testing.md#flaky-tests"]
threats: [TM-10, TM-15]
---

# T-011: Reuse a verified Maven distribution in CI instead of downloading it every job

## Goal

Stop CI failing when Maven Central rate-limits the Maven download, by keeping a copy of the
Maven distribution zip in the CI cache and checking its SHA-256 before every use, so a normal
run downloads nothing and a tampered copy still fails the job.

## Context

T-010 (PR #11, #12) made every Maven job (`build`, `jqwik-ban`, `codeql`) delete
`~/.m2/wrapper/dists` and download `apache-maven-3.9.16-bin.zip` again, because the wrapper
(3.3.4, `only-script`) checks `distributionSha256Sum` only when it downloads, never for a copy
that is already extracted. That is three downloads from `repo.maven.apache.org` per CI run.

On 2026-11-25 Maven Central answered **429 Too Many Requests** to every attempt for the whole
retry window (about 4 minutes) in three jobs across two runs on PR #14 (runs 45702725334,
45702725070, 45705952726). The tests never ran. Retrying while rate-limited does not help, so
the fix is to stop downloading on normal runs.

The security property from T-010 must survive: Maven only ever runs from a distribution whose
zip matched the pinned `distributionSha256Sum` in `.mvn/wrapper/maven-wrapper.properties`.
The pinned value is the single source of truth; the script reads it from that file.

Facts to verify before choosing the mechanism (record what was found in the report):
- `actions/setup-java` v6.3.0 with `cache: maven` caches `~/.m2/repository`, keyed on the
  hash of `**/pom.xml`, and saves only on a cache miss. A zip stored under
  `~/.m2/repository/org/apache/maven/apache-maven/3.9.16/` (its normal repository path) would
  ride that cache with no new action.
- The wrapper downloads with `wget` when present (no `file://` support) and otherwise `curl`.
  Getting the wrapper to install from a local verified zip may need a different route.

Recommended mechanism (the implementer may propose a simpler one that meets the criteria):
the script keeps the verified zip in the cached repository path, deletes
`~/.m2/wrapper/dists` as today, and installs Maven from the verified zip so the wrapper finds
it, without running anything from an unverified extracted copy. If the only working route
needs a new action (for example `actions/cache`), stop and return NEEDS_DECISION.

Workflow files change only through a card that says so. This card does, and the orchestrator
asks the director before pushing.

**Round 1 finding and director decision (2026-11-26):** `setup-java` v6.3.0 saves its Maven
cache only when the key (a hash of `pom.xml`, the wrapper properties and
`.mvn/extensions.xml`) was not restored, and `main` already holds an entry for the current key.
So a zip placed under `~/.m2/repository` would never be saved until `pom.xml` changes. The
director approved one new action: `actions/cache`, pinned to a full commit SHA with the
version in a comment, used only for the Maven zip in a dedicated directory (not inside
`~/.m2`), with a key derived from `.mvn/wrapper/maven-wrapper.properties`. Criterion 5's
"no new action" is amended to allow exactly this one. A poisoned cache entry cannot run,
because the script verifies the zip against the pinned SHA-256 and fails on a mismatch.

## Acceptance criteria

1. Given a cached zip whose SHA-256 equals `distributionSha256Sum`, When a Maven job runs,
   Then it makes no request to Maven Central for the distribution, prints
   `Using cached Maven distribution (SHA-256 verified)`, and `./mvnw -v` reports Maven 3.9.16.
2. Given a cached zip whose SHA-256 differs, When the job runs, Then it fails at once with
   `FAIL: cached Maven distribution checksum mismatch`, with no retry and no fallback download,
   so a tampering signal is never turned into a green run.
3. Given no cached zip, When the job runs, Then it downloads the zip with the existing retry
   (5 attempts, 15/30/60/120 s waits), verifies the SHA-256 before use (a mismatch fails at
   once), stores it in the cached path, and prints `Maven download ok on attempt N`.
4. Given any run, When Maven starts, Then nothing under `~/.m2/wrapper/dists` that existed
   before the step is ever executed: it is deleted first, as in T-010, and Maven is installed
   from the verified zip.
5. Given the step, When it is read, Then it uses no `${{ }}` inside `run:`, no new action other
   than the approved SHA-pinned `actions/cache`, no secrets and no permission change, and the logic lives in `scripts/ci/` (the T-010 script
   is changed or replaced; no copy-paste across jobs).
6. Given the pull request, When CI runs twice (the second run after the first has saved the
   cache), Then `build`, `jqwik-ban`, `codeql` and `dependency-review` are green, and the second
   run's Maven job logs show criterion 1's line and no distribution download.

## Out of scope

- Caching or verifying dependency jars in `~/.m2/repository` (accepted risk, TM-15).
- Upgrading Maven or the wrapper.
- Mirrors or a different download host.

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| modify | `scripts/ci/maven-download-with-retry.sh` (or a renamed successor) | verify-or-download the zip, install from it |
| modify | `.github/workflows/ci.yml` | `actions/cache` step for the Maven zip before the script, in both jobs |
| modify | `.github/workflows/codeql.yml` | the same `actions/cache` step before the script |
| modify | `docs/threat-model.md` | TM-15 defense and test text for the cached, verified zip |

## Public API

none.

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| Stub cache with a correct zip: no network call (stubbed `wget`/`curl` that fail if called), success line printed | local script test | 1 |
| Stub cache with a modified zip: fails once with the mismatch line, download stub never called | local script test | 2 |
| Empty cache, download stub fails 4 times then serves the real zip: success on attempt 5; a download that serves wrong bytes fails at once | local script test | 3 |
| Pre-populated `wrapper/dists/.../bin/mvn` that prints a marker is deleted and never run | local script test | 4 |
| actionlint (checksum-verified temporary binary) and a read-through of permissions | review | 5 |
| Two CI runs on the pull request, logs quoted | CI | 6 |

Local tests set `MAVEN_USER_HOME` and the cache path to temporary directories and are not
committed (as in T-010), unless the implementer proposes a committed test script.

## Security and privacy

- Trust boundaries touched: CI runners, the CI cache, the Maven Central download.
- Threats: TM-10, TM-15. The distribution is verified against the pinned SHA-256 on every use,
  cached or downloaded. Dependency jars stay an accepted risk.
- Secrets: none. Personal data: none.

## Gates

All gates in `/verify-gates` pass locally, and all CI checks pass on the pull request.

## Definition of done

- [ ] Every acceptance criterion has evidence
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] Director approved pushing the workflow change
- [ ] CI green on the pull request, twice
- [ ] Director merged the pull request

## Open questions

- none, unless the implementer finds that no route works without a new action (then
  NEEDS_DECISION to the director).
