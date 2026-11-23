# Cairn backlog

Status values: DRAFT, READY, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED.
Cards live in `docs/tasks/`. The orchestrator keeps this file in sync; the director approves
cards (DRAFT to READY) in `/plan-day`.

## Day 1: design, gates and foundations

| Id | Title | Agent | Status | Depends on |
| --- | --- | --- | --- | --- |
| - | Design draft and review of `docs/designs/cairn.md` (`/design-review`) | orchestrator + design-reviewer | DONE (PR #1) | - |
| T-001 | Maven project skeleton with Spring Boot, Spring Modulith, Spotless, Error Prone, JaCoCo | junior-implementer | DONE (PR #2) | design PR merged |
| T-002 | Modularity test and module root packages for all modules | sde1-implementer | READY | T-001 |
| T-003 | `docker-compose.yml` (Postgres with pgvector, ports on `127.0.0.1` only) and first Flyway migration | sde1-implementer (medium risk: TM-07, credentials) | READY | T-001 |
| T-004 | CI workflow: build, gates, CodeQL, dependency review; Dependabot config; jqwik behavioral check | sde1-implementer | READY | T-001, T-009 |
| T-005 | `common.Money` and `SourceRef` value types with property tests | sde2-implementer | READY | T-002 |
| T-006 | Golden JSON for the first SBC fixture and `cairn-plan-v1.json` schema, checked by hand | orchestrator | READY (needs public SBC PDFs) | - |
| T-007 | Find and cite the CMS coverage-example claim data; draft `sbc-coverage-examples.yaml` | orchestrator | IN_REVIEW | - |
| T-008 | README and Apache-2.0 license | junior-implementer | DONE (PR #3) | T-001 |
| T-009 | Remove jqwik, ban it in the build, reword rule 5 | sde1-implementer | DONE (PR #4) | T-001 |

## Day 2 onward

Created each morning with `/plan-day` from the roadmap in `docs/designs/cairn.md`. v0.1.0
moved to Wed Dec 2, 2026 (director decision, Nov 23).

Plan for Mon Nov 23, approved: T-007, T-004, T-002, T-003, T-005, T-006, in that order.

## Notes for later cards

- Day 7 README card: include the known-limitations table from design section 13 (deferred
  from T-008, review findings R6 and N3).
- T-004: the Dependabot config must ignore `net.jqwik:*` (ADR 0006).
- T-004: add a behavioral CI check for the jqwik ban: copy the whole repo (pom and `.mvn/`), add
  `net.jqwik:jqwik` as a direct dependency, run `./mvnw validate` with an explicit command line
  and empty `MAVEN_OPTS`, and require it to fail with the ban message (`net.jqwik is banned`),
  not just any failure (T-009 review, director decision; N1).
  The same job also runs the real repo's `./mvnw test` and fails if the output contains
  "jqwik" anywhere other than the test class name `JqwikBanSecurityTest` (T-009 review R10, N4).
  This also closes the known guard gaps: Surefire excludes that skip the guard tests, an
  edited `mvnw.cmd`, build extensions declared in the pom, and a system-scope dependency
  pointing at a cached jqwik jar (T-009 review N2, D2, D3; security S1 round 8).
- Engine card: CMS claims list one visit as several same-day lines (for example the fracture ER
  visit: ambulance, facility, x-ray, treatment). Define whether a copay applies per visit or
  per line before writing the reconciliation tests (T-007).
- Rules loader card (T-007 security review, proposed TM-09a): parse rules YAML safely into typed
  records (SafeConstructor or Jackson YAML, no tags, no aliases, code-point limit, unknown
  fields rejected); amounts must be quoted strings parsed to `BigDecimal`; dates as `LocalDate`;
  every claim category must be in `category_mapping`; `service` is null only when `status` is
  `excluded`; each scenario's line sum must equal `total_allowed_published`; each scenario must
  carry `source_sha256`, `source_revision`, `effective_from` and `verified_on`. Add
  `RuleDataLoaderSecurityTest` with one failing fixture per rule. The Rule Watcher compares
  `source_sha256` with the live file.
- Any card that adds a second Enforcer rule: add guard tests for `skipRules` and
  `enforcer.skipRules` in the pom and `.mvn` files (T-009 review R9).
- README Privacy connection sentence: update it if the email stretch (Mailpit SMTP) ships, and
  keep Chart.js and Swagger UI assets bundled locally, never from a CDN (T-008 security S1).

## Cut list (first to go)

1. Ask Cairn
2. Red-flag scanner
3. Dental and vision parsing (keep manual entry)
4. Rule Watcher (keep `verified_on` review flags)
5. CLI `timeline` command

Removed from v1 already: Redis cache (ADR 0001). Email reminders are a stretch goal.

Never cut: tests, CI, provenance links, review screen, privacy rules, quality and security
gates, disclaimer.
