# Testing rules

Tests are the main evidence that work is correct. A task is not done until its tests exist,
pass, and would have failed without the change.

## Test-first workflow

1. Read the acceptance criteria in the task card.
2. Write tests that express each criterion. Run them and confirm they fail for the expected
   reason (not a compile error or setup problem).
3. Write the smallest code that makes them pass.
4. Refactor with the tests green.
5. Record the "failed before, passed after" evidence in the task report.

## The test pyramid

| Level | Naming | Runs with | Use for |
| --- | --- | --- | --- |
| Unit | `*Test.java` | Surefire (`./mvnw test`) | Pure logic: parser, engine steps, timeline math, rule lookup, diff |
| Property-based | `*Properties.java` | Surefire, jqwik | Invariants over many generated inputs (engine, money) |
| Golden file | `*GoldenTest.java` | Surefire | Real SBC fixtures against hand-checked expected JSON |
| Integration | `*IT.java` | Failsafe (`./mvnw verify`), Testcontainers | Repositories, migrations, job queue, scheduler |
| API | `*ApiTest.java` | MockMvc | Status codes, validation, problem details, security headers |
| Architecture | `ModularityTest.java` | Surefire | Spring Modulith `ApplicationModules.of(...).verify()` |
| Security | `*SecurityTest.java` | Surefire or MockMvc | One test per threat in `docs/threat-model.md` |
| End to end | `scripts/e2e-smoke.sh` | CI only | Compose up, upload fixture, compare, reminder in Mailpit |

## Writing good tests

- Test names describe behavior: `copayBeforeDeductibleIsChargedEvenWhenDeductibleUnmet()`.
- Arrange, act, assert, with a blank line between sections. One behavior per test.
- Use AssertJ (`assertThat`) with specific assertions. For money, compare with
  `isEqualByComparingTo`.
- No logic in tests (no loops or conditionals that decide what to assert). Use parameterized
  tests for tables of cases.
- No `Thread.sleep`. Use a fixed `Clock`, Awaitility for async work, and Testcontainers for
  real dependencies.
- Mock only what you do not own or what is slow (external HTTP, the LLM). Never mock the class
  under test or value types.
- Each test is independent and can run in any order.

## Required tests by area

- **Engine:** each of the six cost steps has unit tests; properties cover: cost never exceeds
  premiums plus the out-of-pocket maximum (in-network), more usage never costs less,
  in-network preventive care is always zero, results are deterministic.
- **Parser:** a table of real phrases from fixtures (`"$30 copay/visit; deductible does not
  apply"`, `"20% coinsurance"`, `"No charge"`, `"Not covered"`) with expected typed rules;
  unknown phrases go to quarantine, never to a default.
- **Reconciliation:** for every golden SBC, simulating its coverage examples matches the
  printed totals within the tolerance stated in the design.
- **Timeline:** gap detection on boundaries (same-day start and end, overlapping coverage,
  open-ended coverage), and rule selection by effective date.
- **Security:** see `docs/threat-model.md`; each threat lists its test.

## Fixtures

- Only public SBC documents (published by insurers, employers or state exchanges) or
  synthetic documents. Record the source URL of each fixture in
  `src/test/resources/fixtures/sbc/SOURCES.md`.
- Golden JSON is checked by hand by the orchestrator before it is trusted. Note who checked it
  and when in `SOURCES.md`.
- No real personal data in any fixture.

## Coverage

- JaCoCo gate: 80% line coverage for `engine`, `normalize`, `validate`, `timeline`, `diff`;
  70% overall. The gate is in `./mvnw verify`.
- Coverage is a floor, not the goal. A reviewer may reject code with high coverage but weak
  assertions.

## Flaky tests

- A test that fails intermittently is a bug. Do not retry it until it passes. Fix it or
  quarantine it with `@Disabled("flaky: T-xxx")` and open a task card.
