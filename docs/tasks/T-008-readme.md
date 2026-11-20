---
id: T-008
title: Add the README and the Apache-2.0 license
status: IN_PROGRESS
complexity: low
risk: low
agent: junior-implementer
modules: [docs]
branch: docs/T-008-readme
depends_on: [T-001]
design_refs: ["docs/designs/cairn.md#1-overview", "docs/designs/cairn.md#2-users-and-v1-scope", "docs/designs/cairn.md#13-risks-limitations-cut-list-how-we-work-disclaimer-and-sources"]
threats: []
---

# T-008: Add the README and the Apache-2.0 license

## Goal

Give the public repository a README that says honestly what Cairn is, what works today and
what v0.1.0 will deliver, and make the project legally open source with an Apache-2.0
license. A visitor should understand the project in under a minute and be able to build what
exists.

## Context

The repository is public. It has the approved design (`docs/designs/cairn.md`), ADRs 0001 to
0005, the threat model, the AI workflow log, and the T-001 build skeleton. `./mvnw clean
verify` builds an empty Spring Boot 4.1 app. Nothing a user can run exists yet. v0.1.0 is
planned for Nov 25, 2026. There is no `README.md` and no `LICENSE` file.

Director decisions (Nov 20, 2026):
- License: Apache-2.0.
- No personal origin story. The README describes the OPT coverage problem in general terms
  and says nothing about the author's visa status or personal situation.
- Status is stated honestly as "in development". The README lists only steps that work
  today, and describes planned features as planned.

## Acceptance criteria

1. Given the repository root, When a visitor opens `README.md`, Then it has these sections in
   order:
   - title "Cairn" with the tagline "Trail markers for your health coverage."
   - a status line saying Cairn is under active development, v0.1.0 is planned for
     Nov 25, 2026, and today only the build skeleton exists
   - "What Cairn will do": the v0.1.0 features from design section 2 (compare plans from SBC
     PDFs with every number linked to its source, free-benefits checklist with calendar
     reminders, "what changed since last year", and the OPT and STEM OPT coverage timeline),
     written as planned, not as working
   - "Privacy": runs only on your machine, nothing about you leaves it, minimal data, and
     delete everything
   - "Not advice": the disclaimer copied word for word from the end of design section 13
   - "Build from source": prerequisites (JDK 21) and `./mvnw clean verify`. No
     `docker compose` or run instructions yet, because they do not work yet.
   - "How Cairn is built": one or two sentences on the AI-directed, human-reviewed workflow,
     with links to the design, `docs/adr/`, `docs/threat-model.md` and `docs/ai-workflow.md`
   - "Tech stack": one line
   - "License": Apache-2.0, linking to `LICENSE`
2. Given `README.md`, When every relative link in it is checked, Then each target file or
   folder exists in the repository.
3. Given `README.md`, When it is read, Then it contains no personal information about the
   author (no visa status, school or origin story), no emoji, no badges (there is no CI yet),
   and no claim that a feature works when it does not.
4. Given the repository root, When `LICENSE` is opened, Then it is the unmodified Apache
   License 2.0 text from https://www.apache.org/licenses/LICENSE-2.0.txt.
5. Given `pom.xml`, When it is read, Then it declares the license in a `<licenses>` element
   (name "Apache-2.0", url https://www.apache.org/licenses/LICENSE-2.0).
6. Given the change, When `./mvnw clean verify` runs, Then all gates still pass.

## Out of scope

- Demo GIF, screenshots and usage walkthrough (Day 7 README task, once the app runs).
- The known-limitations table from design section 13 ("shown in the README"). The Day 7 README
  card must include it (review finding R6).
- Contributing guide, code of conduct and security policy (`SECURITY.md`), which are follow-up
  cards if the director wants them.
- A NOTICE file (Apache-2.0 does not require one unless third-party notices need it).
- CI badges (after T-004).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `README.md` | project README |
| create | `LICENSE` | Apache License 2.0, unmodified |
| modify | `pom.xml` | `<licenses>` element |

## Public API

none.

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| Link check: every relative link in `README.md` resolves (scripted check, output in report) | manual | 2 |
| `LICENSE` compared byte for byte with the official text (download to the scratchpad, `cmp`) | manual | 4 |
| Read-through against criteria 1 and 3 | review | 1, 3 |
| `./mvnw clean verify` | build | 5, 6 |

## Security and privacy

- Trust boundaries touched: none.
- Threats: none.
- Personal data: none allowed. The README must not mention the author's visa status, school
  or personal situation (director decision).

## Gates

All gates in `/verify-gates` must pass (gate 4 is `./mvnw clean verify`).

## Definition of done

- [ ] Every acceptance criterion has evidence
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] Director merged the pull request

## Open questions

- none

## Notes

- The README may address the reader as "you". The no-"you" rule applies only to pull request
  descriptions.
- Keep it short: about 80 to 150 lines of Markdown.
- The pull request also carries the T-001 close-out commit (card DONE, AI log entry). That is
  bookkeeping, because commits cannot go to `main` directly.
