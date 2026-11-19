---
paths:
  - "**/*.java"
---

# Java coding rules

These rules apply to every Java file. Formatting is automatic (Spotless with Google Java
Style), so these rules cover what a formatter cannot check.

## Language level and style

- Java 21. Use records for data carriers, sealed interfaces for closed sets of variants,
  `switch` expressions with pattern matching, and `var` only when the type is obvious from the
  right-hand side.
- Google Java Style naming: `UpperCamelCase` classes, `lowerCamelCase` methods and fields,
  `UPPER_SNAKE_CASE` constants, packages all lowercase.
- Names say what something is in the domain: `OutOfPocketMaximum`, not `Limit2`;
  `estimateYearlyCost`, not `calc`.
- One top-level class per file. Keep methods short enough to read without scrolling (about 30
  lines is a warning sign, not a hard limit).
- No emoji anywhere. No commented-out code. No `TODO` without a task id (`TODO(T-012): ...`).

## Comments

- Write short, plain comments that explain *why*, not *what* the code already says.
- Every public API type and method in a module root package has a one or two sentence
  Javadoc saying what it does and what it returns.
- No authorship tags, no AI credits, no dates in comments.

## Immutability and nulls

- Fields are `final` unless there is a stated reason. Records are preferred for values.
- Return empty collections, never `null`. Use `Optional` only as a return type for "may be
  absent"; never as a field or parameter.
- Validate arguments at public boundaries with `Objects.requireNonNull` or explicit checks and
  clear messages.
- Defensive copies (`List.copyOf`) for collections passed into records.

## Money, numbers and time

- Money is the `common.Money` value type wrapping `BigDecimal` with scale 2 and
  `RoundingMode.HALF_EVEN`. Never use `double`, `float` or `BigDecimal.equals` for comparing
  money; use `compareTo`.
- Percentages are `BigDecimal` fractions (0.20 for 20%), named with a `Rate` suffix.
- Dates use `java.time` (`LocalDate`, `Instant`). Inject `Clock`. Never parse dates with
  default locale or time zone assumptions.

## Errors

- Throw specific exceptions (`UnreadableDocumentException`, `RuleValidationException`) that
  extend a small module-level base exception. Never throw or catch bare `Exception` or
  `RuntimeException` except at the outermost boundary.
- Never swallow an exception. If you catch it, either handle it fully or rethrow with context.
- REST errors use RFC 9457 problem details through one `@RestControllerAdvice`.
- Expected outcomes (a value failed validation) are return values, not exceptions.

## Spring usage

- Constructor injection only. No field injection, no `@Autowired` on fields.
- Keep controllers thin: parse input, call the module API, map the result.
- `@Transactional` lives on service methods in the module that owns the data, never on
  controllers.
- Typed `@ConfigurationProperties` records with `@Validated` for configuration.

## Logging

- SLF4J with structured key-value pairs (`kv("jobId", id)` style through the configured JSON
  encoder). Log events, not values: `"extraction finished"` with `jobId`, `documentId`,
  `durationMs`.
- Never log document text, plan values tied to a person, profile data, file names supplied
  by the user, or exception messages that may contain them.
- Levels: `ERROR` needs action, `WARN` is unexpected but handled, `INFO` is a lifecycle event,
  `DEBUG` is for development only.

## Dependencies

- Use only libraries listed in the tech stack in `CLAUDE.md`. A new library needs an ADR.
- Prefer the JDK and Spring features already present over adding a library.
