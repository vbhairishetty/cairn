# Cairn threat model

Cairn runs on a user's own machine and processes personal health-plan documents. This file
lists the threats specific to Cairn, the defense for each, and the test that proves the
defense. Every threat must have a passing test before release. The security reviewer adds new
entries when a change introduces a new risk.

Last reviewed: <date> by <director>

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
| TM-02 | Prompt injection inside a PDF steers Ask Cairn ("ignore your instructions") | Document text placed in a delimited data block, system prompt says it is data, citations required, number post-check | `AskInjectionSecurityTest` with an injected fixture; eval case in `ask-cases.yaml` | planned |
| TM-03 | Rule Watcher fetches an unsafe URL (SSRF in CI, malicious redirect) | HTTPS allowlist, no cross-host redirects, timeout and size limit | `RuleWatcherAllowlistTest`: non-allowlisted URL and cross-host redirect refused | planned |
| TM-04 | Cross-site scripting through plan names or extracted text | Thymeleaf `th:text` only, Content-Security-Policy without inline scripts | `XssSecurityTest`: a plan name with a script tag renders as text | planned |
| TM-05 | SQL injection through search or sort parameters | Parameterized queries, explicit sort-key mapping | `SqlInjectionSecurityTest`: malicious inputs return no extra rows and no error details | planned |
| TM-06 | Cross-site request forgery changes data from another site | Spring Security CSRF tokens on state-changing requests | `CsrfSecurityTest`: post without a token returns 403 | planned |
| TM-07 | Cairn exposed on the local network | Bind to `127.0.0.1` by default; warning when changed | `BindAddressTest`: default bind address is loopback | planned |
| TM-08 | Personal data leaks into logs or error responses | No document text or profile values in logs; problem details without internals | `LogPrivacyTest`: known fixture values absent from captured logs; `ErrorResponseTest` | planned |
| TM-09 | Wrong or stale legal rule misleads a user | Versioned rules with sources and `verified_on`; 90-day review flag; human approval of changes | `RuleDataValidationTest`: missing source or overlapping dates fail startup; stale flag shown | planned |
| TM-10 | Vulnerable dependency or image | Dependabot, dependency review, CodeQL, Trivy; pinned versions | CI jobs; release blocked on High or Critical | planned |

Status values: `planned`, `defended` (defense merged), `tested` (test merged and passing).

## Out of scope for v1

- Multi-user access control: Cairn is single-user and local.
- Protection against an attacker who already controls the user's machine.
