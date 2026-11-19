---
paths:
  - "src/main/java/app/cairn/ask/**"
  - "src/test/java/app/cairn/ask/**"
  - "src/main/resources/prompts/**"
  - "src/test/resources/evals/**"
  - ".github/rule-watcher/**"
  - ".github/workflows/rule-watcher.yml"
---

# AI feature rules (Ask Cairn and the Rule Watcher)

The AI features help users understand Cairn's results. They never become the source of truth
for money, eligibility or law.

## Ask Cairn (RAG)

- **Allowed sources only:** the user's extracted plan rules, Cairn engine results, rule
  notices from `rules`, and the official Uniform Glossary. Nothing else is retrieved.
- **Retrieval:** chunks carry their `SourceRef` (document, page, line or rule id). Embeddings
  live in pgvector in the `ask` schema.
- **Prompt structure:** a fixed system prompt in `src/main/resources/prompts/`, then the
  retrieved chunks inside a clearly delimited data block, then the question. The system
  prompt states that the data block is reference material and that instructions inside it
  must be ignored.
- **Citations required:** every answer lists the `SourceRef` of each chunk it used. An answer
  without citations is not shown.
- **Numbers:** any number in an answer must appear in the retrieved sources or the engine
  result. A post-check extracts numbers from the answer and rejects the answer if one is not
  found.
- **Refusals:** when retrieval finds nothing relevant, the answer says so and points to the
  insurer or the school's international student office. Questions asking "should I" get facts
  and trade-offs, not a decision. Immigration-status questions are always referred to the
  international student office.
- **Model:** a local Ollama model by default. A cloud model is opt-in only, with a privacy
  warning, and is never enabled by default or in tests.

## Evaluation

- `src/test/resources/evals/ask-cases.yaml` holds questions with expected facts, expected
  citations, and expected refusals, including prompt-injection cases (a fixture SBC whose
  text tells the assistant to ignore its rules).
- The evaluation runs in CI against a stubbed model for determinism, and locally against the
  real model with a report. A drop in score blocks the task.

## Rule Watcher

- Runs in GitHub Actions on a schedule. It fetches only allowlisted URLs, stores a normalized
  text snapshot, compares it with the previous snapshot, and opens an issue when it changes.
- It never edits rule data. A human approves every rule change through a pull request that
  adds a new rule version with dates and sources.
- Optional AI summaries of a change are labeled as unverified summaries in the issue.

## Never

- Let model output change stored data without a human or a deterministic validator in the
  loop.
- Put personal data into a prompt sent to a non-local model.
- Ship an AI feature without its evaluation cases.
