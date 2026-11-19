# How Cairn was built with AI agents

This is the honest record of how each part of Cairn was designed, built and verified by a
team of Claude Code agents directed by a human. It records what worked, what reviewers
caught, and what was rejected. Entries are added by `/ai-log` after each merge and are never
rewritten except to correct a factual error.

## The team

| Role | Agent | Model |
| --- | --- | --- |
| Director | Human | - |
| Orchestrator (staff engineer) | Main Claude Code session | as configured |
| Senior implementer | `sde2-implementer` | Opus |
| Implementer | `sde1-implementer` | Sonnet |
| Junior implementer | `junior-implementer` | Haiku |
| Independent reviewer | `independent-reviewer` | Opus |
| Security reviewer | `security-reviewer` | Opus |
| Design reviewer | `design-reviewer` | Sonnet |

## Totals

| Measure | Count |
| --- | --- |
| Tasks merged | 0 |
| Review rounds (total) | 0 |
| Reviewer findings (High / Medium / Low) | 0 / 0 / 0 |
| Security findings (High / Medium / Low) | 0 / 0 / 0 |
| Agent outputs rejected by the orchestrator | 0 |
| CI failures after local gates passed | 0 |

## Design reviews

<!-- /design-review adds entries here -->

### 2026-11-19: `docs/designs/cairn.md` (v1 design)

- **Method:** the EGM plugin is not installed, so `/design-review` ran fresh `design-reviewer`
  agents: comprehension, adversarial and readiness in parallel (round 1), then readiness
  alone after each round of edits (rounds 2 and 3).
- **Round 1:** NO-GO from all three. After merging duplicates there were 16 findings (R1 to
  R16, 6 High). Key gaps: the engine formula contradicted step 6 (it capped "not covered"
  costs); reconciliation was undefined (whose prices?); there was no table ownership; the
  compose ports would be exposed on the LAN; there was no Host-header defense; and SMTP
  "exactly once" was impossible.
- **Director decisions:** reminders at least once (email is a stretch goal only); free
  benefits filtered by age band with no sex field; Redis cut now (ADR 0001); `.ics` is the only
  v1 reminder channel; finish the review today.
- **Orchestrator findings beyond the reviewers:** the design conflicted with the repo rules
  (`api` module, coverage list, branch names, engine reading the database). Three module
  dependency gaps needed ADRs: `profile` ownership (0002), jobs calling extract (0003), and
  engine needing reference data for reconciliation, which also moved reconciliation from
  `validate` to `engine` to avoid a cycle (0004).
- **Round 2:** NO-GO, 22 findings (7 High). The main fixes: the pipeline became synchronous
  listeners inside the job transaction, so "job done" means the plan exists; quarantine and
  review merged into `review_item`; `SourceRef` gained kinds for user-entered and assumed
  values; delete-all separates user data from reference data; and shared enums moved to
  `common`.
- **Round 3:** GO, 22 findings (0 High, 14 Medium, 8 Low). D20 and D21 were fixed at once;
  the rest are carried into the task cards for the day they affect.
- **Process note:** in rounds 2 and 3 the orchestrator told the reviewer that per-task detail
  belongs in task cards, and in round 3 that High means "forces incompatible guesses". That
  calibration shaped the GO verdict.
- **Director approval (same day):** design approved; ADRs 0001 to 0004 accepted;
  iCal4j, springdoc-openapi and Spring Security added to the stack. The director changed one
  default: plans with unconfirmed low-confidence values are flagged "needs review" and never
  ranked best, the same as incomplete plans. Reconciliation stays strict, with escalation in
  T-007. The director accepted the OPT persona rewording, but noted that the orchestrator
  changed product wording without asking first; future scope or wording changes go to the
  director before they are made.
- **Rejected:** none of the reviewer findings were rejected. Some were narrowed: the request
  for full signatures and DDL in the design was answered with the contracts other modules
  depend on, leaving the rest to task cards.

## Task log

<!-- /ai-log appends entries here, newest last -->
