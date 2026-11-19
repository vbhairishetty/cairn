# Cairn backlog

Status values: DRAFT, READY, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED.
Cards live in `docs/tasks/`. The orchestrator keeps this file in sync; the director approves
cards (DRAFT to READY) in `/plan-day`.

## Day 1: design, gates and foundations

| Id | Title | Agent | Status | Depends on |
| --- | --- | --- | --- | --- |
| - | Design draft and review of `docs/designs/cairn.md` (`/design-review`) | orchestrator + design-reviewer | DRAFT | - |
| T-001 | Maven project skeleton with Spring Boot, Spring Modulith, Spotless, Error Prone, JaCoCo | junior-implementer | DRAFT | design approved |
| T-002 | Modularity test and module root packages for all modules | sde1-implementer | DRAFT | T-001 |
| T-003 | `docker-compose.yml` (Postgres with pgvector, Redis, Mailpit) and first Flyway migration | junior-implementer | DRAFT | T-001 |
| T-004 | CI workflow: build, gates, CodeQL, dependency review; Dependabot config | junior-implementer | DRAFT | T-001 |
| T-005 | `common.Money` and `SourceRef` value types with property tests | sde2-implementer | DRAFT | T-002 |
| T-006 | Golden JSON for the first SBC fixture, checked by hand | orchestrator | DRAFT | - |

## Day 2 onward

Created each morning with `/plan-day` from the roadmap in `docs/designs/cairn.md`.

## Cut list (first to go)

1. Ask Cairn
2. Red-flag scanner
3. Dental and vision parsing (keep manual entry)
4. Rule Watcher (keep `verified_on` review flags)
5. CLI `timeline` command
6. Redis cache

Never cut: tests, CI, provenance links, review screen, privacy rules, quality and security
gates, disclaimer.
