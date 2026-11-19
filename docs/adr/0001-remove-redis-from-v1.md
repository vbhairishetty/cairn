# 0001. Remove Redis from v1

Status: Accepted
Date: 2026-11-19
Deciders: Director

## Context

The first design listed Redis to cache computed comparisons and rule lookups. The design
review (Nov 19) found three problems with it:

- The `estimate` table in Postgres already stores engine results, so Redis would be a second
  cache with no stated key or invalidation rule. A user who corrects a cost rule or edits a
  price assumption could see a stale comparison.
- The engine is pure and fast. A comparison for 2 to 4 plans and 4 scenarios is a few
  hundred `BigDecimal` operations, so caching saves nothing a user would notice.
- Version 1 must ship in 7 days on an 8 GB Mac. Each extra service adds a container, a
  health check, a Testcontainers dependency, a delete-everything target and a place where
  personal data could linger.

Redis was already last on the cut list. The review found that the scope does not fit the week,
so the director chose to cut it now rather than on the evening a day slips.

## Options considered

### Option A: Keep Redis as a cache in front of the engine
- Pros: Teaches caching. It is ready if computation ever becomes slow.
- Cons: There are two caches with no invalidation design, which risks stale numbers. It adds
  one container, about 50 MB of memory, a test dependency and one more store to wipe on
  delete-everything. It solves a performance problem Cairn does not have.

### Option B: Remove Redis and recompute (or read `estimate`) on demand
- Pros: One store for all data, so delete-everything has one less target. There is no stale
  cache to invalidate beyond `estimate`, which is rewritten whenever its inputs change.
  Compose, CI and the memory budget get simpler.
- Cons: It loses the caching lesson. If a later feature is slow, a cache must be designed
  then.

### Option C: Use Spring's in-memory cache (`@Cacheable` with a simple map)
- Pros: No new service.
- Cons: It has the same invalidation problem as Option A and still solves no real problem.

## Decision

Option B: remove Redis from v1. Estimates are computed by the pure engine on demand, and the
`estimate` table keeps the last result per plan, scenario and input hash. Correct numbers
matter more than speed Cairn does not need, and one store is easier to keep private and to
delete.

## Consequences

- Positive: The memory budget, compose file and CI are simpler. Delete-everything has one less
  store. There is no cross-store staleness.
- Negative or accepted risks: Caching is no longer a learning goal this week. A slow
  feature later would need its own cache design and ADR.
- Follow-ups: Remove Redis from the design, `CLAUDE.md` tech stack and commands, the T-003
  compose file, and the cut list (done as part of the Nov 19 design review edits).
