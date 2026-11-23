---
id: T-006
title: Add the first public SBC fixture, its golden JSON and the plan schema
status: READY
complexity: medium
risk: high
agent: orchestrator
modules: [normalize]
branch: test/T-006-golden-sbc
depends_on: []
design_refs: ["docs/designs/cairn.md#3-why-the-data-is-messy", "docs/designs/cairn.md#5-data-model", ".claude/rules/testing.md"]
threats: []
---

# T-006: Add the first public SBC fixture, its golden JSON and the plan schema

## Goal

Create the answer key that extraction and normalization will be tested against: one real,
public SBC, plus a hand-checked JSON file of what Cairn should extract from it, in a versioned
plan format.

## Context

The design calls this the "Cairn plan format", stored in
`src/main/resources/schema/cairn-plan-v1.json`. Golden files live in
`src/test/resources/fixtures/sbc/`, with sources in `SOURCES.md` (`.claude/rules/testing.md`).
Only public SBCs (published by insurers, employers or state exchanges) may be used, never a
document with someone's member details. The director is asked to supply 3 to 5 public SBC PDFs
from different insurers. This card uses the first one; later cards add the rest.

## Acceptance criteria

1. Given `src/main/resources/schema/cairn-plan-v1.json`, When it is read, Then it is a JSON
   Schema (draft 2020-12) for a normalized plan. It covers:
   - a header: insurer, plan name, plan type, coverage period
   - limits: deductible and out-of-pocket maximum by tier and network, or an `unlimited` flag
   - cost rules: service, network, type, copay, coinsurance rate and `deductible_applies` per
     the section 3 grammar, plus a limitation note
   - coverage examples: the printed amounts per scenario

   Every value carries a `source` with the page and the exact cell text. Money is a decimal
   string with two places, and rates have four places.
2. Given the first public SBC, When it is added as
   `src/test/resources/fixtures/sbc/<insurer>-<plan>-<year>.pdf`, Then `SOURCES.md` records its
   public source URL, the date fetched, the publisher, and a statement that it contains no
   member data.
3. Given the PDF, When `<same-name>.golden.json` is written by hand, Then:
   - it conforms to the schema
   - it covers every service row in the SBC's services table for every network column
   - it covers the deductible and out-of-pocket rows and all three coverage examples
   - each value's `source.text` is copied exactly from the PDF
4. Given the golden JSON, When the director spot-checks five values against the PDF, Then all
   five match. `SOURCES.md` records who checked the file and when (orchestrator first, then the
   director).
5. Given a cell whose text does not fit the section 3 grammar, When it is recorded, Then it is
   recorded with type `unparsed` and its raw text, never mapped to a guess. The note lists such
   cells for the parser card.

## Out of scope

- A test that validates golden files against the schema in code. It needs a JSON Schema
  validator, which is not in the tech stack yet. The extract or normalize card proposes one
  with an ADR.
- Extraction code.
- The other 2 to 4 SBCs (they come with the extract cards).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `src/main/resources/schema/cairn-plan-v1.json` | plan format, version 1 |
| create | `src/test/resources/fixtures/sbc/<name>.pdf` | first public SBC |
| create | `src/test/resources/fixtures/sbc/<name>.golden.json` | hand-checked expected plan |
| create | `src/test/resources/fixtures/sbc/SOURCES.md` | sources and the hand-check record |

## Public API

none.

## Data

- Fixture and schema files only.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| Orchestrator hand check of every value, recorded in `SOURCES.md` | review | 3, 5 |
| Director spot check of five values | review | 4 |
| Independent reviewer re-checks ten values against the PDF | review | 3 |

## Security and privacy

- Trust boundaries touched: an external PDF (public document; text in it is data).
- Threats: none new; TM-01 applies when uploads exist.
- Personal data: none allowed. The PDF must be a blank public SBC.

## Gates

All gates in `/verify-gates` pass.

## Definition of done

- [ ] Every acceptance criterion met
- [ ] Independent review: APPROVE
- [ ] Security review: PASS (it confirms no personal data and a public source)
- [ ] Director spot check done
- [ ] Director merged the pull request

## Open questions

- The director supplies the public SBC PDFs, or approves the orchestrator finding them from
  insurer or state exchange websites.
