---
id: T-005
title: Add the Money and SourceRef value types
status: READY
complexity: medium
risk: high
agent: sde2-implementer
modules: [common]
branch: feat/T-005-money-sourceref
depends_on: [T-002]
design_refs: ["docs/designs/cairn.md#4-architecture", "docs/designs/cairn.md#5-data-model", "docs/designs/cairn.md#6-cost-engine-and-how-we-prove-it-is-correct"]
threats: []
---

# T-005: Add the Money and SourceRef value types

## Goal

Give every module one safe type for dollar amounts and one type for "where a number came
from", so money is never a `double` and every number can carry its source (`CLAUDE.md` rules 4
and 5).

## Context

`app.cairn.common` exists after T-002 and may depend on nothing. Design facts:
- Money is `numeric(12,2)` in Postgres and `BigDecimal` in Java.
- The engine rounds to cents, half up, per event (section 6).
- A yearly total may be negative (a net gain when the HSA contribution exceeds costs).
- `SourceRef` has four kinds (section 4): a document cell (a `raw_cell` id), a value the user
  entered (the field and when), a labeled assumption (a price assumption and its source), and
  a computed value (the breakdown lines it was built from).

`common` holds value types only: no Spring, no database. Error Prone fails the build on
`BigDecimal.equals` and on `==` between objects.

## Acceptance criteria

1. Given `Money.of(new BigDecimal("30"))`, When it is read, Then its amount is `30.00`, with
   scale 2. Given `Money.of(new BigDecimal("30.005"))`, Then it throws
   `IllegalArgumentException`: more than two decimals is a bug, not a rounding choice. There is
   no factory that takes `double` or `float`.
2. Given `Money.of("1500.00")` and `Money.of("1500")`, When compared with `equals`, Then they
   are equal and have the same `hashCode`, because scale is normalized to 2.
3. Given `$1,000.00 + $250.50`, Then `plus` gives `$1,250.50`. Given `$100.00 - $250.00`, Then
   `minus` gives `-$150.00` (negative values are allowed).
4. Given `$12,700.00` times rate `0.2000` (`Money.times(BigDecimal rate)`), Then the result is
   `$2,540.00`. Given `$33.33 × 0.5000`, Then the result is `$16.67` (exact value 16.665,
   rounded half up to cents).
5. Given `min($30.00, $25.00)`, Then `$25.00`. `Money.zero()` is `$0.00`. `isNegative`,
   `isZero` and `compareTo` behave as expected.
6. Given a `Money` amount with more than 10 integer digits, When it is created, Then it throws
   `IllegalArgumentException`, because `numeric(12,2)` allows 10 integer digits.
7. Given `SourceRef`, When it is used, Then it is a sealed interface with exactly four record
   implementations:
   - `DocumentCell(UUID rawCellId)`
   - `UserEntered(String field, Instant enteredAt)`
   - `Assumption(String label, String source)`
   - `Computed(List<SourceRef> parts)`, where `parts` is non-empty, copied and unmodifiable

   Null or blank components throw `NullPointerException` or `IllegalArgumentException` with a
   message that names the field.
8. Given the properties in `MoneyProperties` (fixed seed, 1,000 cases each, per
   `.claude/rules/testing.md` and ADR 0006), When they run, Then they hold:
   - `plus` is commutative and associative
   - `a.plus(b).minus(b)` equals `a`
   - `times(rate)` with `0 ≤ rate ≤ 1` gives a result between zero and the amount (inclusive)
     for non-negative amounts
   - `times` never changes the scale from 2
9. Given the generator used by `MoneyProperties`, When its own unit test runs, Then it only
   produces valid `Money` values (scale 2, within range) and covers negatives, zero and the
   maximum.

## Out of scope

- Parsing money from SBC text such as `"$1,500"` (normalize card).
- `Service`, `Network`, `LimitTier` and `CoverageTier` enums (each added with the first card
  that uses it).
- JSON or JDBC mapping of `Money` (repository cards).
- Currency: Cairn is USD only and `Money` holds no currency field.

## Files

| Action | Path | Purpose |
| --- | --- | --- |
| create | `src/main/java/app/cairn/common/Money.java` | money value type |
| create | `src/main/java/app/cairn/common/SourceRef.java` | sealed interface and its four records |
| create | `src/test/java/app/cairn/common/MoneyTest.java` | criteria 1 to 6 |
| create | `src/test/java/app/cairn/common/SourceRefTest.java` | criterion 7 |
| create | `src/test/java/app/cairn/common/MoneyProperties.java` | criterion 8 |
| create | `src/test/java/app/cairn/common/MoneyGenerator.java` and `MoneyGeneratorTest.java` | criterion 9 |

## Public API

```java
public record Money(BigDecimal amount) implements Comparable<Money> {
  public static Money of(BigDecimal amount);
  public static Money of(String amount);
  public static Money zero();
  public Money plus(Money other);
  public Money minus(Money other);
  public Money times(BigDecimal rate);
  public static Money min(Money a, Money b);
  public boolean isNegative();
  public boolean isZero();
}

public sealed interface SourceRef {
  record DocumentCell(UUID rawCellId) implements SourceRef {}
  record UserEntered(String field, Instant enteredAt) implements SourceRef {}
  record Assumption(String label, String source) implements SourceRef {}
  record Computed(List<SourceRef> parts) implements SourceRef {}
}
```

## Data

- Tables and migrations: none.
- Events published or consumed: none.

## Tests required

| Test | Level | Covers criteria |
| --- | --- | --- |
| `MoneyTest` (parameterized tables for arithmetic and rounding) | unit | 1 to 6 |
| `SourceRefTest` | unit | 7 |
| `MoneyProperties` | property (seeded) | 8 |
| `MoneyGeneratorTest` | unit | 9 |

## Security and privacy

- Trust boundaries touched: none.
- Threats: none.
- Personal data: none. `UserEntered.field` names a field, such as `premium_per_paycheck`, never a
  value the user typed.

## Gates

All gates in `/verify-gates` must pass. `common` must reach the overall 70% line coverage; aim
for full coverage, since it is pure logic.

## Definition of done

- [ ] Every acceptance criterion has a passing test
- [ ] Gates pass, verified by the orchestrator
- [ ] Independent review: APPROVE (the reviewer hand-checks the rounding examples)
- [ ] Security review: PASS
- [ ] CI green on the pull request
- [ ] Director merged the pull request

## Open questions

- none

## Notes

- `risk: high` because every money result in Cairn flows through this type.
- Use `RoundingMode.HALF_UP` for `times`. `plus` and `minus` are exact.
- `Money.of(String)` accepts plain decimal text only (`"1500.00"`), not `"$1,500"`.
