# Git and delivery rules

## Branches

- `main` is protected. Changes reach it only through pull requests with green CI, merged by
  the director.
- One branch per task, created from an up-to-date `main`:
  `<type>/<task-id>-<short-slug>`, for example `feat/T-007-cost-sharing-parser`.
- Types: `feat`, `fix`, `test`, `refactor`, `docs`, `build`, `ci`, `chore`, `security`.
- Never commit directly to `main`. Never force-push. Never rewrite history that was pushed.

## Commits

- Conventional Commits: `<type>(<module>): <imperative summary>`, under 72 characters,
  for example `feat(engine): apply out-of-pocket maximum to running total`.
- A body explains why, when the reason is not obvious. Reference the task id: `Refs: T-007`.
- Commits carry only the director's name and email from the local git config.
- Never add `Co-Authored-By`, "Generated with", "Claude", or any AI credit line. A PreToolUse
  hook blocks commits that contain them.
- Small, focused commits. Formatting-only changes go in their own commit.

## Pull requests

- One task per pull request. Title follows the commit convention.
- The description is produced by `/pr-summary` and contains: what changed and why, how it was
  verified (commands and results), review and security findings with their resolution,
  risks, and how to try it.
- Agents may open a pull request (`gh pr create`) only after the full task loop passes.
- Agents never merge, approve, or close pull requests. The director does.

## CI (GitHub Actions)

- On every pull request: build, `spotless:check`, Error Prone, unit, golden, property,
  integration, API, architecture and security tests, JaCoCo gate, CodeQL, dependency review.
- On merge to `main`: the same, plus the end-to-end smoke test.
- On a `v*` tag: build and scan the Docker image, push it to GHCR, create a GitHub Release.
- Weekly: the Rule Watcher workflow.
- A red CI run is fixed before any new task starts on the same area.
- Workflow files (`.github/workflows/`) change only through a task card that says so, and
  the orchestrator asks the director before pushing such a change.

## Releases

- Semantic versioning. `v0.1.0` is the first public release.
- Release notes list features, known limitations and the disclaimer.
