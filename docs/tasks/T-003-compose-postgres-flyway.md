---
id: T-003
title: Add Docker Compose Postgres, Flyway and the first migration
status: READY
complexity: low
risk: medium
agent: sde1-implementer
modules: [documents]
branch: build/T-003-compose-postgres-flyway
depends_on: [T-001]
design_refs: ["docs/designs/cairn.md#5-data-model", "docs/designs/cairn.md#8-privacy-reliability-testing-cicd-observability-and-git"]
threats: [TM-07]
---

# T-003: Add Docker Compose Postgres, Flyway and the first migration

## Goal

Give Cairn its database: a local Postgres 16 with pgvector, started by Docker Compose and
reachable only from this machine, plus Flyway with one schema per module and the first real
table (`documents.document`). Every later card that stores data builds on this.

## Context

T-001 created the Maven skeleton (`pom.xml`, `CairnApplication`, `CairnApplicationTest`) with
no database. The design (section 5) fixes the conventions:
- One Flyway instance. `spring.flyway.schemas` lists module schemas, `spring.flyway.locations`
  lists `classpath:db/migration/<module>` folders, and migrations use schema-qualified names.
- Versions are timestamps (`V202611191400__documents_create_document.sql`).
- IDs are `uuid`, timestamps are `timestamptz`, and enums are `text` with a check constraint.

Redis was removed (ADR 0001). Mailpit is a stretch goal and is not added here. Section 8 and
TM-07 require every published Compose port to bind to `127.0.0.1`.

Docker runs on Colima. Testcontainers may need `DOCKER_HOST` and
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` set for Colima. Document what
worked in the report, and do not commit machine-specific paths.

## Acceptance criteria

1. Given the repository, When `docker compose up -d postgres` runs, Then a Postgres 16 container
   with the pgvector extension available starts and becomes healthy (`pg_isready` health
   check). Its port is published as `127.0.0.1:5432:5432`, and data lives in a named volume.
2. Given `docker-compose.yml`, When `ComposePortsTest` parses it, Then every entry under every
   service's `ports` starts with `127.0.0.1:`. A port without a host address, or with
   `0.0.0.0`, fails the test. (TM-07)
3. Given the app's configuration, When it starts, Then the datasource URL, user and password
   come from the environment variables `CAIRN_DB_URL` (default
   `jdbc:postgresql://127.0.0.1:5432/cairn`), `CAIRN_DB_USER` and `CAIRN_DB_PASSWORD`. No
   password value is committed. Compose requires `CAIRN_DB_PASSWORD`: without it,
   `docker compose up` stops with a clear message (`${CAIRN_DB_PASSWORD:?...}`). `.env.example`
   shows the variable with a placeholder (director decision, option (a)).
4. Given an empty database, When the app or a test starts, Then Flyway creates the
   `documents` schema and applies
   `db/migration/documents/V202611191400__documents_create_document.sql`. It creates
   `documents.document` with these columns: `id uuid primary key`; `kind text not null check
   (kind in ('sbc','dental','vision','brochure'))`; `sha256 char(64) not null unique`;
   `bytes bigint not null check (bytes > 0)`; `page_count int not null check (page_count > 0)`;
   `storage_name text not null unique`; `uploaded_at timestamptz not null`.
5. Given the migrated schema, When `DocumentSchemaIT` inserts two rows with the same `sha256`,
   Then the second insert fails with a unique violation.
6. Given the migrated schema, When `DocumentSchemaIT` inserts a row with `kind = 'invoice'`,
   Then the insert fails with a check violation.
7. Given Testcontainers, When `CairnApplicationTest#contextLoads` runs, Then it starts against a
   `pgvector/pgvector` container through `@ServiceConnection` (the same image tag as Compose),
   and the context loads with Flyway applied.
8. Given a second migration folder added later for another module, When Flyway runs, Then both
   folders are picked up. This is shown by `spring.flyway.locations` and
   `spring.flyway.schemas` being lists in `application.yml`, with a comment that each new module
   appends its entry.

## Out of scope

- The app service in Compose and the Dockerfile (Day 6).
- Mailpit and Redis (stretch goal, and removed by ADR 0001).
- Any repository, entity or upload code for `documents` (Day 2 upload card).
- Tables for other modules.
- The `vector` extension itself (`CREATE EXTENSION vector` comes with the Ask Cairn stretch;
  only the image must provide it).
- CI changes (T-004).

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| modify | `docs/designs/cairn.md` | "Done means" fresh-clone wording (director-approved) |
| create | `docker-compose.yml` | `postgres` service, pinned `pgvector/pgvector:<exact>-pg16` tag, `127.0.0.1` port, health check, named volume, password from env |
| create | `.env.example` | Variable names with placeholder values, no real secret |
| modify | `pom.xml` | `spring-boot-starter-jdbc`, `org.postgresql:postgresql`, `flyway-core`, `flyway-database-postgresql`; test: `spring-boot-testcontainers`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter` (versions from the Boot BOM) |
| modify | `src/main/resources/application.yml` | datasource from env vars; Flyway schemas and locations lists |
| create | `src/main/resources/db/migration/documents/V202611191400__documents_create_document.sql` | first migration |
| create | `src/test/java/app/cairn/TestcontainersConfiguration.java` | `@TestConfiguration` with a `@ServiceConnection` `PostgreSQLContainer` (the pgvector image declared compatible with `postgres`) |
| modify | `src/test/java/app/cairn/CairnApplicationTest.java` | import the Testcontainers configuration |
| create | `src/test/java/app/cairn/ComposePortsTest.java` | TM-07 compose check (SnakeYAML, already on the classpath through Spring Boot) |
| create | `src/test/java/app/cairn/documents/DocumentSchemaIT.java` | migration and constraint checks with `JdbcClient` |

## Public API

none (infrastructure only).

## Data

- Tables and migrations: `documents.document` as in criterion 4, file
  `db/migration/documents/V202611191400__documents_create_document.sql`.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| `ComposePortsTest#everyPublishedPortBindsToLoopback` | unit (security, TM-07) | 2 |
| `DocumentSchemaIT#migrationCreatesDocumentTable` | integration | 4, 7 |
| `DocumentSchemaIT#duplicateSha256IsRejected` | integration | 5 |
| `DocumentSchemaIT#unknownKindIsRejected` | integration | 6 |
| `CairnApplicationTest#contextLoads` | integration (Testcontainers) | 3, 7 |
| `docker compose up -d postgres` then `docker compose ps` (healthy) | manual, evidence in report | 1 |

Test-first evidence: `ComposePortsTest` and `DocumentSchemaIT` are written first and fail
(no compose file, no migration), then pass.

## Security and privacy

- Trust boundaries touched: the database and the network surface of the local machine.
- Threats: TM-07. Compose publishes only on `127.0.0.1`, and `ComposePortsTest` guards it.
- Secrets: no password in any committed file. `.env` is ignored by git (T-001), and
  `.env.example` holds placeholders only. Agents must not read or print `.env`.
- Personal data: none yet. The `document` table will hold only metadata about uploaded files
  (no file names, per section 5).

## Gates

All gates in `/verify-gates` must pass. `./mvnw verify` now needs Docker running for the
`*IT` and context tests.

## Definition of done

- [ ] Every acceptance criterion has a passing test or recorded evidence
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE
- [ ] Security review: PASS
- [ ] CI green on the pull request (or the local gate, if T-004 has not merged yet, stated in
      the pull request)
- [ ] Director merged the pull request

## Open questions

- none. The director chose option (a) on Nov 20: Compose requires `CAIRN_DB_PASSWORD`, and
  there is no committed default. The director also approved the matching "Done means" wording
  in the design. This PR updates it to: "A fresh clone starts with `cp .env.example .env`,
  setting `CAIRN_DB_PASSWORD`, and then `docker compose up`".

## Notes

- Pin the image to an exact tag (for example `pgvector/pgvector:0.8.x-pg16`, the newest
  available), not `latest`.
- Do not set `spring.flyway.create-schemas=false`. Flyway creates the listed schemas.
- Spring Boot 4.1 split auto-configuration into modules. Use `spring-boot-starter-flyway` for
  Flyway, plus the Boot 4.1 Testcontainers support artifacts. Check the names against the Boot
  4.1.3 BOM rather than Boot 3 tutorials.
- JaCoCo already merges unit and integration coverage (T-001). `DocumentSchemaIT` coverage
  counts.
- Probes and builds that download artifacts must not leave test artifacts in `~/.m2`.
