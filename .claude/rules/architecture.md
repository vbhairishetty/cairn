# Architecture rules

Cairn is a modular monolith: one Spring Boot application, one PostgreSQL database, and
strict internal modules. These rules keep the modules independent so the code stays easy
to change, test and, if ever needed, split.

## Module shape

- A module is a direct subpackage of `app.cairn` (for example `app.cairn.engine`).
- Classes in the module root package are its **public API**. Keep the API small: interfaces,
  records for inputs and outputs, and events.
- Everything else lives under `<module>.internal` and must not be referenced from another
  module. Spring Modulith treats subpackages as internal, and the verify test fails the build
  if another module reaches into them.
- `common` holds shared value types only (`Money`, `SourceRef`, `Service`, `Network`,
  `LimitTier`, `CoverageTier`).
  It has no Spring beans, no database access and no dependencies on other modules.

## Allowed dependencies

Arrows point from a module to the modules it may use. Anything not listed is forbidden.
Cycles are forbidden.

| Module | May depend on |
| --- | --- |
| `common` | nothing |
| `documents` | `common` |
| `jobs` | `common` |
| `extract` | `common`, `documents`, `jobs` (implements `JobHandler`, ADR 0003) |
| `normalize` | `common`, `extract` |
| `validate` | `common`, `normalize` |
| `engine` | `common`, `validate` (reads validated plans only), `rules` (reference data, ADR 0004) |
| `diff` | `common`, `validate` |
| `rules` | `common` |
| `profile` | `common` (ADR 0002) |
| `timeline` | `common`, `rules` |
| `reminders` | `common`, `validate`, `rules`, `profile` (ADR 0002) |
| `ask` | `common`, `engine`, `validate`, `rules` |
| `web` | any module's public API except `cli`, never internals |
| `cli` | any module's public API except `web`, never internals |

If a task needs a dependency that is not in this table, stop and propose an ADR instead of
adding it.

## Communication between modules

- Use a direct call to another module's public API when the caller needs an answer now
  (for example `engine` asking `validate` for a validated plan).
- Use an application event when a module reacts to something that already happened (for
  example `normalize` reacting to `CellsExtracted`). Events are records in the publishing
  module's root package, named in past tense, and carry IDs only.
- The extraction pipeline (`CellsExtracted`, `PlanNormalized`, `PlanValidated`) uses
  synchronous listeners in the job's transaction, so a failure rolls back and the job retries
  (ADR 0003). No event publication registry is used.
- Event listeners must be idempotent: handling the same event twice has the same result as
  handling it once.

## Data ownership

- Each module owns its tables in its own Postgres schema (`documents.document`,
  `engine.estimate`, and so on) and its own Flyway folder
  `src/main/resources/db/migration/<module>/`.
- A module never reads or writes another module's tables. It asks the owning module's API.
- Migrations are append-only. Never edit a migration that has been merged; add a new one.
- Raw extraction data (`raw_cell`) is immutable. Fixes happen by re-running extraction.
- The normalized plan format is described by a versioned JSON Schema in
  `src/main/resources/schema/cairn-plan-v1.json`. Changing it requires a new version number.

## Design principles

- Prefer simple, explicit code over clever abstractions. A reader should understand a class
  in one pass.
- Extract a shared abstraction only when the same need appears in three real places (rule of
  three). Two similar blocks of code are acceptable.
- Pure logic (engine, parser, timeline math, diff) has no I/O and no Spring annotations, so it
  can be unit tested without a container.
- Side effects (database, files, network, time) sit behind small interfaces at the edge of the
  module. Inject `java.time.Clock`; never call `LocalDate.now()` directly.
- Configuration uses typed `@ConfigurationProperties` records with validation, not scattered
  `@Value` strings.
- Fail fast at startup on invalid configuration or invalid rule data.

## Decision records

- Any choice that a future reader would ask "why?" about gets an ADR in `docs/adr/` using
  `/adr`. Examples: a new dependency, a new module, a change to a public API, a schema change
  that affects more than one table, a security trade-off.
- An ADR states the context, the options considered, the decision, and its consequences.
- Code that implements an ADR references it in the pull request summary.

## What the orchestrator checks before accepting a change

- The Spring Modulith verify test passes.
- No class outside a module references `<module>.internal`.
- New tables are in the owning module's schema with a migration in that module's folder.
- No new dependency or module appears without an approved ADR.
