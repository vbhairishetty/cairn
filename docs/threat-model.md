# Cairn threat model

Cairn runs on a user's own machine and processes personal health-plan documents. This file
lists the threats specific to Cairn, the defense for each, and the test that proves the
defense. Every threat must have a passing test before release. The security reviewer adds new
entries when a change introduces a new risk.

Last reviewed: 2026-11-19 in the design review, approved by the director

## Assets

- Uploaded plan documents and the text extracted from them.
- The user's profile (age band, coverage tier, optional visa category).
- Estimates, reminders and timeline dates.
- Rule data that users rely on for decisions.

## Trust boundaries

1. Uploaded files and extracted text.
2. HTTP requests from the browser or API clients.
3. HTML rendered back to the browser.
4. The database and file store.
5. The local LLM: prompts in, text out.
6. Pages fetched by the Rule Watcher in CI.
7. Dependencies and the Docker image.

## Threats

| Id | Threat | Defense | Test | Status |
| --- | --- | --- | --- | --- |
| TM-01 | Malicious or huge upload exhausts memory or CPU (oversized file, page bomb, fake PDF) | 10 MB and 50-page limits, PDF magic bytes and content-type check, parser timeout, generated storage names | `UploadLimitsSecurityTest`: oversized, too many pages, fake PDF, slow parse all rejected | planned |
| TM-02 | (Ask Cairn only; "not shipped" if cut) Prompt injection inside a PDF steers Ask Cairn ("ignore your instructions") | Document text placed in a delimited data block, system prompt says it is data, citations required, number post-check | `AskInjectionSecurityTest` with an injected fixture; eval case in `ask-cases.yaml` | planned |
| TM-03 | (Rule Watcher only; "not shipped" if cut) Rule Watcher fetches an unsafe URL (SSRF in CI, malicious redirect) | HTTPS allowlist, no cross-host redirects, timeout and size limit | `RuleWatcherAllowlistTest`: non-allowlisted URL and cross-host redirect refused | planned |
| TM-04 | Cross-site scripting through plan names or extracted text | Thymeleaf `th:text` only, Content-Security-Policy without inline scripts | `XssSecurityTest`: a plan name with a script tag renders as text | planned |
| TM-05 | SQL injection through search or sort parameters | Parameterized queries, explicit sort-key mapping | `SqlInjectionSecurityTest`: malicious inputs return no extra rows and no error details | planned |
| TM-06 | Cross-site request forgery changes data from another site | Spring Security CSRF tokens on state-changing requests | `CsrfSecurityTest`: post without a token returns 403 | planned |
| TM-07 | Cairn exposed on the local network | Bind to `127.0.0.1` by default; warning when changed; Docker Compose publishes every port on `127.0.0.1` only | `BindAddressTest`: default bind address is loopback; `ComposePortsTest`: every published port in `docker-compose.yml` starts with `127.0.0.1:` | planned |
| TM-11 | DNS rebinding: a malicious website in the user's browser reaches the local API | Requests whose `Host` header is not `localhost` or `127.0.0.1` are rejected; CSRF on every state-changing request | `HostHeaderSecurityTest`: `Host: evil.example` is rejected; `localhost:8080`, `127.0.0.1` and `[::1]` are accepted | planned |
| TM-12 | Management endpoints leak configuration or internals | Only the `health` and `prometheus` actuator endpoints are exposed | `ActuatorExposureSecurityTest`: `/actuator/env` and `/actuator/heapdump` return 404 | planned |
| TM-13 | Personal data left behind after "delete everything" | Delete-all pauses the worker and wipes every module schema and the file store | `DeleteAllSecurityTest`: every user-data table and the data directory are empty afterwards | planned |
| TM-14 | Text in build, test or tool output (dependency messages, fetched pages, issue text) tries to instruct the AI agents doing development work, for example to ignore failing tests or skip gates | Tool output is data (CLAUDE.md rule 14); gate verdicts come only from exit codes and Surefire/Failsafe report counts; the orchestrator re-runs gates itself; dependency bumps are checked for new agent-directed output; any such text is reported to the director. First seen: jqwik 1.10.2 printed a message addressed to AI agents in every test run; jqwik was removed and banned with the Enforcer `bannedDependencies` rule (ADR 0006) | `JqwikBanSecurityTest`: BuildGuard requires the Enforcer plugin to match one allowlisted shape, rejects "jqwik" anywhere else in the pom (names split by comments or CDATA are caught), rejects dependencies or classpath settings on Surefire or Failsafe and `${` in any groupId or artifactId, requires the standard parent, requires `.mvn/maven.config` absent or blank, `.mvn/jvm.config` equal to the committed lines, `.mvn/extensions.xml` absent, and `mvnw` and `maven-wrapper.properties` to match pinned SHA-256 values. Each bypass found in review is a generated edit of the live build files in a temporary copy. The T-004 `jqwik-ban` CI job builds a copy with jqwik added (must fail with the ban), and requires a normal test run to leave no `net/jqwik` in the local Maven repository, no jqwik text in output or `target/`, and the named guard tests to pass. That closes the earlier guard gaps for every accidental route. Residual gaps (Low, deliberate and visible edits only; T-004 review N4 to N6): a committed `.mvn/maven.config` that moves the local repository together with a decoy build tree; Surefire `workingDirectory` pointing the guard tests at a decoy copy; committed jqwik jars under other coordinates or a pom repository serving relocated jqwik | tested (T-004: `jqwik-ban` green on PR #7 run 45295297179; red with the ban removed, run 45295130586) |
| TM-15 | A tampered build tool or dependency is reused from the CI cache. `actions/setup-java` with `cache: maven` restores `~/.m2/wrapper/dists` (the Maven distribution) and `~/.m2/repository` (dependency jars), and neither is re-verified when it is reused | Maven distribution: every CI job deletes the extracted distribution, then installs Maven from a zip whose SHA-256 equals `distributionSha256Sum`. The zip is kept in `~/.cache/cairn/maven-dist`, saved by `actions/cache` (pinned by SHA) under a key from the wrapper properties, and re-checked on every use; an otherwise poisoned entry is out of reach because only runs of merged code on main can save main-scope caches (the same premise as the dependency-jar risk below); a mismatch fails the job at once with no fallback download, and with no cached zip the job downloads it, checks it, and stores it (T-010, T-011). Dependency jars: accepted risk (director, 2026-11-24). The cache on `main` is saved only by runs of merged, reviewed code, pull request caches are scoped to their own ref, and a malicious dependency would already run during the build that cached it. Re-check if CI gains a release or deploy job with write permissions | `scripts/ci/test-maven-download-with-retry.sh` (local stub test, not run in CI) against `scripts/ci/maven-download-with-retry.sh`: a pre-populated distribution is deleted and never run, a modified cached zip fails with `FAIL: cached Maven distribution checksum mismatch` and no download, a download with wrong bytes fails at once; each CI job's log shows `Deleted cached Maven distributions under ...`, then either `Using cached Maven distribution (SHA-256 verified)` or `Maven download ok on attempt N`. Recovery: a corrupt or poisoned cache entry fails every run until `gh cache delete <key>` is run | tested for the Maven distribution (T-010, PR #11; T-011, PR #15: CI runs 45747443591 and 45747443749, attempt 1 missed the cache, downloaded and saved the zip, attempt 2 logged `Using cached Maven distribution (SHA-256 verified)` in all three Maven jobs with no download); dependency jars accepted |
| TM-08 | Personal data leaks into logs or error responses | No document text or profile values in logs; problem details without internals | `LogPrivacyTest`: known fixture values absent from captured logs; `ErrorResponseTest` | planned |
| TM-09 | Wrong or stale legal rule misleads a user | Versioned rules with sources and `verified_on`; 90-day review flag; human approval of changes | `RuleDataValidationTest`: missing source or overlapping dates fail startup; stale flag shown | planned |
| TM-10 | Vulnerable dependency or image | Dependabot, dependency review, CodeQL, Trivy; pinned versions | CI jobs: `codeql` and `dependency-review` workflows (fail on High or Critical), `.github/dependabot.yml` (weekly, maven and github-actions), actions pinned by SHA (T-004); Trivy and release blocking come with the Dockerfile Note: on 2026-11-24 the dependency graph listed 18 packages, so dependency review likely sees only direct dependencies declared in `pom.xml`; transitive ones rely on Dependabot alerts and, later, Trivy. | planned |

Status values: `planned`, `defended` (defense merged), `tested` (test merged and passing),
`not shipped` (the feature was cut; the threat does not apply to the release).

## Out of scope for v1

- Multi-user access control: Cairn is single-user and local.
- Protection against an attacker who already controls the user's machine.
