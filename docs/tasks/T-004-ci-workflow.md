---
id: T-004
title: Add CI, CodeQL, dependency review, Dependabot and the jqwik behavioral check
status: READY
complexity: medium
risk: medium
agent: sde1-implementer
modules: [ci]
branch: ci/T-004-ci-workflow
depends_on: [T-001, T-009]
design_refs: ["docs/designs/cairn.md#11-quality-and-security-gates", ".claude/rules/git-and-delivery.md", "docs/adr/0006-remove-jqwik.md"]
threats: [TM-10, TM-14]
---

# T-004: Add CI, CodeQL, dependency review, Dependabot and the jqwik behavioral check

## Goal

Run every gate on GitHub for each pull request so that merges depend on checks, not only on
local runs. Add the behavioral check that proves the jqwik ban works end to end, which closes
the Low gaps recorded in TM-14.

## Context

There is no `.github/` folder yet. The build is `./mvnw clean verify` (Spotless check, Error
Prone, Surefire, Failsafe, JaCoCo). The jqwik ban is the Maven Enforcer rule plus
`BuildGuard` (T-009). `TASKS.md` lists two notes this card must satisfy: the Dependabot ignore
rule and the behavioral check. Workflow files change only through a card that says so, and the
orchestrator asks the director before pushing (`.claude/rules/git-and-delivery.md`). This card
says so.

## Acceptance criteria

1. Given `.github/workflows/ci.yml`, When a pull request to `main` is opened or updated, or
   `main` is pushed, Then a job named `build` runs on `ubuntu-latest` with Temurin JDK 21
   (`actions/setup-java`, Maven cache) and runs `./mvnw -B clean verify`. The job fails if any
   gate fails.
2. Given the same workflow, When it runs, Then a job named `jqwik-ban` runs
   `scripts/ci/jqwik-ban-check.sh`, and the job fails unless both checks pass:
   - **(a)** It makes a `git archive` copy of the commit in a temporary directory, adds a
     direct `net.jqwik:jqwik:1.10.2` test dependency to the copy's `pom.xml`, and runs
     `./mvnw -B validate` with `MAVEN_OPTS` empty and no extra `-D` flags. That run must
     **fail**, and its output must contain `net.jqwik is banned`.
   - **(b)** It runs `./mvnw -B test` on the real checkout. That run must pass, and its output
     must not contain "jqwik" (any case) except inside the class name `JqwikBanSecurityTest`.
3. Given `.github/workflows/codeql.yml`, When a pull request to `main` is opened, `main` is
   pushed, or the weekly schedule fires, Then CodeQL analyses `java-kotlin` with a manual
   build step, `./mvnw -B clean compile`, and uploads results.
4. Given `.github/workflows/dependency-review.yml`, When a pull request is opened, Then
   `actions/dependency-review-action` runs and fails on High or Critical vulnerabilities.
5. Given `.github/dependabot.yml`, When it is read, Then it has weekly updates for `maven` and
   `github-actions`, and the `maven` entry ignores `net.jqwik:*` (ADR 0006).
6. Given every workflow, When it is read, Then:
   - top-level `permissions` is `contents: read`, with any wider permission granted per job
     (CodeQL needs `security-events: write`)
   - every third-party action is pinned to a full commit SHA, with the version in a comment
   - no secrets are used
   - nothing writes to the repository
7. Given the pull request for this card, When CI runs on it, Then `build`, `jqwik-ban`,
   `codeql` and `dependency-review` are green. The PR links the runs.
8. Given `scripts/ci/jqwik-ban-check.sh`, When run locally on the Mac from the repo root with
   an isolated local repository, Then both checks pass and the temporary directory is removed
   on exit (trap).

## Out of scope

- Trivy image scan and the release workflow (Day 7, once a Dockerfile exists).
- The end-to-end smoke test (once the app runs).
- Branch protection and required status checks. Setting them changes GitHub settings, so the
  director does it or approves it separately after this merges.
- Rule Watcher workflow (cut list item 4 if needed; its own card).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `.github/workflows/ci.yml` | `build` and `jqwik-ban` jobs |
| create | `.github/workflows/codeql.yml` | CodeQL for Java |
| create | `.github/workflows/dependency-review.yml` | dependency review on pull requests |
| create | `.github/dependabot.yml` | weekly updates; ignore `net.jqwik:*` |
| create | `scripts/ci/jqwik-ban-check.sh` | behavioral jqwik check (both parts of criterion 2) |
| modify | `docs/threat-model.md` | TM-14 status to `tested` once `jqwik-ban` is green; TM-10 notes the CI jobs |

## Public API

none.

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| `jqwik-ban` job green on the PR, plus a local run of the script | CI and local | 2, 8 |
| A temporary commit on the branch that removes the Enforcer ban makes `jqwik-ban` fail. Revert it before review and link both runs. | CI | 2 |
| `build`, `codeql`, `dependency-review` green on the PR | CI | 1, 3, 4, 7 |
| Read-through of permissions, pins and Dependabot ignore | review | 5, 6 |

## Security and privacy

- Trust boundaries touched: CI runners, third-party actions, dependency updates.
- Threats: TM-10 (CodeQL, dependency review and Dependabot now run); TM-14 (the behavioral check).
- Secrets: none used. Third-party actions pinned by SHA. Least-privilege permissions.
- Personal data: none.

## Gates

All gates in `/verify-gates` pass locally, and all four CI checks pass on the pull request.

## Definition of done

- [ ] Every acceptance criterion has evidence (local output or CI run links)
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] Director approved pushing the workflow files
- [ ] CI green on the pull request
- [ ] Director merged the pull request

## Open questions

- none

## Notes

- Look up each action's latest release and pin its SHA from the release tag. Do not guess
  SHAs: `actions/checkout`, `actions/setup-java`, `github/codeql-action`,
  `actions/dependency-review-action`.
- The project hook blocks test-skipping flags, even inside quoted text in a shell command. Use
  `./mvnw -B clean compile` for the CodeQL build.
