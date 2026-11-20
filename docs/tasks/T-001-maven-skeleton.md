---
id: T-001
title: Create the Maven project skeleton with build gates
status: IN_REVIEW
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
5. Given a Java file containing a bug that Error Prone flags as an error, When
   `./mvnw compile` runs, Then compilation fails with the Error Prone check name. Three probes,
   each must fail the build (evidence in the report; probe files are not committed):
   an ignored return value from `String.trim()` (`ReturnValueIgnored`); `"a" == someString`
   (`ReferenceEquality`, raised to ERROR with `-Xep:ReferenceEquality:ERROR`); and
   `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` (`BigDecimalEquals`, raised to ERROR
   with `-Xep:BigDecimalEquals:ERROR`). Changed in round 2 by director decision: the first
   version of this card wrongly said `"a" == s` fails by default; it only warned.
6. Given a temporary `*IT` test class, When `./mvnw verify` runs, Then Failsafe runs with the
   JaCoCo agent attached and `target/jacoco-it.exec` is created (round 2, review finding R1:
   `prepare-agent-integration` must set `propertyName` to `failsafeArgLine`, read by Failsafe
   with late replacement `@{failsafeArgLine}`). The coverage `check` evaluates unit and
   integration data together (merge the two `.exec` files, or point `check` at both).
7. Given the JaCoCo configuration, When `./mvnw verify` runs, Then the `check` goal runs with
   these rules: line coverage of at least 0.70 for the bundle, and at least 0.80 for the
   packages `app.cairn.engine*`, `app.cairn.normalize*`, `app.cairn.validate*`,
   `app.cairn.timeline*` and `app.cairn.diff*`. Packages with no classes yet do not fail the
   build.
8. Given test classes named `*Test`, `*Properties` and `*GoldenTest`, When `./mvnw test` runs,
   Then Surefire runs them. Given `*IT` classes, Then Failsafe runs them during `verify` and
   not during `test`. `*ApiTest` and `*SecurityTest` also run under Surefire.
9. Given the `.gitignore`, When `git status` runs after a build, Then `target/`, `.env`,
   `.env.*` (except `.env.example`), IDE folders, the local `data/` directory and
   `.claude/agent-memory/` (agent notes, not committed for now) are not listed.
10. Given `.mvn/wrapper/maven-wrapper.properties`, When the wrapper downloads Maven, Then it
   verifies `distributionSha256Sum` (round 2, finding R4). Maven Central publishes no `.sha256`
   for the Maven zip (404; `.sha512` and `.sha1` exist), so by director decision the value is
   the SHA-256 of a zip first verified against the published `.sha512`. The report shows the
   `.sha512` match and the computed SHA-256.

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

- Parent: `org.springframework.boot:spring-boot-starter-parent` **4.1.3** (ADR 0005; round 2
  changed this from 3.5.16). `java.version` is 21.
- Dependencies: `spring-boot-starter`, `spring-modulith-starter-core` (version from the
  `spring-modulith-bom` **2.1.3**, imported in `dependencyManagement`; Modulith 2.1.3 is
  built against Boot 4.1.3). Test:
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
| `BuildConfigurationTest#errorProneTreatsReferenceAndBigDecimalEqualityAsErrors` | unit (reads `pom.xml`) | 5: guards the two `-Xep:...:ERROR` flags so they cannot be removed silently |
| Temporary `ReferenceEquality`, `BigDecimalEquals` and `ReturnValueIgnored` probes (not committed) | build | 5 |
| Temporary `*IT` probe showing `target/jacoco-it.exec` (not committed) | build | 6 |
| `./mvnw clean verify` output | build | 2, 7, 8 |
| Temporary bad-format and Error Prone files (not committed) | build | 4, 5 |
| `git status` after build | manual check | 9 |
| Wrapper tamper probe: one changed digit in `distributionSha256Sum` is rejected (not committed) | manual check | 10 |

Test-first evidence: `CairnApplicationTest` is written first and fails to compile (no
`CairnApplication`), then passes.

## Security and privacy

- Trust boundaries touched: none.
- Threats: none directly. TM-10 (vulnerable dependency) is supported by exact version pins.
- Personal data: none.
- `.gitignore` must exclude `.env` files so secrets are never committed.

## Gates

All gates in `/verify-gates` must pass. Gate 4 runs `./mvnw clean verify` (director
decision, round 2: JaCoCo appends to its data files, so a plain `verify` can count stale
coverage). Coverage thresholds are configured as in criterion 7.
With only `CairnApplication` present, the class may be excluded with the exact pattern
`app/cairn/CairnApplication.class` (not a wildcard) and an accurate comment (round 2, finding
R3).

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
