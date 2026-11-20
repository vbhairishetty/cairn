# Cairn backlog

Status values: DRAFT, READY, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED.
Cards live in `docs/tasks/`. The orchestrator keeps this file in sync; the director approves
cards (DRAFT to READY) in `/plan-day`.

## Day 1: design, gates and foundations

| Id | Title | Agent | Status | Depends on |
| --- | --- | --- | --- | --- |
| - | Design draft and review of `docs/designs/cairn.md` (`/design-review`) | orchestrator + design-reviewer | IN_REVIEW | - |
| T-001 | Maven project skeleton with Spring Boot, Spring Modulith, Spotless, Error Prone, JaCoCo | junior-implementer | IN_REVIEW (PR #2) | design PR merged |
| T-002 | Modularity test and module root packages for all modules | sde1-implementer | DRAFT | T-001 |
| T-003 | `docker-compose.yml` (Postgres with pgvector, ports on `127.0.0.1` only) and first Flyway migration | sde1-implementer (medium risk: TM-07, credentials) | READY (waits on DB password decision) | T-001 |
| T-004 | CI workflow: build, gates, CodeQL, dependency review; Dependabot config | junior-implementer | DRAFT | T-001 |
| T-005 | `common.Money` and `SourceRef` value types with property tests | sde2-implementer | DRAFT | T-002 |
| T-006 | Golden JSON for the first SBC fixture and `cairn-plan-v1.json` schema, checked by hand | orchestrator | DRAFT | - |
| T-007 | Find and cite the CMS coverage-example claim data; draft `sbc-coverage-examples.yaml` | orchestrator | DRAFT | - |

## Day 2 onward

Created each morning with `/plan-day` from the roadmap in `docs/designs/cairn.md`.

## Cut list (first to go)

1. Ask Cairn
2. Red-flag scanner
3. Dental and vision parsing (keep manual entry)
4. Rule Watcher (keep `verified_on` review flags)
5. CLI `timeline` command

Removed from v1 already: Redis cache (ADR 0001). Email reminders are a stretch goal.

Never cut: tests, CI, provenance links, review screen, privacy rules, quality and security
gates, disclaimer.
