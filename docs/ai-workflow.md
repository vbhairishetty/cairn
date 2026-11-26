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
| Tasks merged | 8 |
| Review rounds (total) | 30 |
| Reviewer findings (High / Medium / Low) | 1 / 26 / 77 |
| Security findings (High / Medium / Low) | 0 / 6 / 31 |
| Agent outputs rejected by the orchestrator | 1 |
| CI failures after local gates passed | 4 |

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

### T-008: Add the README and the Apache-2.0 license (2026-11-20)
- Pull request: https://github.com/vbhairishetty/cairn/pull/3
- Implementer: junior-implementer (Haiku); rounds: 2
- Reviewer findings: 1 Medium, 9 Low. Most important: R1 [Medium], the README promised that
  "every number will link to the line in the PDF", but the design's sources include values the
  user enters, labeled price assumptions and computed totals. The sentence now names all of
  them. The reviewer also found that CLAUDE.md rule 5 had the same overstatement; the director
  had it reworded in T-009.
- Security findings: 3 Low. Most important: S2, the Privacy section did not say that uploaded
  PDFs and their extracted text are stored locally until deleted; a sentence was added. S1
  (keep the connection wording in sync as features land) was recorded in TASKS.md. S3
  (personal email in public commit metadata) led the director to switch to the GitHub no-reply
  address.
- Rejected: one implementer claim. It reported that CLAUDE.md still said "Spring Boot 3" and
  listed Redis. The orchestrator checked the file on disk and found this false (it already
  said Spring Boot 4.1 and no Redis); the agent had relied on an outdated copy.
- CI: none yet (T-004). Gates were run locally by the implementer, the reviewer and the
  orchestrator.
- Director decisions: Apache-2.0 license; no personal origin story in the public README; an
  honest "in development" status with only working build steps.
- Interview note: "The reviewer caught the README promising more provenance than the design
  delivers, and the same overstatement turned up in the project's own rules."

### T-009: Remove jqwik, ban it in the build, and reword rule 5 (2026-11-21)
- Pull request: https://github.com/vbhairishetty/cairn/pull/4
- Implementer: sde1-implementer (Sonnet); rounds: 8. The task loop's limit is 3; the director
  approved each extension.
- Reviewer findings: 6 Medium, 23 Low, across three independent reviewers (one restarted with
  the session). Most important: the guard tests kept passing while the ban was disabled. In
  turn the reviewers found skip or fail settings, WARN level, an includes allow-list, profiles,
  property-valued settings, a split execution, `.mvn/jvm.config` skips, jqwik on the Surefire
  classpath, a local parent pom, `.mvn/maven.config` redirects and interpolated plugin keys.
  Rounds 1 to 4 added one check per bypass. After the security review failed, round 5 replaced
  that blocklist with an allowlist: the Enforcer block must match one approved shape, and
  everything else that could reach a classpath is rejected. The test cases became anchored
  edits of the live build files instead of 24 committed pom copies.
- Security findings: 3 Medium, 4 Low, over three security reviews (FAIL, FAIL, PASS).
  - Most important: the guard only read `/project/build/plugins`, so `pluginManagement` or
    plugin-level settings turned the ban off.
  - Test-only system properties could point the guards at a decoy pom from a committed
    `.mvn/maven.config`.
  - Both were fixed. Four Low gaps remain, recorded in TM-14, for the T-004 CI check.
- Rejected: one implementer proposal. It asked to exempt the `enforcer.version` property from
  the property rule; the orchestrator inlined the version instead, so the allowlist has no
  exceptions. Also recorded: an implementer once discarded its own uncommitted pom edits with
  `git checkout` and restored them from a backup; later prompts forbade that command on
  uncommitted work. A reviewer probe left a fake artifact in the shared Maven cache, which made
  a later probe pass for the wrong reason. The director approved deleting it, and probes moved
  to an isolated local repository.
- CI: none yet (T-004). Gates were run locally by the implementer, every reviewer and the
  orchestrator.
- Director decisions:
  - remove jqwik rather than keep or silence it (ADR 0006)
  - reword CLAUDE.md rule 5 to the design's four kinds of source
  - ban with Maven Enforcer now; the Dependabot ignore rule goes to T-004
  - extend past the round limit several times; approve the allowlist redesign
  - delete the stray probe artifact
  - open the pull request only after security passes
- Interview note: "Reviewers found more than a dozen ways to switch off a one-line build
  ban, and the fix that ended it was to stop listing bad configurations and allow only the one
  good one, with the real proof being a CI job that tries to add the banned library."

### T-007: Find, cite and record the CMS coverage-example claims (2026-11-23)
- Pull request: https://github.com/vbhairishetty/cairn/pull/6
- Implementer: orchestrator (research and data work; a general-purpose research agent found the
  sources); rounds: 4
- Reviewer findings: 7 Medium, 10 Low.
  - Most important (R1): the orchestrator's reason text said the bundled obstetric fee was
    billed at delivery. The CMS guide dates it at the first prenatal visit. The director had
    already decided a mapping on that wrong premise; the mapping was reopened and re-decided.
  - The other Mediums were design gaps in reconciliation: per-field rows, a half-up tie rule,
    the copay-to-deductible split, and a tolerance that could never pass because a $50 limit
    cannot absorb a $100 rounding step. The reviewer rebuilt all 165 claim lines from CMS
    with its own parser.
- Security findings: 2 Low. Each guide is now pinned by SHA-256 and revision in the data, and
  provenance fields are repeated per scenario. Safe-loading rules for the future loader
  (proposed TM-09a) are recorded in TASKS.md.
- Rejected: nothing from agents. Recorded errors by the orchestrator:
  - the wrong obstetric date
  - an unsourced "generic insulin glargine exists"
  - an overstated "13 categories" count
  - a mapping (cast supplies) marked clean when it was not
  - a rounding-edge list that contradicted the tolerance
- CI: none yet (T-004). Gates were run locally.
- Director decisions:
  - store no CPT codes or AMA descriptors
  - reconcile after CMS rounding and caps, comparing every printed field
  - the category mappings, including two re-decisions after the corrected facts
  - derived rows inherit `near_match`
  - SBC fixtures are not committed, only URL, hash and a download script
  - the first golden SBC is the City of Seattle Kaiser HMO
- Interview note: "The reviewer re-derived every claim line from the government source and
  caught that I had fed the director a wrong date; we fixed the data and the decision, and
  wrote down both."

### T-004: Add CI, CodeQL, dependency review, Dependabot and the jqwik behavioral check (2026-11-24)
- Pull request: https://github.com/vbhairishetty/cairn/pull/7
- Implementer: sde1-implementer (Sonnet); rounds: 3
- Reviewer findings: 4 Medium, 8 Low.
  - Most important: each round found a new way to make both CI jobs green while jqwik ran:
    - round 1: an export-ignored `.mvn/maven.config` skip missing from the `git archive` copy,
      plus output redirected to report files;
    - round 2: Surefire method filters plus an extra test execution with jqwik on its
      classpath and hidden reports.
  - Round 3 replaced pom-shape checks with one that does not depend on the pom: the build with
    jqwik added gets its own local repository, and a normal test run must leave no `net/jqwik`
    in the shared one.
  - Three deliberate-edit bypasses remain as Low, listed in TM-14.
- Security findings: 1 Medium, 3 Low. Most important (S1): the pinned `setup-java` action
  exports `MAVEN_ARGS=-ntp`, so the script's refusal of any `MAVEN_ARGS` in CI would have
  failed `jqwik-ban` on every run. No local run could show it. The fix was
  `show-download-progress: true`. Also fixed: every push to `main` keeps its own CI result, and
  Dependabot waits 7 days after a release.
- Rejected: nothing from agents. The orchestrator found and fixed its own T-009 bug: the
  `.gitattributes` line `mvnw.cmd text eol=crlf` made every fresh clone show `mvnw.cmd` as
  modified.
- CI: two red runs after local gates passed.
  - On the PR, `dependency-review` returned "Forbidden" because the repository's Dependency
    graph was off. The director enabled it.
  - On `main` after the merge, `build` failed before Maven started, because the wrapper could
    not download Maven from Maven Central once. A re-run passed. Caching the Maven download is
    proposed as a follow-up.
  - Negative proof: a temporary commit removing the ban turned `build` and `jqwik-ban` red
    (run 45295130586), then was reverted.
- Director decisions:
  - approve pushing workflow files once security passed
  - enable the Dependency graph and Dependabot alerts
  - first Dependabot PRs: review #9 (compiler plugin) and close #8 (Maven 3.10 upgrade
    deferred, because the wrapper hash pin needs a deliberate task)
- Interview note: "Every local check passed, and the security reviewer still found that a
  pinned GitHub action set an environment variable that would have failed our check on every
  CI run."

### T-010: Retry the Maven wrapper download in CI (2026-11-25)
- Pull request: https://github.com/vbhairishetty/cairn/pull/11 (merged through #12)
- Implementer: sde1-implementer (Sonnet); rounds: 3
- Reviewer findings: 1 High, 8 Low.
  - Most important (High): the orchestrator's card said the Maven download was not cached
    between CI runs. The pinned `setup-java` with `cache: maven` also caches
    `~/.m2/wrapper/dists`, and the wrapper runs that copy without re-checking its checksum.
  - The security reviewer found the same thing independently. The director decided to delete
    the cached copy before every download, and to record cached dependency jars as an
    accepted risk (new TM-15).
- Security findings: 1 Medium (the same false premise), 5 Low: a retry could hide a checksum
  mismatch, the CI log could not prove a fresh download, and the card wording was stale.
- Rejected: nothing from agents. The orchestrator's own card premise was wrong; it was
  corrected in the card with a dated note.
- CI: one red run after local gates passed. On the green PR, one `jqwik-ban` run failed all
  three download attempts within 30 seconds, while other jobs in the same run downloaded
  Maven. A re-run passed. The director approved widening the retry to five attempts over
  about four minutes, with the wrapper in verbose mode so the real error reaches the log.
- Director decisions:
  - retry rather than cache
  - delete the cached Maven before each download
  - accept the dependency-jar cache risk (TM-15)
  - widen the backoff after the CI-only failure
- Interview note: "Two reviewers independently caught that my own task card had the caching
  behavior of a pinned GitHub action wrong, which would have left an unverified build tool in
  every CI run."

### T-002: Create the module root packages and the Modularity test (2026-11-25)
- Pull request: https://github.com/vbhairishetty/cairn/pull/13
- Implementer: sde1-implementer (Sonnet); rounds: 5 (the director approved rounds 4 and 5)
- Reviewer findings: 4 Medium, 3 Low.
  - Most important: Spring Modulith's `verify()` only checks code against whatever the
    annotations declare, so the boundary could be switched off in the same change as the
    violation.
  - Each round found another one-line escape that kept the build green:
    - a widened or deleted `allowedDependencies`;
    - `type = OPEN`;
    - `@Modulithic(sharedModules)`;
    - the composed `@Modulith` annotation carrying the same setting.
  - The final fix asserts what Modulith actually computes (`getSharedModules()` empty) plus a
    merged-annotation lookup, alongside a table test that pins every module's dependencies and
    CLOSED type.
- Security findings: 4 Low. Public APIs should return IDs, not paths or raw text; changes to
  `package-info.java` need an ADR; the root package should hold only `CairnApplication`; and
  the indirect `@Modulithic` gap, later fixed. The first three are recorded in TASKS.md for
  the first feature cards.
- Rejected: nothing from agents. Two reviewers disagreed on whether `sharedModules` affects
  dependency rules; the orchestrator followed the reviewer whose probe showed it does.
- CI: green on the first run.
- Director decisions:
  - `web` and `cli` may use any module except each other (card and architecture rules updated)
  - two extra rounds to close the shared-modules escapes
- Interview note: "The reviewer kept proving that a one-line annotation could quietly switch
  off a module boundary, and the fix that ended it was to test what the framework computes
  rather than how the annotations are written."

### T-003: Add Docker Compose Postgres, Flyway and the first migration (2026-11-25)
- Pull request: https://github.com/vbhairishetty/cairn/pull/14
- Implementer: sde1-implementer (Sonnet); rounds: 3
- Reviewer findings: 3 Medium, 11 Low.
  - Most important: the TM-07 port check only read `ports` in `docker-compose.yml`, so
    `network_mode: host` (later also written as `${NET:-host}`) exposed a service while the
    test stayed green.
  - Flyway put its history table in the `documents` schema; the orchestrator moved it to a
    dedicated `flyway` schema so a delete-all of module data cannot wipe it.
  - Negative tests restated the rule instead of calling it, so a weakened rule passed; all
    cases now go through one shared check, and the `contains` mutant is killed.
- Security findings: 1 Medium, 7 Low.
  - S1 (Medium): Compose auto-loads `compose.yaml`, override files, `include` and `extends`,
    none of which the guard read. Fixed.
  - The example password `change-me` would have passed Compose's required-variable check;
    the director chose an empty value with a test. The image is pinned by digest (director).
  - Deferred to later cards in TASKS.md: a non-superuser app role (S4), no password in
    `CAIRN_DB_URL` (S5), the `KEY: value` env form (S8).
- Rejected: nothing from agents. One test (`EnvExampleTest`) had no red-first evidence; the
  reviewer supplied it with a mutant.
- CI: the tests passed on every run, but `build` and `codeql` failed four times before Maven
  started because Maven Central returned 429 Too Many Requests to the wrapper download for
  the whole retry window. Re-runs passed once the limit cleared. T-011 removes the per-job
  download.
- Director decisions: empty `CAIRN_DB_PASSWORD` in `.env.example`; tag-and-digest image pin;
  wait and re-run rather than retry into the rate limit; write T-011.
- Interview note: "Every round the reviewers found another way Docker Compose could expose
  the database without touching the file the test read, so the guard ended up rejecting
  anything it could not prove safe."
