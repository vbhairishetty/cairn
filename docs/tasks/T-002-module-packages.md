---
id: T-002
title: Create the module root packages and the Modularity test
status: IN_PROGRESS
complexity: low
risk: low
agent: sde1-implementer
modules: [all]
branch: build/T-002-module-packages
depends_on: [T-001]
design_refs: ["docs/designs/cairn.md#4-architecture", ".claude/rules/architecture.md"]
threats: []
---

# T-002: Create the module root packages and the Modularity test

## Goal

Create the 15 application modules as Spring Modulith modules, and make the build fail whenever a
module depends on another module that `.claude/rules/architecture.md` does not allow, or
reaches into another module's internals.

## Context

T-001 created `app.cairn.CairnApplication` with Spring Boot 4.1.3 and Spring Modulith 2.1.3
(`spring-modulith-starter-core` and `spring-modulith-starter-test` are already in `pom.xml`).
There are no module packages yet. The module list is in `CLAUDE.md`. The allowed dependencies
are the table in `.claude/rules/architecture.md`; `web` and `cli` may use any module's public API.

## Acceptance criteria

1. Given the source tree, When it is listed, Then each of these packages exists with a
   `package-info.java`: `app.cairn.common`, `documents`, `jobs`, `extract`, `normalize`,
   `validate`, `engine`, `diff`, `rules`, `profile`, `timeline`, `reminders`, `ask`, `web`,
   `cli`.
2. Given each `package-info.java`, When it is read, Then it carries
   `@org.springframework.modulith.ApplicationModule` with `allowedDependencies` equal to that
   module's row in the architecture table. `common` allows none. `web` and `cli` list every
   other module. Each file has a one-line comment naming the module's job.
3. Given `src/test/java/app/cairn/ModularityTest.java`, When `./mvnw test` runs, Then
   `ApplicationModules.of(CairnApplication.class).verify()` passes.
4. Given a temporary class in `app.cairn.engine` that references a class in `app.cairn.jobs`
   (not allowed for `engine`), When `./mvnw test` runs, Then `ModularityTest` fails and names
   the violation. *(Evidence in the report, not committed.)*
5. Given a temporary class in `app.cairn.normalize` that references a class in
   `app.cairn.extract.internal`, When `./mvnw test` runs, Then `ModularityTest` fails.
   *(Evidence in the report, not committed.)*
6. Given `ModularityTest`, When it runs, Then it also asserts that the detected module names
   are exactly the 15 above, so a misspelled or missing package fails the test.

## Out of scope

- Any class inside a module other than `package-info.java` (and the probe classes, which are
  not committed).
- Spring Modulith documentation generation (`Documenter`) for diagrams (a later release task).
- Events and listeners (their first feature cards).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `src/main/java/app/cairn/<module>/package-info.java` (15 files) | module declarations with allowed dependencies |
| create | `src/test/java/app/cairn/ModularityTest.java` | Modulith verify and the module-name check |

## Public API

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"common", "validate", "rules"})
package app.cairn.engine;
```

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| `ModularityTest#modulesFollowTheAllowedDependencies` | architecture | 2, 3 |
| `ModularityTest#detectsExactlyTheFifteenModules` | architecture | 1, 6 |
| Temporary engine-to-jobs and normalize-to-extract.internal probes (not committed) | build | 4, 5 |

Test first: `ModularityTest` is written before the packages and fails (no modules or wrong
names), then passes.

## Security and privacy

- Trust boundaries touched: none.
- Threats: none.
- Personal data: none.

## Gates

All gates in `/verify-gates` must pass. With only `package-info.java` files there is no new
production code to cover.

## Definition of done

- [ ] Every acceptance criterion has a passing test or recorded evidence
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] CI green on the pull request (if T-004 has merged; otherwise the local gate, stated in the PR)
- [ ] Director merged the pull request

## Open questions

- none

## Notes

- Module names in `allowedDependencies` are the simple package names. If Spring Modulith 2.1
  needs a different form for named interfaces, keep plain module names; no named interfaces are
  used yet.
