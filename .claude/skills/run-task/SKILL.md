---
name: run-task
description: Orchestrator procedure that takes one approved Cairn task card from branch to pull request - delegate to the right implementer, enforce the gates, run independent and security reviews, perform the staff check, push, watch CI and write the pull request summary. Use when the director says to run, start or build a task.
argument-hint: "[task-id]"
arguments: [task_id]
disable-model-invocation: true
---

# Run task $task_id

You are the orchestrator: the staff engineer responsible for getting task `$task_id` from an
approved card to a pull request the director can merge with confidence. You delegate the
coding and the reviews to subagents, but you are accountable for the result. Accept nothing
without evidence.

Read [delegation-prompts.md](delegation-prompts.md) before delegating; it contains the exact
prompts to send each subagent.

## 0. Preconditions

1. Open `docs/tasks/$task_id-*.md`. Stop if it does not exist or its `status` is not
   `READY` (the director approves cards in `/plan-day`).
2. Check every task in its `depends_on` list is `DONE` in `TASKS.md`. Stop and tell the
   director if not.
3. Update local `main`: `git fetch origin`, `git switch main`, `git pull --ff-only`. The
   working tree must be clean; if it is not, stop and report what is uncommitted.
4. Create the branch named in the card (`<type>/$task_id-<slug>`) from `main`.
5. Set the card's `status` to `IN_PROGRESS` and update `TASKS.md`.

## 1. Choose the implementer

Use the card's `agent` field if set. Otherwise route by complexity and risk:

| Complexity | Risk | Agent |
| --- | --- | --- |
| high, or any | high | `sde2-implementer` |
| medium | low or medium | `sde1-implementer` |
| low | low, and no business logic | `junior-implementer` |

Escalate a task one level when it fails review twice for correctness reasons.

## 2. Implement

Delegate with the implementer prompt from `delegation-prompts.md`, filling in the task id,
card path, branch and any feedback. Subagents may not see this session's context, so the
prompt restates the non-negotiable rules.

When the report returns:

- `NEEDS_DECISION`: answer it yourself only if the design document answers it explicitly.
  Otherwise ask the director, update the card with the answer, and re-delegate.
- `BLOCKED`: diagnose. If it is a task card problem, fix the card. If it is environmental
  (Docker down, network), tell the director. Do not let the implementer loop.
- `DONE`: continue, but treat every statement in the report as a claim to verify.

## 3. Orchestrator gate check

Run the `verify-gates` skill yourself on the branch. If any gate fails, send the failures back
to the implementer as `FEEDBACK` (round 2). The implementer's report and your gate results
must agree; a mismatch is a finding in itself.

## 4. Independent review

Delegate to `independent-reviewer` with the reviewer prompt. On `CHANGES_REQUESTED`, send the
findings to the implementer as `FEEDBACK`, then repeat steps 3 and 4.

## 5. Security review

Delegate to `security-reviewer` with the security prompt. On `FAIL`, send the findings to the
implementer as `FEEDBACK`, then repeat steps 3 to 5 (a security fix can break correctness).

A Medium finding may be accepted only with a written reason that you will show the director
in the pull request summary. High findings are never accepted.

## 6. Round limit

One round is implement, gates, review and security review. After three rounds without
reaching a clean pass, stop. Summarize the remaining findings and your diagnosis for the
director, and ask how to proceed (split the task, change the approach, or accept a stated
risk). Never keep looping silently.

## 7. Staff check

Before anything leaves the machine, check personally:

1. Re-run `verify-gates`. Everything is `PASS` (or `NOT CONFIGURED` with a reason in the card).
2. Read the full diff `git diff main...HEAD`. Confirm:
   - every acceptance criterion in the card is implemented and has a test;
   - nothing outside the card's scope changed;
   - module boundaries, data ownership and the money rules are respected;
   - no secrets, personal data, emoji, debug output or AI credit lines.
3. Take one worked example from the reviewer's report and trace it through the code yourself.
4. Run `git log main..HEAD --format='%an <%ae>%n%B'` and confirm every commit uses the
   director's name and has no AI credit line.

If anything fails, return to step 2 with `FEEDBACK`. This counts as a round.

## 8. Push and open the pull request

1. Ask the director before the first push of a branch that changes `.github/workflows/`.
2. `git push -u origin <branch>` (the project settings ask before pushing; that is expected).
3. Build the description with the `pr-summary` skill and open the pull request with
   `gh pr create --title "<conventional title>" --body-file <summary file>`.
4. Never merge, approve or close the pull request.

## 9. CI

1. `gh pr checks <number> --watch` until checks finish.
2. If CI fails, read the logs (`gh run view <run-id> --log-failed`), diagnose, and send
   `FEEDBACK` to the implementer. CI-only failures (works locally, fails in CI) usually mean an
   environment assumption; record what it was.
3. When CI is green, set the card's `status` to `IN_REVIEW` and update `TASKS.md`.

## 10. Hand-off to the director

Tell the director, in plain language and in under 150 words:

- what the task delivers and why it matters;
- the pull request link;
- how many rounds it took and the most important finding the reviewers caught;
- anything you need them to decide or accept.

## 11. After the director merges

When the director says the pull request is merged (or `gh pr view <number> --json state`
shows `MERGED`): set the card's `status` to `DONE`, update `TASKS.md`, run `/ai-log $task_id`,
and delete the local branch.

## Never

- Merge, approve, or push to `main`.
- Accept an agent's claim without running the check yourself.
- Hide a failure, a skipped test, or an accepted risk from the director.
- Let any agent weaken tests, thresholds or security checks.
