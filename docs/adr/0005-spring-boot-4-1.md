# 0005. Build on Spring Boot 4.1 instead of 3.5

Status: Accepted
Date: 2026-11-20
Deciders: Director

## Context

The approved design and `CLAUDE.md` said "Spring Boot 3", and T-001 first pinned Spring Boot
3.5.16 with Spring Modulith 1.4.14. While T-001 was being built (Nov 20, 2026), a support check
found that free (OSS) support for Spring Boot 3.5 ended on Jun 30, 2026 (source:
endoflife.date/spring-boot, updated Nov 20, 2026). A line without OSS support gets no free
security fixes. That works against TM-10, and a High or Critical dependency finding blocks a
release. The project was still an empty skeleton, so switching now cost one pom change.

## Options considered

### Option A: Stay on Spring Boot 3.5.16
- Pros: No change. The design and most tutorials target Boot 3.
- Cons: No free security patches. Every future CVE in Spring needs a paid support contract
  or an unplanned upgrade.

### Option B: Spring Boot 4.0.x
- Pros: A mature 4.x line.
- Cons: OSS support ends Dec 31, 2026, about five weeks after the v0.1.0 release, so an
  upgrade would follow almost at once.

### Option C: Spring Boot 4.1.x (latest patch 4.1.3)
- Pros: OSS support until Jul 31, 2027. Released Jun 30, 2026, with three patch releases, so
  it is settled.
- Cons: Boot 4 renamed and split some starters and test support compared with Boot 3, so
  some tutorial code needs adjusting.

### Option D: Spring Boot 4.2.0
- Pros: The newest line.
- Cons: Just released, with no patch releases and no published support dates found. That is
  too new for a one-week build that cannot absorb early bugs.

## Decision

Option C: Spring Boot 4.1.3 with Spring Modulith 2.1.3. Boot 3.5 is out of OSS support, 4.0
loses support five weeks after release, and 4.2.0 is too new for a one-week build.

Compatibility evidence: the Spring Modulith compatibility matrix in the reference docs
(checked Nov 20, 2026, on both the current and the 2.1 pages) is out of date. Its newest row
is "2.0 (snapshot)", and it lists no 2.1 or Boot 4.1 row. The published artifacts on Maven
Central show the pairing directly: `spring-modulith-core` 2.0.10 depends on
`spring-boot-autoconfigure` 4.0.10, 2.1.3 on 4.1.3, and 2.2.0 on 4.2.0. So the 2.1 line is
built for Boot 4.1. Spring Framework comes from the Boot BOM (7.0.x).

## Consequences

- Positive: Free security fixes until Jul 31, 2027, which covers v1 and the planned
  follow-up weeks.
- Negative or accepted risks: Code and tutorials written for Boot 3 need adjusting (for
  example the Boot 4 module starters for Flyway and test support). Each task card notes the
  starters it needs.
- Follow-ups: Update the tech stack in `CLAUDE.md` and the design (done in T-001). Plan a move
  to the next supported line before Jul 2027.
