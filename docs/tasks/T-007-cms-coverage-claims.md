---
id: T-007
title: Find, cite and record the CMS coverage-example claims
status: IN_PROGRESS
complexity: medium
risk: high
agent: orchestrator
modules: [rules]
branch: docs/T-007-cms-claims
depends_on: []
design_refs: ["docs/designs/cairn.md#6-cost-engine-and-how-we-prove-it-is-correct", "docs/designs/cairn.md#5-data-model"]
threats: [TM-09]
---

# T-007: Find, cite and record the CMS coverage-example claims

## Goal

Confirm that the standard claims behind the SBC coverage examples are published by CMS and can
be cited, and record them as reference data. The reconciliation check depends on them: it
reproduces each SBC's printed totals within $50 (design section 6).

## Context

The design assumes that CMS publishes the claims for each coverage example (having a baby,
managing Type 2 diabetes, a simple fracture), with services and allowed amounts. It says this
task stops and asks the director if the data cannot be found. On Nov 21 the director also
decided that reconciliation stays strict. Work escalates to the director if the data cannot be
found, or if a real SBC misses the tolerance because of a documented simplification.

The `rules` module has no loader yet. This card records the data and its source; the loader and
its validation are a later `rules` card.

## Acceptance criteria

1. Given the research, When it is recorded in `docs/research/cms-coverage-examples.md`, Then it
   names the official CMS pages and files (URLs), the SBC template version they apply to, the
   effective date, the reuse status, and the calculator rules that matter for Cairn:
   - claim order
   - how deductibles, copays and coinsurance apply
   - self-only coverage
   - out-of-pocket limits
2. Given the claims are published, When `src/main/resources/rules/sbc-coverage-examples.yaml`
   is written, Then it has one entry per scenario. Each entry has:
   - `scenario` (`having_baby`, `managing_diabetes`, `simple_fracture`)
   - `source_url`, `effective_from`, `verified_on`
   - the printed total example cost and the published line total
   - claim lines, each with `line`, `date_of_service`, `cms_category`, `provider_type`, a
     description and `allowed_amount`

   Every amount is copied exactly from the CMS source, never estimated. No billing or
   diagnosis codes are stored, and CPT lines get a plain label instead of the AMA descriptor
   (director decision, 2026-11-23). The guides have no quantity column; repeat fills are
   separate lines. *(Changed while doing the task: the first draft asked for a per-line
   `service` and `quantity`.)*
3. Given each CMS category, When it is mapped to a design service row (in `category_mapping`) (`primary_care`,
   `specialist`, `diagnostic_test`, ... from section 5), Then the mapping is recorded with a
   reason. Categories that do not map cleanly are marked `needs_director` until the director decides.
4. Given the YAML, When the line amounts of each scenario are summed by hand, Then they equal
   the CMS total example cost for that scenario. The check is recorded in the research note.
5. Given the claims are **not** published in citable form, When the research ends, Then no
   YAML is written. The research note records what exists, and the task stops with a decision
   request to the director. This is the escalation path the director approved.

## Out of scope

- The `rules` YAML loader, startup validation and the `coverage_example_claim` table
  (`rules` card).
- Running reconciliation (engine card).
- Any SBC fixture (T-006).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `docs/research/cms-coverage-examples.md` | sources, quotes, calculator rules, hand checks |
| create | `src/main/resources/rules/sbc-coverage-examples.yaml` | claims data (only if criterion 2 applies) |

## Public API

none.

## Data

- Reference data file only (no table yet).
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| Hand check of each scenario's line sum against the CMS total, recorded in the note | review | 4 |
| Independent reviewer re-derives two scenarios from the CMS source | review | 2, 3 |

## Security and privacy

- Trust boundaries touched: fetched web pages (treated as data).
- Threats: TM-09 (wrong or stale rule data). The data carries `source_url` and `verified_on`.
- Personal data: none; the claims are a public CMS example.

## Gates

All gates in `/verify-gates` pass (no code changes, so this confirms nothing broke).

## Definition of done

- [ ] Every acceptance criterion met, or criterion 5's escalation raised
- [ ] Independent review: APPROVE (the reviewer checks the sources and the amounts)
- [ ] Security review: PASS
- [ ] Director merged the pull request

## Open questions

- none before research. The outcome may raise one (criterion 5).
