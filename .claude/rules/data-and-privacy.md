# Data and privacy rules

Cairn's promise is that personal health-plan data never leaves the user's machine and that
Cairn keeps as little of it as possible.

## Minimization

- Store only what a feature needs. The profile holds an age band, a coverage tier, and
  optionally a visa category. No names, birth dates, member IDs, Social Security numbers,
  addresses or medical history.
- A new personal field requires a task card that explains why it is needed and an update to
  this file.

## Where data may go

- Personal data stays in the local Postgres database and the local file store.
- It is never sent to a remote service, never included in telemetry, and never written to
  logs, metrics labels, exception messages, issues, pull requests or commit messages.
- Metrics use counts and durations only, never values.

## Documents

- Uploaded PDFs are stored under generated names in the configured data directory.
- `raw_cell` keeps text extracted from documents because provenance needs it. It is
  personal data and follows every rule in this file.
- The "delete everything" action and `cairn delete-all` pause the job worker, then remove
  every user-data row (documents, jobs, extracted and normalized data, review items,
  estimates, reconciliations, reminders, timeline events, profile and embeddings) and every
  stored file, and reset price assumptions to their seed values. Reference data in `rules` is
  kept. The test for it checks every user-data table and the data directory.
- No email address is stored. If the email reminder stretch ships, the address comes from
  configuration.
- The free-benefits checklist does not use sex; items that depend on it are labeled.

## Test data

- Fixtures are public SBCs or synthetic documents, with sources recorded in
  `src/test/resources/fixtures/sbc/SOURCES.md`.
- Synthetic profiles use obviously fake values.
- A test scans log output during the test run for known fixture personal values and fails if
  it finds one.

## Rules data

- Legal and policy rules in `src/main/resources/rules/` are public facts with a
  `source_url`, `effective_from`, optional `effective_to`, and `verified_on`. A legal rule's
  `source_url` is a primary source (CMS, IRS, USCIS, DHS or the Federal Register), not a blog.
- A rule is never edited in place when the law changes; a new entry is added with new dates.
- Rules not re-verified within 90 days are flagged in the UI as needing review.

## Retention

- Reminders that were sent are kept so they are not sent twice; the user can delete them.
- Extraction jobs older than 30 days keep only their status and error code, not document text.
