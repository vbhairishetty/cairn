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

## Task log

<!-- /ai-log appends entries here, newest last -->
