#!/usr/bin/env bash
# Behavioral check for the jqwik ban (ADR 0006, TM-14).
#   (a) A checkout of HEAD with a direct jqwik test dependency must fail the
#       build with the Enforcer ban failure, not just any failure.
#   (b) The real checkout must pass ./mvnw -B test, the guard tests must have
#       run, and neither the console output nor the test reports may mention
#       jqwik except inside the class name JqwikBanSecurityTest.
# The working tree must be clean: (a) builds HEAD and (b) builds the tree, so
# they must be the same content. Locally, commit first.
# Run from anywhere inside the repository. Extra Maven settings, if any, come
# from the MAVEN_ARGS environment variable (local runs only; CI must not use it).
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

echo "MAVEN_ARGS=[${MAVEN_ARGS:-}]"
if [ "${CI:-}" = "true" ] && [ -n "${MAVEN_ARGS:-}" ]; then
  echo "FAIL: MAVEN_ARGS must be empty when CI=true" >&2
  exit 1
fi

# A fresh checkout can show mvnw.cmd as modified because of its CRLF
# attribute, so compare ignoring a carriage return at the end of a line.
untracked="$(git ls-files --others --exclude-standard)"
if [ -n "$untracked" ] || ! git diff --quiet --ignore-cr-at-eol HEAD; then
  echo "FAIL: working tree is not clean; (a) builds HEAD and (b) builds the tree, so commit or stash first" >&2
  git status --short >&2
  exit 1
fi

work_dir="$(mktemp -d)"
copy_dir="$work_dir/copy"
cleanup() {
  git -C "$repo_root" worktree remove --force "$copy_dir" > /dev/null 2>&1 || true
  rm -rf "$work_dir"
  git -C "$repo_root" worktree prune > /dev/null 2>&1 || true
}
trap cleanup EXIT

ban_message="net.jqwik is banned"

echo "== (a) HEAD with a direct net.jqwik:jqwik dependency must fail =="
# A worktree applies .gitattributes like a normal checkout (git archive would
# drop export-ignore files, so the copy could differ from the real build).
git worktree add --detach "$copy_dir" HEAD > /dev/null 2>&1

# Insert the dependency before the last closing dependencies tag, which
# closes the project-level dependencies block.
last_line="$(grep -n '</dependencies>' "$copy_dir/pom.xml" | tail -n 1 | cut -d: -f1 || true)"
if [ -z "$last_line" ]; then
  echo "FAIL: no </dependencies> found in pom.xml" >&2
  exit 1
fi
awk -v n="$last_line" '
  NR == n {
    print "    <dependency>"
    print "      <groupId>net.jqwik</groupId>"
    print "      <artifactId>jqwik</artifactId>"
    print "      <version>1.10.2</version>"
    print "      <scope>test</scope>"
    print "    </dependency>"
  }
  { print }
' "$copy_dir/pom.xml" > "$work_dir/pom.xml.new"
mv "$work_dir/pom.xml.new" "$copy_dir/pom.xml"

if ! grep -q '<groupId>net.jqwik</groupId>' "$copy_dir/pom.xml"; then
  echo "FAIL: the test dependency was not added to the copy" >&2
  exit 1
fi

a_status=0
(cd "$copy_dir" && env MAVEN_OPTS= ./mvnw -B validate) > "$work_dir/a.log" 2>&1 || a_status=$?

if [ "$a_status" -eq 0 ]; then
  echo "FAIL (a): build passed with a direct jqwik dependency" >&2
  tail -n 40 "$work_dir/a.log" >&2
  exit 1
fi
# Require the Enforcer's own failure lines. A bare message match could also
# come from a configuration dump in a build that failed for another reason.
for expected in \
  "BannedDependencies failed with message" \
  "$ban_message" \
  "net.jqwik:jqwik:jar:1.10.2 <--- banned"; do
  if ! grep -q -F -- "$expected" "$work_dir/a.log"; then
    echo "FAIL (a): build failed (exit $a_status) but the log lacks: $expected" >&2
    tail -n 40 "$work_dir/a.log" >&2
    exit 1
  fi
done
echo "OK (a): build failed with exit $a_status and the Enforcer ban failure"

echo "== (b) real checkout: ./mvnw -B test must pass without jqwik output =="
# Remove old reports so only this run's reports are checked.
rm -rf target/surefire-reports target/failsafe-reports

b_status=0
env MAVEN_OPTS= ./mvnw -B test > "$work_dir/b.log" 2>&1 || b_status=$?

if [ "$b_status" -ne 0 ]; then
  echo "FAIL (b): ./mvnw -B test exited with $b_status" >&2
  tail -n 60 "$work_dir/b.log" >&2
  exit 1
fi

# Read stdin, drop the allowed class name, print any other jqwik mention.
scan_for_jqwik() {
  sed 's/JqwikBanSecurityTest//g' | grep -a -i -n 'jqwik' | cut -c1-200 || true
}

console_hits="$(scan_for_jqwik < "$work_dir/b.log")"
if [ -n "$console_hits" ]; then
  echo "FAIL (b): console output mentions jqwik outside JqwikBanSecurityTest:" >&2
  echo "$console_hits" >&2
  exit 1
fi

# Test output can be redirected to files under the report directories.
report_dirs=()
for dir in target/surefire-reports target/failsafe-reports; do
  if [ -d "$dir" ]; then
    report_dirs+=("$dir")
  fi
done
if [ "${#report_dirs[@]}" -eq 0 ]; then
  echo "FAIL (b): no test report directory found" >&2
  exit 1
fi
report_hits="$(find "${report_dirs[@]}" -type f -exec cat {} + | scan_for_jqwik)"
if [ -n "$report_hits" ]; then
  echo "FAIL (b): test reports mention jqwik outside JqwikBanSecurityTest:" >&2
  echo "$report_hits" >&2
  exit 1
fi

# The guard tests must have run, with at least one test and no failures,
# errors or skips (an exclude in the build could otherwise hide them).
require_suite() {
  local file="target/surefire-reports/TEST-$1.xml"
  if [ ! -f "$file" ]; then
    echo "FAIL (b): missing test report $file; the suite did not run" >&2
    exit 1
  fi
  local tag tests errors failures skipped
  tag="$(tr '\n' ' ' < "$file" | grep -o '<testsuite [^>]*>' | head -n 1 || true)"
  tests="$(printf '%s' "$tag" | grep -o ' tests="[0-9]*"' | grep -o '[0-9]*' || true)"
  errors="$(printf '%s' "$tag" | grep -o ' errors="[0-9]*"' | grep -o '[0-9]*' || true)"
  failures="$(printf '%s' "$tag" | grep -o ' failures="[0-9]*"' | grep -o '[0-9]*' || true)"
  skipped="$(printf '%s' "$tag" | grep -o ' skipped="[0-9]*"' | grep -o '[0-9]*' || true)"
  if [ -z "$tests" ] || [ -z "$errors" ] || [ -z "$failures" ] || [ -z "$skipped" ]; then
    echo "FAIL (b): could not read the counts in $file" >&2
    exit 1
  fi
  if [ "$tests" -le 0 ] || [ "$errors" -ne 0 ] || [ "$failures" -ne 0 ] || [ "$skipped" -ne 0 ]; then
    echo "FAIL (b): $1 tests=$tests errors=$errors failures=$failures skipped=$skipped" >&2
    exit 1
  fi
  echo "  $1: tests=$tests errors=$errors failures=$failures skipped=$skipped"
}
require_suite app.cairn.JqwikBanSecurityTest
require_suite app.cairn.BuildConfigurationTest

echo "OK (b): tests passed, guard tests ran, and no jqwik text in console or reports"
