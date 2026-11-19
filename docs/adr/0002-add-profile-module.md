# 0002. Add a profile module

Status: Accepted
Date: 2026-11-19
Deciders: Director

## Context

Cairn stores one small set of personal settings: an age band, a coverage tier and an optional
visa category. The design review (Nov 19) found that no module owned the `profile` table.
`reminders` needs the age band for the free-benefits checklist. `web` and `cli` need to read
and edit the profile. The rules require each table to have exactly one owning module and
forbid reading another module's tables.

## Options considered

### Option A: A new `profile` module that depends only on `common`
- Pros: One clear owner for the only personal settings. It is easy to find, delete and audit,
  and other modules read it through a two-method API.
- Cons: One more module and schema for a single row.

### Option B: Put the profile in `reminders`
- Pros: No new module.
- Cons: `web` and `cli` would edit reminder internals to change a coverage tier. The visa
  category, which turns on the OPT path, would live in a module that has nothing to do with
  OPT.

### Option C: Put the profile in `web`
- Pros: No new module.
- Cons: `reminders` would then depend on `web`, which the dependency table forbids, and the
  CLI would depend on the web layer.

## Decision

Option A: add `app.cairn.profile`, which owns `profile.profile` (one row) and depends only on
`common`. `reminders` may depend on `profile`. Like every module, `web` and `cli` may use its
public API. The engine does not read the profile: the caller passes the coverage tier in.

## Consequences

- Positive: Data ownership is unambiguous, and delete-everything has an obvious target.
- Negative or accepted risks: A slightly larger module map.
- Follow-ups: Add `profile` to the module list in `CLAUDE.md` and to the dependency table in
  `.claude/rules/architecture.md` (done with the design review edits, pending this ADR's
  approval). The module root package is created in T-002.
