---
id: T-001
title: Create the Maven project skeleton with build gates
status: READY
complexity: low
risk: low
agent: junior-implementer
modules: [app]
branch: build/T-001-maven-skeleton
depends_on: []
design_refs: ["docs/designs/cairn.md#4-architecture", "docs/designs/cairn.md#11-quality-and-security-gates"]
threats: []
---

# T-001: Create the Maven project skeleton with build gates

## Goal

Create the empty Spring Boot application and the build that every later task runs through:
`./mvnw verify` compiles with Error Prone, checks formatting with Spotless, runs unit and
integration tests, and enforces JaCoCo coverage. After this card, every gate in
`/verify-gates` exists and passes on an empty project.

## Context

The repository holds only documentation (`CLAUDE.md`, `.claude/`, `docs/`, `TASKS.md`,
`scripts/git-hooks/`). There is no `pom.xml` and no `src/`. The tech stack and commands are
fixed in `CLAUDE.md`, and test naming and coverage thresholds in `.claude/rules/testing.md`.
The base package is `app.cairn`. Modules are added by later cards (T-002 creates the module
root packages and the Modularity test), so this card creates only the application class.

Local toolchain (verified 2026-11-19): OpenJDK 21.0.13, Maven 3.10.1, and Docker 29.5.2 on
Colima. No database is needed for this card.

## Acceptance criteria

1. Given a fresh clone with JDK 21, When `./mvnw -v` runs, Then the Maven wrapper downloads
   and reports a pinned Maven version (no system Maven needed).
2. Given the skeleton, When `./mvnw verify` runs, Then the build succeeds, and the output shows
   Spotless check, compilation with Error Prone, Surefire, Failsafe and the JaCoCo check
   running.
3. Given the application, When `CairnApplicationTest#contextLoads` runs, Then the Spring
   context starts with no errors.
4. Given a Java file with formatting that violates Google Java Style, When
   `./mvnw spotless:check` runs, Then it fails and names the file. *(Evidence in the report;
   the bad file is not committed.)*
5. Given a Java file containing a bug that Error Prone flags as an error (for example
   `"a" == someString`, which triggers `StringEquality`, or an ignored return value from
   `String.trim()`, which triggers `ReturnValueIgnored`), When `./mvnw compile` runs, Then
   compilation fails with the Error Prone check name. *(Evidence in the report; not
   committed.)*
6. Given the JaCoCo configuration, When `./mvnw verify` runs, Then the `check` goal runs with
   these rules: line coverage of at least 0.70 for the bundle, and at least 0.80 for the
   packages `app.cairn.engine*`, `app.cairn.normalize*`, `app.cairn.validate*`,
   `app.cairn.timeline*` and `app.cairn.diff*`. Packages with no classes yet do not fail the
   build.
7. Given test classes named `*Test`, `*Properties` and `*GoldenTest`, When `./mvnw test` runs,
   Then Surefire runs them. Given `*IT` classes, Then Failsafe runs them during `verify` and
   not during `test`. `*ApiTest` and `*SecurityTest` also run under Surefire.
8. Given the `.gitignore`, When `git status` runs after a build, Then `target/`, `.env`,
   `.env.*` (except `.env.example`), IDE folders and the local `data/` directory are not
   listed.

## Out of scope

- Module root packages and `ModularityTest` (T-002).
- Postgres, Flyway, Testcontainers, `docker-compose.yml` and the datasource (T-003).
- CI workflow and Dependabot (T-004).
- Spring Security, springdoc-openapi, iCal4j, PDFBox, Tabula and picocli dependencies: added by
  the card that first uses each one.
- Bind address, security headers and actuator exposure (Day 2 local-network card).
- Dockerfile (Day 6).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `pom.xml` | Spring Boot parent, dependencies, plugins and gates |
| create | `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | Maven wrapper (pinned Maven version) |
| create | `.mvn/jvm.config` | `--add-exports` and `--add-opens` flags that Error Prone needs on JDK 21 |
| create | `src/main/java/app/cairn/CairnApplication.java` | `@SpringBootApplication` entry point (with `@Modulithic` if useful for T-002) |
| create | `src/main/resources/application.yml` | `spring.application.name: cairn` only |
| create | `src/test/java/app/cairn/CairnApplicationTest.java` | context-load test |
| create | `.gitignore` | build output, secrets, IDE files, local data |

## Public API

```java
package app.cairn;

@SpringBootApplication
public class CairnApplication {
  public static void main(String[] args);
}
```

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Build configuration (pom.xml)

- Parent: `org.springframework.boot:spring-boot-starter-parent`, the latest 3.5.x release
  available in Maven Central (pinned exactly). `java.version` is 21.
- Dependencies: `spring-boot-starter`, `spring-modulith-starter-core` (version from the
  `spring-modulith-bom`, latest 1.4.x, imported in `dependencyManagement`). Test:
  `spring-boot-starter-test` (JUnit 5, AssertJ, Mockito), `spring-modulith-starter-test`, and
  `net.jqwik:jqwik` (pinned).
- Plugins, each with an exact version: `spotless-maven-plugin` (Google Java Format, `check`
  bound to `verify`, or to `validate` if earlier failure is preferred), `maven-compiler-plugin`
  with Error Prone (`-XDcompilePolicy=simple`, `-Xplugin:ErrorProne`, Error Prone core pinned),
  `maven-surefire-plugin` and `maven-failsafe-plugin` with the include patterns from criterion
  7, and `jacoco-maven-plugin` (`prepare-agent`, `prepare-agent-integration`, `report`,
  `check`).
- No `LATEST`, `RELEASE` or version ranges anywhere (`.claude/rules/security.md`).

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| `CairnApplicationTest#contextLoads` | unit (Spring context) | 3 |
| `./mvnw verify` output | build | 2, 6, 7 |
| Temporary bad-format and Error Prone files (not committed) | build | 4, 5 |
| `git status` after build | manual check | 8 |

Test-first evidence: `CairnApplicationTest` is written first and fails to compile (no
`CairnApplication`), then passes.

## Security and privacy

- Trust boundaries touched: none.
- Threats: none directly. TM-10 (vulnerable dependency) is supported by exact version pins.
- Personal data: none.
- `.gitignore` must exclude `.env` files so secrets are never committed.

## Gates

All gates in `/verify-gates` must pass. Coverage thresholds are configured as in criterion 6.
With only `CairnApplication` present, the bundle threshold is met or the class is excluded
from the bundle rule with a comment explaining why (an entry point with no logic).

## Definition of done

- [ ] Every acceptance criterion has passing evidence
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] CI green on the pull request (CI arrives with T-004; until then the orchestrator's local
      `./mvnw verify` is the gate, and the pull request says so)
- [ ] Director merged the pull request

## Open questions

- none

## Notes

- Use `./mvnw spotless:apply` before committing; a formatting-only change goes in its own
  commit.
- Commit messages follow `build(app): ...` and carry no AI credit lines. The commit-msg hook
  in `scripts/git-hooks/` enforces this, enabled with `git config core.hooksPath
  scripts/git-hooks`.
- Error Prone on JDK 21 needs the `--add-exports jdk.compiler/...` lines in `.mvn/jvm.config`.
  See the Error Prone installation docs for Maven.
