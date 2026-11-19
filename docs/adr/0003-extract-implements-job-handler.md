# 0003. Let extract implement the jobs handler interface

Status: Accepted
Date: 2026-11-19
Deciders: Director

## Context

The `jobs` module owns the Postgres queue and the worker that claims jobs with
`FOR UPDATE SKIP LOCKED`, retries them with backoff and records their status. Someone has to
do the work of an extraction job, and that code lives in `extract`. The dependency table in
`.claude/rules/architecture.md` lets `extract` use only `common` and `documents`, and `jobs`
only `common`, so the worker had no allowed way to run an extraction. The design review
(Nov 19) found this gap.

The worker needs a result from the handler (success, a retryable failure or a permanent
failure with an error code) to decide whether to retry.

## Options considered

### Option A: `jobs` defines a `JobHandler` interface; `extract` implements it
- Pros: A plain method call with a typed result, which makes retries and error codes simple
  and easy to test. `jobs` stays generic and knows nothing about PDFs. This is the standard
  plug-in shape.
- Cons: Adds one dependency edge, `extract` to `jobs`.

### Option B: The worker publishes a `JobDue` event and `extract` listens
- Pros: No new dependency.
- Cons: Events have no return value, so success and failure would need a second event back
  to `jobs`, which `jobs` cannot see without depending on `extract`. Retry logic gets split
  across two modules.

### Option C: Put the handler interface in `common`
- Pros: No change to the dependency table.
- Cons: `common` is for value types only, with no behavior and no Spring beans. This would
  start turning it into a dumping ground.

## Decision

Option A. `app.cairn.jobs` exposes `JobHandler` (with `type()` and
`handle(JobContext) -> JobResult`) in its public API. `extract` provides the handler for job
type `extract`. The dependency table gains `extract -> jobs`. No cycle is created, because
`jobs` depends only on `common`.

The rest of the pipeline after extraction (`normalize`, `validate`, `engine`) runs as
synchronous event listeners inside the handler's transaction, after the PDF has been parsed
outside it. So a job that reports `done` means the plan, its review items and its
reconciliation exist, and any failure rolls everything back and is retried by `jobs`. We
chose this over asynchronous listeners with Spring Modulith's event publication registry,
which would leave a gap where the job is `done` but the plan is missing or stuck, and would
need a second retry mechanism.

## Consequences

- Positive: Retries, leases and error codes are owned and tested in one module. Adding
  another job type later is one new handler.
- Negative or accepted risks: `extract` now depends on the `jobs` API, so a change to
  `JobHandler` touches both.
- Follow-ups: Update the dependency table in `.claude/rules/architecture.md` (done with the
  design review edits, pending this ADR's approval). The job queue task card (Day 2) defines
  the interface.
