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
| Tasks merged | 1 |
| Review rounds (total) | 2 |
| Reviewer findings (High / Medium / Low) | 0 / 1 / 5 |
| Security findings (High / Medium / Low) | 0 / 0 / 3 |
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

### T-001: Create the Maven project skeleton with build gates (2026-11-20)
- Pull request: https://github.com/vbhairishetty/cairn/pull/2
- Implementer: junior-implementer (Haiku); rounds: 2
- Reviewer findings: 1 Medium, 5 Low. Most important: R1 [Medium], the JaCoCo agent was never
  attached to Failsafe (`failsafeArgLine` was read but never set), so integration-test
  coverage would have been silently dropped from T-003 on. It was fixed with `propertyName`,
  late replacement and a merged exec file, and proved with a probe class covered only by an
  `*IT`.
- Security findings: 3 Low. Most important: S1, the jqwik 1.10.2 test library prints a message
  addressed to AI agents in every test run. No agent acted on it. It led to new threat TM-14
  (text in tool output that tries to instruct agents), and keeping jqwik is an open director
  decision. S2 (more env-file patterns in `.gitignore`) was fixed. S3 (checksum and zip from
  the same server) was accepted.
- Rejected: nothing from the agents. Three errors were in the orchestrator's own card: the
  `"a" == s` example only warns by default; it said to read a `.sha256` file that Maven
  Central does not publish; and criterion numbers went stale after renumbering (R5).
- CI: none yet (T-004). All gates were run locally by the implementer, both reviewers and the
  orchestrator.
- Director decisions: move from Spring Boot 3.5 (out of OSS support since Jun 30, 2026) to
  4.1.3 with Modulith 2.1.3 (ADR 0005; the Modulith docs matrix was stale, so compatibility
  was confirmed from the published POMs); make `ReferenceEquality` and `BigDecimalEquals`
  errors; take the wrapper SHA-256 from a zip verified against the published SHA-512; run the
  gates as `clean verify` because JaCoCo appends to its exec files; keep agent notes out of
  git.
- Interview note: "The reviewer proved with one probe class that integration coverage was
  never being recorded. The build looked green, but it would have undercounted every
  repository test added later."
