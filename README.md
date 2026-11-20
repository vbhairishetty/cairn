# Cairn

Trail markers for your health coverage.

**Status:** Cairn is under active development. v0.1.0 is planned for Nov 25, 2026. Today only
the build skeleton exists. Nothing in the features below works yet.

## What Cairn will do

Cairn is a free, open-source, self-hosted tool. It helps employees choose a health plan, use
the benefits they already pay for, and avoid coverage gaps, with extra guidance for F-1
students on OPT and STEM OPT. It runs for one person on their own machine, with one profile
and no login.

v0.1.0 is planned to include:

- **Compare plans.** Upload the Summary of Benefits and Coverage (SBC) PDFs for your plans.
  Cairn will show the expected, best and worst yearly cost of each plan for your usage. Every
  number will link to the line in the PDF it came from. Costs are estimates, not bills.
- **Free-benefits checklist.** A personal list of preventive care that costs nothing under
  your plan, with calendar reminders you import as an `.ics` file.
- **What changed since last year?** A rule-by-rule comparison of two versions of the same plan.
- **OPT and STEM OPT coverage timeline.** Shows the days without coverage between a school
  plan and an employer plan, and the rules that apply on those dates, each with its source.

## Privacy

- Cairn will run only on your machine, and nothing about you will leave it. The app makes no
  outbound calls. During setup, Docker downloads the image and, if you choose, a local AI
  model. Neither carries your data.
- Cairn will store as little as it can. It uses an age band instead of a birth date, and it
  does not store names, member IDs, email addresses or medical history.
- Delete everything: a button in the app and the `cairn delete-all` command will remove your
  documents, extracted data, estimates, reminders and profile.
- Cairn will not encrypt stored data itself. It relies on your computer's disk encryption,
  such as FileVault on a Mac.

## Not advice

Cairn provides estimates and general information. It is not insurance, financial, legal or immigration advice. Confirm coverage with your insurer and immigration questions with your school's international student office.

## Build from source

You need JDK 21. The Maven wrapper in the repository downloads Maven for you.

```
./mvnw clean verify
```

The command runs the formatting check, the compiler checks, the tests and the coverage gate.
Today it builds a Spring Boot application with no features yet. Instructions to run the app
and to start the database with Docker Compose will be added when they work.

## How Cairn is built

Cairn is built with an AI-directed, human-reviewed workflow. AI agents write code and tests
against a design that a person approved, and separate reviewers and a security review check
each change. A person reviews and merges every change. To learn more, read:

- [The design](docs/designs/cairn.md)
- [Architecture decision records](docs/adr/)
- [The threat model](docs/threat-model.md)
- [The AI workflow log](docs/ai-workflow.md)

## Tech stack

Java 21, Spring Boot 4.1, Spring Modulith, PostgreSQL 16 with Flyway and pgvector, Apache PDFBox 3 with Tabula-java, Thymeleaf with Chart.js, and Maven.

## License

Cairn is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
